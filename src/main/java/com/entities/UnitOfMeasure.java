package com.entities;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;

/**
 * UnitOfMeasure entity
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
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "measurementId", nullable = false)
    private MeasurementName measurement;

    @Column(name = "price", nullable = false)
    private double price;

    @Column(name = "baseUnitConversionRate", nullable = false)
    private double baseUnitConversionRate;

    @Transient
    private double basePriceConversionRate;

    protected UnitOfMeasure() {}

    public UnitOfMeasure(Product product,
                         MeasurementName measurement,
                         double price,
                         double baseUnitConversionRate) {
        this.product = product;
        this.measurement = measurement;
        this.price = price;
        this.baseUnitConversionRate = baseUnitConversionRate;
        this.basePriceConversionRate = 1 / baseUnitConversionRate;
    }

    @PostLoad
    private void calculateDerivedFields() {
        if (baseUnitConversionRate != 0) {
            basePriceConversionRate = 1 / baseUnitConversionRate;
        }
    }

    public UnitOfMeasureId getId() {
        return new UnitOfMeasureId(
                product != null ? product.getId() : null,
                measurement != null ? measurement.getId() : null
        );
    }

    public Product getProduct() {
        return product;
    }

    public MeasurementName getMeasurement() {
        return measurement;
    }

    public void setMeasurement(MeasurementName measurement) {
        this.measurement = measurement;
    }

    public void setProduct(Product product) {
        this.product = product;
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
        this.basePriceConversionRate = 1 / baseUnitConversionRate;
    }

    public double getBasePriceConversionRate() {
        return basePriceConversionRate;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                product != null ? product.getId() : null,
                measurement != null ? measurement.getId() : null
        );
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof UnitOfMeasure)) return false;

        UnitOfMeasure other = (UnitOfMeasure) obj;

        return Objects.equals(
                product != null ? product.getId() : null,
                other.product != null ? other.product.getId() : null
        ) && Objects.equals(
                measurement != null ? measurement.getId() : null,
                other.measurement != null ? other.measurement.getId() : null
        );
    }

    @Override
    public String toString() {
        return "UnitOfMeasure{" +
                "product=" + (product != null ? product.getId() : null) +
                ", measurement=" + (measurement != null ? measurement.getName() : null) +
                ", price=" + price +
                ", baseUnitConversionRate=" + baseUnitConversionRate +
                '}';
    }

    /**
     * Composite key for UnitOfMeasure
     */
    public static class UnitOfMeasureId implements Serializable {
        private String product;
        private Integer measurement;

        public UnitOfMeasureId() {}

        public UnitOfMeasureId(String product, Integer measurement) {
            this.product = product;
            this.measurement = measurement;
        }

        public String getProduct() { return product; }
        public Integer getMeasurement() { return measurement; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof UnitOfMeasureId)) return false;
            UnitOfMeasureId that = (UnitOfMeasureId) o;
            return Objects.equals(product, that.product) &&
                    Objects.equals(measurement, that.measurement);
        }

        @Override
        public int hashCode() {
            return Objects.hash(product, measurement);
        }
    }
}
