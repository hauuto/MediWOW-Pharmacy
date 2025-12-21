package com.entities;

import com.enums.PromotionEnum.*;
import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;

@Entity
@Table(name = "PromotionCondition")
public class PromotionCondition {

    @Id
    @UuidGenerator
    @Column(name = "id", insertable = false, updatable = false, nullable = false, length = 50)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promotion", nullable = false)
    private Promotion promotion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "type")
    private ConditionType conditionType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Comp comparator;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Target target;

    @Column(name = "value", precision = 18, scale = 2)
    private BigDecimal value;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "product", referencedColumnName = "product", insertable = true, updatable = true),
            @JoinColumn(name = "unitOfMeasure", referencedColumnName = "measurementId", insertable = true, updatable = true)
    })
    private UnitOfMeasure productUOM;

    // ======================
    // Constructors
    // ======================
    public PromotionCondition() {}

    public PromotionCondition(
            Target target,
            Comp comparator,
            ConditionType conditionType,
            BigDecimal value,
            UnitOfMeasure productUOM
    ) {
        this.target = target;
        this.comparator = comparator;
        this.conditionType = conditionType;
        this.value = value;
        this.productUOM = productUOM;
    }

    // ======================
    // Getters / Setters
    // ======================
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Promotion getPromotion() { return promotion; }
    public void setPromotion(Promotion promotion) { this.promotion = promotion; }

    public ConditionType getConditionType() { return conditionType; }
    public void setConditionType(ConditionType conditionType) { this.conditionType = conditionType; }

    public Comp getComparator() { return comparator; }
    public void setComparator(Comp comparator) { this.comparator = comparator; }

    public Target getTarget() { return target; }
    public void setTarget(Target target) { this.target = target; }

    public BigDecimal getValue() { return value; }
    public void setValue(BigDecimal value) { this.value = value; }

    public UnitOfMeasure getProductUOM() { return productUOM; }
    public void setProductUOM(UnitOfMeasure productUOM) { this.productUOM = productUOM; }

    // ======================
    // Compatibility Helpers
    // ======================
    public Product getProduct() {
        return (productUOM != null) ? productUOM.getProduct() : null;
    }

    // ======================
    // Business helpers
    // ======================

    /** Preferred accessor for comparing conditions (BigDecimal). */
    public BigDecimal getPrimaryValue() {
        return value != null ? value : BigDecimal.ZERO;
    }
}
