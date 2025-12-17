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
    @Column(name = "id", length = 50)
    private String id;

    @Column(name = "batchNumber", length = 50)
    private String batchNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product", nullable = false) // ✅ DB cột tên "product"
    private Product product;

    @Column(name = "quantity")
    private int quantity;

    @Column(name = "rawPrice")
    private double rawPrice;

    @Column(name = "expiryDate")
    private LocalDateTime expiryDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private LotStatus status;

    protected Lot() {}

    public Lot(String id, String batchNumber, Product product, int quantity, double rawPrice, LocalDateTime expiryDate, LotStatus status) {
        this.id = id;
        this.batchNumber = batchNumber;
        this.product = product;
        this.quantity = quantity;
        this.rawPrice = rawPrice;
        this.expiryDate = expiryDate;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getBatchNumber() {
        return batchNumber;
    }

    public void setBatchNumber(String batchNumber) {
        this.batchNumber = batchNumber;
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
        return Objects.hash(id);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;

        if (o == null || getClass() != o.getClass())
            return false;

        Lot other = (Lot) o;
        return Objects.equals(id, other.id);
    }

    @Override
    public String toString() {
        return super.toString();
    }

    public void setProduct(Product p) {
        this.product = p;
    }
}
