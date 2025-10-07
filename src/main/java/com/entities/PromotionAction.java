package com.entities;

import com.enums.PromotionEnum.*;

public class PromotionAction {
    private ActionType type;
    private Target target;           // áp dụng lên ORDER_SUBTOTAL / PRODUCT
    private Double value;            // % hoặc số tiền hoặc số lượng quà
    private String productOrGift;    // nếu là PRODUCT_GIFT
    private int order;

    public PromotionAction(ActionType t, Target ta, Double v, String p, int o){
        type=t; target=ta; value=v; productOrGift=p; order=o;
    }

    public ActionType getType() {
        return type;
    }

    public void setType(ActionType type) {
        this.type = type;
    }

    public Target getTarget() {
        return target;
    }

    public void setTarget(Target target) {
        this.target = target;
    }

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }

    public String getProductOrGift() {
        return productOrGift;
    }

    public void setProductOrGift(String productOrGift) {
        this.productOrGift = productOrGift;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }
}