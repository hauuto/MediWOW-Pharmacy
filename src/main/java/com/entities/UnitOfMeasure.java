package com.entities;

import jakarta.persistence.*;

import java.io.Serializable;
import java.util.Objects;

/**
 * @author Bùi Quốc Trụ, Nguyễn Thanh Khôi
 */
@Entity
@Table(name = "UnitOfMeasure")
@IdClass(UnitOfMeasure.UnitOfMeasureId.class)
public class UnitOfMeasure {
    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product", nullable = false)
    private Product product;

    @Id
    @Column(name = "name", length = 100)
    private String name;

    @Column(name = "price", nullable = false)
    private double price;

    @Column(name = "baseUnitConversionRate", nullable = false)
    private double baseUnitConversionRate;

    @Transient
    private double basePriceConversionRate;

    protected UnitOfMeasure() {}

    public UnitOfMeasure(Product product, String name, double price, double baseUnitConversionRate) {
        this.product = product;
        this.name = name;
        this.price = price;
        this.baseUnitConversionRate = baseUnitConversionRate;
        basePriceConversionRate = 1 / baseUnitConversionRate;
    }

    @PostLoad
    private void calculateDerivedFields() {
        if (baseUnitConversionRate != 0) {
            basePriceConversionRate = 1 / baseUnitConversionRate;
        }
    }

    public UnitOfMeasureId getId() {
        return new UnitOfMeasureId(product.getId(), name);
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public double getBaseUnitConversionRate() {
        return baseUnitConversionRate;
    }

    public void setBaseUnitConversionRate(double baseUnitConversionRate) {
        this.baseUnitConversionRate = baseUnitConversionRate;
        basePriceConversionRate = 1 / this.baseUnitConversionRate;
    }

    public double getBasePriceConversionRate() {
        return basePriceConversionRate;
    }

    @Override
    public int hashCode() {
        return Objects.hash(product.getId(), name);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;

        if (obj == null || getClass() != obj.getClass())
            return false;

        UnitOfMeasure other = (UnitOfMeasure) obj;
        return Objects.equals(product.getId(), other.product.getId()) &&
               Objects.equals(name, other.name);
    }

    @Override
    public String toString() {
        return super.toString();
    }

    /**
     * Composite key class for UnitOfMeasure
     */
    public static class UnitOfMeasureId implements Serializable {
        private String product;
        private String name;

        public UnitOfMeasureId() {}

        public UnitOfMeasureId(String product, String name) {
            this.product = product;
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            UnitOfMeasureId that = (UnitOfMeasureId) o;
            return Objects.equals(product, that.product) &&
                   Objects.equals(name, that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(product, name);
        }
    }
}
