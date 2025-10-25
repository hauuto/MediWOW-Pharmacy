package com.entities;

import com.enums.LotStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * @author Bùi Quốc Trụ, Nguyễn Thanh Khôi
 */
@Entity
@Table(name = "Lot")
public class Lot {
    @Id
    @Column(name = "batchNumber")
    private String batchNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product", nullable = false) // ✅ DB cột tên "product"
    private Product product;

    @Column(name = "quantity")
    private int quantity;

    @Column(name = "mwPrice")
    private double mwPrice;

    @Column(name = "expiryDate")
    private LocalDateTime expiryDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private LotStatus status;

    protected Lot() {}

    public Lot(String batchNumber, Product product, int quantity, double mwPrice, LocalDateTime expiryDate, LotStatus status) {
        this.batchNumber = batchNumber;
        this.product = product;
        this.quantity = quantity;
        this.mwPrice = mwPrice;
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
        return mwPrice;
    }

    public void setRawPrice(double rawPrice) {
        this.mwPrice = rawPrice;
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
