package com.entities;

import com.enums.LineType;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

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
    @JoinColumn(name = "product", nullable = false)
    private Product product;

    @Column(name = "unitOfMeasure", nullable = false, length = 100)
    private String unitOfMeasure;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "unitPrice", nullable = false)
    private double unitPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "lineType", nullable = false, length = 50)
    private LineType lineType;

    @OneToMany(mappedBy = "invoiceLine",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    private List<LotAllocation> lotAllocations = new ArrayList<>();

    protected InvoiceLine() {}

    /**
     * Auto-generate ID if not set
     */
    @PrePersist
    private void ensureId() {
        if (this.id == null) {
            this.id = "ILN-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
        }
    }

    /**
     * Minimal constructor
     */
    public InvoiceLine(Product product, Invoice invoice,
                       String unitOfMeasure, LineType lineType,
                       int quantity, double unitPrice) {
        this.product = product;
        this.invoice = invoice;
        this.unitOfMeasure = unitOfMeasure;
        this.lineType = lineType;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    /**
     * Full constructor
     */
    public InvoiceLine(String id, Product product, Invoice invoice,
                       String unitOfMeasure, LineType lineType,
                       int quantity, double unitPrice) {
        this.id = id;
        this.product = product;
        this.invoice = invoice;
        this.unitOfMeasure = unitOfMeasure;
        this.lineType = lineType;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    // ------------------ GETTERS/SETTERS ------------------

    public String getId() {
        return id;
    }

    public Product getProduct() {
        return product;
    }

    public Invoice getInvoice() {
        return invoice;
    }

    public int getQuantity() {
        return quantity;
    }

    public String getUnitOfMeasure() {
        return unitOfMeasure;
    }

    public LineType getLineType() {
        return lineType;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public List<LotAllocation> getLotAllocations() {
        return lotAllocations;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public void setInvoice(Invoice invoice) {
        this.invoice = invoice;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public void setUnitOfMeasure(String unitOfMeasure) {
        this.unitOfMeasure = unitOfMeasure;
    }

    public void setLineType(LineType lineType) {
        this.lineType = lineType;
    }

    public void setUnitPrice(double unitPrice) {
        this.unitPrice = unitPrice;
    }

    public void setLotAllocations(List<LotAllocation> lotAllocations) {
        this.lotAllocations = lotAllocations;
    }

    /**
     * Helper for allocation
     */
    public void addLotAllocation(LotAllocation allocation) {
        allocation.setInvoiceLine(this);
        lotAllocations.add(allocation);
    }

    // ------------------ CALCULATIONS ------------------

    public double calculateSubtotal() {
        return unitPrice * quantity;
    }

    public double calculateVatAmount() {
        return calculateSubtotal() * (product.getVat() / 100.0);
    }

    public double calculateTotalAmount() {
        return calculateSubtotal() + calculateVatAmount();
    }

    // ------------------ EQUALS/HASHCODE ------------------

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof InvoiceLine)) return false;
        InvoiceLine other = (InvoiceLine) obj;
        return Objects.equals(id, other.id);
    }

    // ------------------ TOSTRING ------------------

    @Override
    public String toString() {
        return "InvoiceLine{" +
                "id='" + id + '\'' +
                ", product=" + (product != null ? product.getId() : null) +
                ", unitOfMeasure='" + unitOfMeasure + '\'' +
                ", qty=" + quantity +
                ", unitPrice=" + unitPrice +
                ", lineType=" + lineType +
                '}';
    }
}
