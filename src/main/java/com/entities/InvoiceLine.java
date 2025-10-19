package com.entities;

import com.enums.LineType;

import java.util.Objects;

/**
 * @author Bùi Quốc Trụ
 */
public class InvoiceLine {
    private Product product;
    private Invoice invoice;
    private int quantity;
    private UnitOfMeasure unitOfMeasure;
    private LineType lineType;

    public InvoiceLine(Product product, Invoice invoice, int quantity, UnitOfMeasure unitOfMeasure, LineType lineType) {
        this.product = product;
        this.invoice = invoice;
        this.quantity = quantity;
        this.unitOfMeasure = unitOfMeasure;
        this.lineType = lineType;
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

    public UnitOfMeasure getUnitOfMeasure() {
        return unitOfMeasure;
    }

    public void setUnitOfMeasure(UnitOfMeasure unitOfMeasure) {
        this.unitOfMeasure = unitOfMeasure;
    }

    public LineType getLineType() {
        return lineType;
    }

    public void setLineType(LineType lineType) {
        this.lineType = lineType;
    }

//    public double calculateSubtotal() {
//        return unitOfMeasure.getRawPrice() * quantity;
//    }


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
