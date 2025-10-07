package com.entities;

import com.enums.PromotionEnum.*;

public class PromotionCondition {
    private Target target;
    private Comp comparator;
    private Double primaryValue;     // ví dụ: ngưỡng tiền, số lượng
    private Double secondaryValue;   // cho BETWEEN
    private String product;          // nếu target liên quan product

    public PromotionCondition(Target t, Comp c, Double v1, Double v2, String p){
        target=t; comparator=c; primaryValue=v1; secondaryValue=v2; product=p;
    }

    public Target getTarget() {
        return target;
    }

    public void setTarget(Target target) {
        this.target = target;
    }

    public Comp getComparator() {
        return comparator;
    }

    public void setComparator(Comp comparator) {
        this.comparator = comparator;
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

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }
}