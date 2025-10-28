package com.entities;

import com.enums.PromotionEnum.*;
import jakarta.persistence.*;

/*
@author Nguyễn Thanh Khôi
 */
@Entity
@Table(name = "PromotionAction")
public class PromotionAction {

    @Id
    @Column(length = 50) // ID do trigger SQL Server sinh: PRMA-XXXXXX
    private String id;

    // Many actions → 1 Promotion
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promotion", nullable = false)
    private Promotion promotion;

    @Column(name = "actionOrder", nullable = false)
    private int actionOrder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Target target;

    @Column(name = "primaryValue", nullable = false)
    private Double primaryValue;

    @Column(name = "secondaryValue", nullable = true)
    private Double secondaryValue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product", nullable = true)
    private Product product;





    public PromotionAction() {}

    public PromotionAction(ActionType type, Target target, Double primaryValue,
                           Double secondaryValue, Product product, int order) {
        this.type = type;
        this.target = target;
        this.primaryValue = primaryValue;
        this.secondaryValue = secondaryValue;
        this.product = product;
        this.actionOrder = order;
    }

    public String getId() { return id; }

    public void setId(String id) { this.id = id; }

    public ActionType getType() { return type; }
    public void setType(ActionType type) { this.type = type; }

    public Promotion getPromotion() {
        return promotion;
    }

    public void setPromotion(Promotion promotion) {
        this.promotion = promotion;
    }

    public int getActionOrder() {
        return actionOrder;
    }

    public void setActionOrder(int actionOrder) {
        this.actionOrder = actionOrder;
    }

    public Target getTarget() {
        return target;
    }

    public void setTarget(Target target) {
        this.target = target;
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
