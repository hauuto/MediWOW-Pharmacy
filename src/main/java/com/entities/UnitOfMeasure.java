package com.entities;

import jakarta.persistence.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
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

    @Column(name = "price", nullable = false, precision = 18, scale = 2)
    private BigDecimal price;

    @Column(name = "baseUnitConversionRate", nullable = false, precision = 18, scale = 4)
    private BigDecimal baseUnitConversionRate;

    @Transient
    private BigDecimal basePriceConversionRate;

    protected UnitOfMeasure() {}

    public UnitOfMeasure(Product product, String name, BigDecimal price, BigDecimal baseUnitConversionRate) {
        this.product = product;
        this.name = name;
        this.price = price;
        this.baseUnitConversionRate = baseUnitConversionRate;
        calculateDerivedFields();
    }

    @PostLoad
    private void calculateDerivedFields() {
        if (baseUnitConversionRate != null && baseUnitConversionRate.compareTo(BigDecimal.ZERO) != 0) {
            // basePriceConversionRate = 1 / baseUnitConversionRate
            basePriceConversionRate = BigDecimal.ONE.divide(baseUnitConversionRate, 10, RoundingMode.HALF_UP);
        } else {
            basePriceConversionRate = BigDecimal.ONE;
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

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getBaseUnitConversionRate() {
        return baseUnitConversionRate;
    }

    public void setBaseUnitConversionRate(BigDecimal baseUnitConversionRate) {
        this.baseUnitConversionRate = baseUnitConversionRate;
        calculateDerivedFields();
    }

    public BigDecimal getBasePriceConversionRate() {
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
