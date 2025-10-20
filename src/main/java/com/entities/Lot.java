package com.entities;

import com.enums.LotStatus;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * @author Bùi Quốc Trụ
 */
public class Lot {
    private final String batchNumber;
    private final Product product;
    private int quantity;
    private double rawPrice;
    private LocalDateTime expiryDate;
    private LotStatus status;

    public Lot(String batchNumber, Product product, int quantity, double rawPrice, LocalDateTime expiryDate, LotStatus status) {
        this.batchNumber = batchNumber;
        this.product = product;
        this.quantity = quantity;
        this.rawPrice = rawPrice;
        this.expiryDate = expiryDate;
        this.status = status;
    }

    public String getBatchNumber() {
        return batchNumber;
    }

    public Product getProduct() {
        return product;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getRawPrice() {
        return rawPrice;
    }

    public void setRawPrice(double rawPrice) {
        this.rawPrice = rawPrice;
    }

    public LocalDateTime getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDateTime expiryDate) {
        this.expiryDate = expiryDate;
    }

    public LotStatus getStatus() {
        return status;
    }

    public void setStatus(LotStatus status) {
        this.status = status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(batchNumber);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;

        if (o == null || getClass() != o.getClass())
            return false;

        Lot other = (Lot) o;
        return Objects.equals(batchNumber, other.batchNumber);
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
