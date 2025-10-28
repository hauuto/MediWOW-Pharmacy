package com.entities;

import com.enums.LineType;
import jakarta.persistence.*;

import java.io.Serializable;
import java.util.Objects;

/**
 * @author Bùi Quốc Trụ
 */
@Entity
@Table(name = "InvoiceLine")
@IdClass(InvoiceLine.InvoiceLineId.class)
public class InvoiceLine {
    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice", nullable = false)
    private Invoice invoice;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product", nullable = false)
    private Product product;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unitOfMeasure", nullable = false)
    private UnitOfMeasure unitOfMeasure;

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "lineType", nullable = false, length = 50)
    private LineType lineType;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "unitPrice", nullable = false)
    private double unitPrice;

    protected InvoiceLine() {}

    public InvoiceLine(Product product, Invoice invoice, UnitOfMeasure unitOfMeasure, LineType lineType, int quantity) {
        this.product = product;
        this.invoice = invoice;
        this.unitOfMeasure = unitOfMeasure;
        this.lineType = lineType;
        this.quantity = quantity;

        // Calculate unit price based on the product's oldest lot
        Lot oldestLot = product.getOldestLotAvailable();
        if (oldestLot != null) {
            this.unitPrice = oldestLot.getRawPrice();
            if (unitOfMeasure != null) {
                this.unitPrice *= unitOfMeasure.getBasePriceConversionRate();
            }
        }
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

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public UnitOfMeasure getUnitOfMeasure() {
        return unitOfMeasure;
    }

    public void setUnitOfMeasure(UnitOfMeasure unitOfMeasure) {
        this.unitOfMeasure = unitOfMeasure;
    }

    public LineType getLineType() {
        return lineType;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(double unitPrice) {
        this.unitPrice = unitPrice;
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
        return Objects.hash(product.getId(), invoice.getId(), unitOfMeasure.getId(), lineType);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;

        if (obj == null || getClass() != obj.getClass())
            return false;

        InvoiceLine other = (InvoiceLine) obj;
        return Objects.equals(product.getId(), other.product.getId()) &&
               Objects.equals(invoice.getId(), other.invoice.getId()) &&
               Objects.equals(unitOfMeasure.getId(), other.unitOfMeasure.getId()) &&
               lineType == other.lineType;
    }

    @Override
    public String toString() {
        return super.toString();
    }

    /**
     * Composite key class for InvoiceLine
     */
    public static class InvoiceLineId implements Serializable {
        private String invoice;
        private String product;
        private String unitOfMeasure;
        private LineType lineType;

        public InvoiceLineId() {}

        public InvoiceLineId(String invoice, String product, String unitOfMeasure, LineType lineType) {
            this.invoice = invoice;
            this.product = product;
            this.unitOfMeasure = unitOfMeasure;
            this.lineType = lineType;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            InvoiceLineId that = (InvoiceLineId) o;
            return Objects.equals(invoice, that.invoice) &&
                   Objects.equals(product, that.product) &&
                   Objects.equals(unitOfMeasure, that.unitOfMeasure) &&
                   lineType == that.lineType;
        }

        @Override
        public int hashCode() {
            return Objects.hash(invoice, product, unitOfMeasure, lineType);
        }
    }
}
