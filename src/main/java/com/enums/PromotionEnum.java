package com.enums;

public class PromotionEnum {
    public enum Status { DANG_AP_DUNG, SAP_TOI, HET_HAN }
    public enum Target { PRODUCT, ORDER_SUBTOTAL, PRODUCT_QTY }
    public enum Comp {
        GREATER_EQUAL("≥"), LESS_EQUAL("≤"), GREATER(">"), LESS("<"), EQUAL("="), BETWEEN("BETWEEN");
        final String symbol; Comp(String s){this.symbol=s;} @Override public String toString(){return symbol;}
    }
    public enum ActionType { PERCENT_DISCOUNT, FIXED_DISCOUNT, PRODUCT_GIFT }
    public enum ConditionType {PRODUCT_ID, PRODUCT_QTY, ORDER_SUBTOTAL}
}
