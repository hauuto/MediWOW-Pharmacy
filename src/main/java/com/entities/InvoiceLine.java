package com.entities;

import com.enums.LineType;
import com.enums.LotStatus;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * @author Bùi Quốc Trụ
 */
@Entity
@Table(name = "InvoiceLine")
public class InvoiceLine {
    @Id
    @Column(name = "id", updatable = false, nullable = false, length = 50)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice", nullable = false)
    private Invoice invoice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "product", referencedColumnName = "product", nullable = false),
            @JoinColumn(name = "unitOfMeasure", referencedColumnName = "measurementId", nullable = false)
    })
    private UnitOfMeasure unitOfMeasure;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "unitPrice", nullable = false, precision = 18, scale = 2)
    private BigDecimal unitPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "lineType", nullable = false, length = 50)
    private LineType lineType;

    @OneToMany(mappedBy = "invoiceLine", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<LotAllocation> lotAllocations = new ArrayList<>();

    public InvoiceLine(Invoice invoice, UnitOfMeasure unitOfMeasure, int quantity, BigDecimal unitPrice, LineType lineType, List<LotAllocation> lotAllocations) {
        this.invoice = invoice;
        this.unitOfMeasure = unitOfMeasure;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.lineType = lineType;
        this.lotAllocations = lotAllocations;
    }

    public InvoiceLine(String id, Invoice invoice, UnitOfMeasure unitOfMeasure, int quantity, BigDecimal unitPrice, LineType lineType, List<LotAllocation> lotAllocations) {
        this.id = id;
        this.invoice = invoice;
        this.unitOfMeasure = unitOfMeasure;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.lineType = lineType;
        this.lotAllocations = lotAllocations;
    }

    protected InvoiceLine() { }

    public String getId() {
        return id;
    }

    public Invoice getInvoice() {
        return invoice;
    }

    public void setInvoice(Invoice invoice) {
        this.invoice = invoice;
    }

    public UnitOfMeasure getUnitOfMeasure() {
        return unitOfMeasure;
    }

    public void setUnitOfMeasure(UnitOfMeasure unitOfMeasure) {
        this.unitOfMeasure = unitOfMeasure;
    }

    /**
     * Convenience method to get the Product from the UnitOfMeasure.
     * @return the Product associated with this invoice line's UnitOfMeasure
     */
    public Product getProduct() {
        return unitOfMeasure != null ? unitOfMeasure.getProduct() : null;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public LineType getLineType() {
        return lineType;
    }

    public void setLineType(LineType lineType) {
        this.lineType = lineType;
    }

    public List<LotAllocation> getLotAllocations() {
        return lotAllocations;
    }

    public void setLotAllocations(List<LotAllocation> lotAllocations) {
        this.lotAllocations = lotAllocations;
    }

    // =====================================================
    // Money calculations (BigDecimal is the source of truth)
    // =====================================================

    /** Calculate the subtotal of this invoice line. */
    public BigDecimal calculateSubtotal() {
        if (unitPrice == null) return BigDecimal.ZERO;
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    /** Calculate the VAT amount for this invoice line. */
    public BigDecimal calculateVatAmount() {
        Product product = unitOfMeasure.getProduct();
        if (product == null) return BigDecimal.ZERO;
        BigDecimal vatPercent = product.getVat() != null ? product.getVat() : BigDecimal.ZERO;
        BigDecimal vatRate = vatPercent.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
        return calculateSubtotal().multiply(vatRate);
    }

    /** Calculate the total amount (subtotal + VAT) for this invoice line. */
    public BigDecimal calculateTotalAmount() {
        return calculateSubtotal().add(calculateVatAmount());
    }

    /**
     * @author Bùi Quốc Trụ
     *
     * Allocates lots to this invoice line using FIFO (First-In-First-Out) principle.
     * Converts the quantity from the current UOM to base UOM, then selects oldest available lots.
     *
     * @return true if sufficient inventory exists and lots were allocated successfully, false otherwise
     */
    public boolean allocateLots() {
        Product product = unitOfMeasure.getProduct();

        // Clear existing allocations
        lotAllocations.clear();

        if (product == null || quantity <= 0) {
            return false;
        }

        // Convert quantity to base UOM
        int baseQuantityNeeded = convertToBaseQuantity();

        // Get all available lots sorted by expiry date (FIFO - oldest first)
        List<Lot> availableLots = product.getLotSet().stream()
                .filter(lot -> lot.getStatus() == LotStatus.AVAILABLE && lot.getQuantity() > 0)
                .sorted(Comparator.comparing(Lot::getExpiryDate))
                .toList();

        // Check if we have enough inventory
        int totalAvailable = availableLots.stream().mapToInt(Lot::getQuantity).sum();
        if (totalAvailable < baseQuantityNeeded) {
            return false; // Insufficient inventory
        }

        // Allocate lots using FIFO
        int remainingQuantity = baseQuantityNeeded;
        for (Lot lot : availableLots) {
            if (remainingQuantity <= 0) {
                break;
            }

            int allocatedQuantity = Math.min(remainingQuantity, lot.getQuantity());

            // Create lot allocation with generated ID
            String allocationId = "LA-" + UUID.randomUUID().toString();
            LotAllocation allocation = new LotAllocation(allocationId, this, lot, allocatedQuantity);
            lotAllocations.add(allocation);

            remainingQuantity -= allocatedQuantity;
        }

        return remainingQuantity == 0;
    }

    /**
     * @author Bùi Quốc Trụ
     *
     * Converts the current quantity from the current UOM to base UOM quantity.
     * Formula: baseQuantity = currentQuantity / baseUnitConversionRate
     *
     * @return The quantity in base UOM units
     */
    public int convertToBaseQuantity() {
        if (unitOfMeasure == null) {
            return quantity;
        }

        Product product = unitOfMeasure.getProduct();
        if (product == null) {
            return quantity;
        }

        // Check if current UOM is already the base unit (name matches baseUnitOfMeasure)
        String currentUomName = unitOfMeasure.getName();
        if (currentUomName != null && currentUomName.equals(product.getBaseUnitOfMeasure())) {
            return quantity;
        }

        // Use the conversion rate from the current unitOfMeasure
        BigDecimal conversionRate = unitOfMeasure.getBaseUnitConversionRate();
        if (conversionRate == null || conversionRate.compareTo(BigDecimal.ZERO) == 0) {
            return quantity; // Fallback to original quantity if no valid conversion rate
        }

        // Convert: baseQuantity = ceil(currentQuantity / baseUnitConversionRate)
        // e.g., if baseUnitConversionRate = 0.1 (1 box = 10 tablets), then 2 boxes = ceil(2 / 0.1) = 20 tablets
        BigDecimal q = BigDecimal.valueOf(quantity);
        BigDecimal base = q.divide(conversionRate, 10, RoundingMode.CEILING);
        return base.setScale(0, RoundingMode.CEILING).intValue();
    }

    /**
     * @author Bùi Quốc Trụ
     *
     * Gets the total base quantity needed for this invoice line.
     *
     * @return The quantity in base UOM units
     */
    public int getBaseQuantityNeeded() {
        return convertToBaseQuantity();
    }

    /**
     * @author Bùi Quốc Trụ
     *
     * Clears all lot allocations for this invoice line.
     */
    public void clearLotAllocations() {
        lotAllocations.clear();
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;

        if (obj == null || getClass() != obj.getClass())
            return false;

        InvoiceLine other = (InvoiceLine) obj;
        return Objects.equals(id, other.id);
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
