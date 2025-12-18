package com.entities;

import com.enums.LineType;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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

    @OneToMany(mappedBy = "invoiceLine", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<LotAllocation> lotAllocations = new ArrayList<>();

    protected InvoiceLine() {}

    /**
     * Constructor without ID - Hibernate will auto-generate UUID
     */
    public InvoiceLine(Product product, Invoice invoice, String unitOfMeasure, LineType lineType, int quantity, double unitPrice) {
        this.product = product;
        this.invoice = invoice;
        this.unitOfMeasure = unitOfMeasure;
        this.lineType = lineType;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    /**
     * Constructor with ID - for cases where ID is already known
     */
    public InvoiceLine(String id, Product product, Invoice invoice, String unitOfMeasure, LineType lineType, int quantity, double unitPrice) {
        this.id = id;
        this.product = product;
        this.invoice = invoice;
        this.unitOfMeasure = unitOfMeasure;
        this.lineType = lineType;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public Invoice getInvoice() {
        return invoice;
    }

    public void setInvoice(Invoice invoice) {
        this.invoice = invoice;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getUnitOfMeasure() {
        return unitOfMeasure;
    }

    public void setUnitOfMeasure(String unitOfMeasure) {
        this.unitOfMeasure = unitOfMeasure;
    }

    public LineType getLineType() {
        return lineType;
    }

    public void setLineType(LineType lineType) {
        this.lineType = lineType;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(double unitPrice) {
        this.unitPrice = unitPrice;
    }

    public List<LotAllocation> getLotAllocations() {
        return lotAllocations;
    }

    public void setLotAllocations(List<LotAllocation> lotAllocations) {
        this.lotAllocations = lotAllocations;
    }

    /**
     * @author Bùi Quốc Trụ
     *
     * Calculate the subtotal of this invoice line.
     * If the unit of measure is not specified (null), the base UOM is used.
     *
     * @return The subtotal amount for this invoice line.
     */
    public double calculateSubtotal() {
        return unitPrice * quantity;
    }

    /**
     * @author Bùi Quốc Trụ
     *
     * Calculate the VAT amount for this invoice line.
     *
     * @return The VAT amount for this invoice line.
     */
    public double calculateVatAmount() {
        return calculateSubtotal() * (product.getVat() / 100.0);
    }

    /**
     * @author Bùi Quốc Trụ
     *
     * Calculate the total amount (subtotal + VAT) for this invoice line.
     *
     * @return The total amount for this invoice line.
     */
    public double calculateTotalAmount() {
        return calculateSubtotal() + calculateVatAmount();
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
