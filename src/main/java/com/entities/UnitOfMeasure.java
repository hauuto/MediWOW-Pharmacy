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
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "measurementId", nullable = false)
    private MeasurementName measurement;

    @Column(name = "price", nullable = false, precision = 18, scale = 2)
    private BigDecimal price;

    @Column(name = "baseUnitConversionRate", nullable = false, precision = 18, scale = 4)
    private BigDecimal baseUnitConversionRate;

    @Transient
    private BigDecimal basePriceConversionRate;

    protected UnitOfMeasure() {}

    public UnitOfMeasure(Product product,
                         MeasurementName measurement,
                         BigDecimal price,
                         BigDecimal baseUnitConversionRate) {
        this.product = product;
        this.measurement = measurement;
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

    @Transient
    public String getName() {
        return measurement.getName();
    }

    @Transient
    public void setName(String name) {
        this.measurement.setName(name);
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
     * Composite key class for UnitOfMeasure
     */
    public static class UnitOfMeasureId implements Serializable {
        private String product;
        private Integer measurement;

        public UnitOfMeasureId() {}

        public UnitOfMeasureId(String product, Integer measurement) {
            this.product = product;
            this.measurement = measurement;
        }

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
