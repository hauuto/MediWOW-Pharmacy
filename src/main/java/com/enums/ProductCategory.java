package com.enums;

import java.math.BigDecimal;

public enum ProductCategory {
    SUPPLEMENT, OTC, ETC;

    /** Mặc định VAT theo yêu cầu: OTC/ETC = 5%, SUPPLEMENT = 10%. */
    public BigDecimal defaultVat() {
        return switch (this) {
            case SUPPLEMENT -> BigDecimal.TEN;
            case OTC, ETC -> BigDecimal.valueOf(5);
        };
    }

    /** Legacy helper for older UI components/widgets. */
    public double defaultVatAsDouble() {
        return defaultVat().doubleValue();
    }
}
