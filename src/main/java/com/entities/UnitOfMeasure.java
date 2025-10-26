package com.entities;

import jakarta.persistence.*;

import java.util.Objects;

/**
 * @author Bùi Quốc Trụ, Nguyễn Thanh Khôi
 */
@Entity
@Table(name = "UnitOfMeasure")
public class UnitOfMeasure {
    @Id
    @Column(name = "id")
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product", nullable = false) // ✅ DB cột tên "product"
    private Product product;

    @Column(name = "name")
    private String name;

    @Column(name = "baseUnitConversionRate")
    private double baseUnitConversionRate;

    @Transient
    private double basePriceConversionRate;

    protected UnitOfMeasure() {}

    public UnitOfMeasure(String id, Product product, String name, double baseUnitConversionRate) {
        this.id = id;
        this.product = product;
        this.name = name;
        this.baseUnitConversionRate = baseUnitConversionRate;
        basePriceConversionRate = 1 / baseUnitConversionRate;
    }

    @PostLoad
    private void calculateDerivedFields() {
        if (baseUnitConversionRate != 0) {
            basePriceConversionRate = 1 / baseUnitConversionRate;
        }
    }

    public String getId() {
        return id;
    }

    public Product getProduct() {
        return product;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
        return Objects.hash(id);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;

        if (obj == null || getClass() != obj.getClass())
            return false;

        UnitOfMeasure other = (UnitOfMeasure) obj;
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
