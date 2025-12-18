package com.enums;

public class PromotionEnum {
    public enum Status { DANG_AP_DUNG, SAP_TOI, HET_HAN }

    public enum Target {
        PRODUCT("Sản phẩm"),
        ORDER_SUBTOTAL("Tổng hóa đơn");

        private final String displayName;
        Target(String displayName) { this.displayName = displayName; }
        @Override public String toString() { return displayName; }
    }

    public enum Comp {
        GREATER_EQUAL("≥"), LESS_EQUAL("≤"), GREATER(">"), LESS("<"), EQUAL("="), BETWEEN("BETWEEN");
        final String symbol; Comp(String s){this.symbol=s;} @Override public String toString(){return symbol;}
    }

    public enum ActionType {
        PERCENT_DISCOUNT("Giảm giá phần trăm"),
        FIXED_DISCOUNT("Giảm giá tiền"),
        PRODUCT_GIFT("Tặng");

        private final String displayName;
        ActionType(String displayName) { this.displayName = displayName; }
        @Override public String toString() { return displayName; }
    }

    public enum ConditionType {PRODUCT_ID, PRODUCT_QTY, ORDER_SUBTOTAL}
}
