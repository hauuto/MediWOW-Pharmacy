package com.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Money utility helpers.
 *
 * Assumptions (current DB schema): money stored as DECIMAL(18,2).
 * If you want VND integer-only, change MONEY_SCALE to 0 and review rounding.
 */
public final class MoneyUtil {
    /** Default scale for money amounts. */
    public static final int MONEY_SCALE = 2;

    /** Default rounding for money operations. */
    public static final RoundingMode MONEY_ROUNDING = RoundingMode.HALF_UP;

    private MoneyUtil() {}

    public static BigDecimal zero() {
        return BigDecimal.ZERO.setScale(MONEY_SCALE, MONEY_ROUNDING);
    }

    /**
     * Prefer {@link #ofBigDecimal(BigDecimal)} or {@link #ofString(String)} when possible.
     * This method exists for UI/widget interoperability.
     */
    public static BigDecimal of(double value) {
        return BigDecimal.valueOf(value).setScale(MONEY_SCALE, MONEY_ROUNDING);
    }

    public static BigDecimal ofBigDecimal(BigDecimal value) {
        return scale(value);
    }

    /**
     * Parses a decimal string safely and normalizes to MONEY_SCALE.
     * Useful to avoid double binary fractions.
     */
    public static BigDecimal ofString(String value) {
        if (value == null || value.trim().isEmpty()) return zero();
        return scale(new BigDecimal(value.trim()));
    }

    public static BigDecimal scale(BigDecimal value) {
        if (value == null) return zero();
        return value.setScale(MONEY_SCALE, MONEY_ROUNDING);
    }

    /**
     * Divides safely with default money scale.
     */
    public static BigDecimal divide(BigDecimal dividend, BigDecimal divisor) {
        if (dividend == null) dividend = BigDecimal.ZERO;
        if (divisor == null || divisor.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO.setScale(MONEY_SCALE, MONEY_ROUNDING);
        return dividend.divide(divisor, MONEY_SCALE, MONEY_ROUNDING);
    }

    /**
     * Used for percentage rates, e.g. 10 (%) -> 0.10.
     */
    public static BigDecimal percentToRate(BigDecimal percent) {
        if (percent == null) return BigDecimal.ZERO;
        return percent.divide(BigDecimal.valueOf(100), 10, MONEY_ROUNDING);
    }
}
