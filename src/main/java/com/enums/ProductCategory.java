package com.enums;

public enum ProductCategory {
    SUPPLEMENT, OTC, ETC;

    /** Mặc định VAT theo yêu cầu: OTC/ETC = 5%, SUPPLEMENT = 10%. */
    public double defaultVat() {
        return switch (this) {
            case SUPPLEMENT -> 10.0;
            case OTC, ETC -> 5.0;
        };
    }
}
