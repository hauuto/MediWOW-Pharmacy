package com.entities;

import com.enums.PromotionEnum.*;
import jakarta.persistence.*;

/*
@author Nguyễn Thanh Khôi
 */
@Entity
@Table(name = "PromotionCondition")
public class PromotionCondition {

    @Id
    @Column(length = 50) // ID được DB trigger sinh tự động: PRMC-XXXXXX
    private String id;

    // Many conditions → 1 Promotion
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promotion", nullable = false)
    private Promotion promotion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name="type")
    private ConditionType conditionType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Comp comparator;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Target target;

    @Column(nullable = false, name="primaryValue")
    private Double primaryValue;

    @Column(nullable = true, name = "secondaryValue")
    private Double secondaryValue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product", nullable = true)
    private Product product;

    public PromotionCondition() {}

    public PromotionCondition(Target target, Comp comparator, ConditionType type,
                              Double primaryValue, Double secondaryValue, Product product) {
        this.target = target;
        this.comparator = comparator;
        this.conditionType = type;
        this.primaryValue = primaryValue;
        this.secondaryValue = secondaryValue;
        this.product = product;
    }

    public String getId() { return id; }

    public Target getTarget() { return target; }
    public void setTarget(Target target) { this.target = target; }

    public Comp getComparator() { return comparator; }
    public void setComparator(Comp comparator) { this.comparator = comparator; }

    public Promotion getPromotion() {
        return promotion;
    }

    public void setPromotion(Promotion promotion) {
        this.promotion = promotion;
    }

    public ConditionType getConditionType() {
        return conditionType;
    }

    public void setConditionType(ConditionType conditionType) {
        this.conditionType = conditionType;
    }

    public Double getPrimaryValue() {
        return primaryValue;
    }

    public void setPrimaryValue(Double primaryValue) {
        this.primaryValue = primaryValue;
    }

    public Double getSecondaryValue() {
        return secondaryValue;
    }

    public void setSecondaryValue(Double secondaryValue) {
        this.secondaryValue = secondaryValue;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }
}
