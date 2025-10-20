package com.entities;

import com.enums.LineType;

import java.util.Objects;

/**
 * @author Bùi Quốc Trụ
 */
public class InvoiceLine {
    private final Product product;
    private final Invoice invoice;
    private int quantity;
    private final UnitOfMeasure unitOfMeasure;
    private final LineType lineType;

    public InvoiceLine(Product product, Invoice invoice, UnitOfMeasure unitOfMeasure, LineType lineType, int quantity) {
        this.product = product;
        this.invoice = invoice;
        this.unitOfMeasure = unitOfMeasure;
        this.lineType = lineType;
        this.quantity = quantity;
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

    public LineType getLineType() {
        return lineType;
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
        Lot oldestLot = product.getOldestLotAvailable();

        if (oldestLot == null)
            return 0.0;

        double baseUomSubtotal = oldestLot.getRawPrice() * (1 + product.getVat() / 100) * quantity;

        if (unitOfMeasure != null) // If the unit of measure is specified (not using base UOM), convert the subtotal
            return baseUomSubtotal * unitOfMeasure.getBasePriceConversionRate();

        return baseUomSubtotal;
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
}
