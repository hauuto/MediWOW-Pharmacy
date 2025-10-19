package com.entities;

import com.enums.LotStatus;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * @author Bùi Quốc Trụ
 */
public class UnitOfMeasure {
    private final String id;
    private final Product product;
    private String name;
    private double baseUnitConversionRate;
    private double basePriceConversionRate;

    public UnitOfMeasure(String id, Product product, String name, double baseUnitConversionRate) {
        this.id = id;
        this.product = product;
        this.name = name;
        this.baseUnitConversionRate = baseUnitConversionRate;
        basePriceConversionRate = 1 / baseUnitConversionRate;
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
}
