package com.dao;

import com.entities.Lot;
import com.entities.Product;
import com.entities.UnitOfMeasure;

import java.math.BigDecimal;

public class DAO_UnitOfMeasure {

    public DAO_UnitOfMeasure() {
    }

    public UnitOfMeasure findUnitOfMeasure(Product product, String name) {
        if (name.equals(product.getBaseUnitOfMeasure())) {
            for (UnitOfMeasure uom : product.getUnitOfMeasureSet())
                if (uom.getName().equals(name)) return uom;
        }
        for (UnitOfMeasure uom : product.getUnitOfMeasureSet())
            if (uom.getName().equals(name)) return uom;
        return null;
    }

    public int getRemainingInventoryInUOM(Product product, String uomName) {
        int totalBaseQuantity = product.getLotSet().stream()
                .filter(lot -> lot.getStatus() == com.enums.LotStatus.AVAILABLE && lot.getQuantity() > 0)
                .mapToInt(Lot::getQuantity)
                .sum();
        if (uomName.equals(product.getBaseUnitOfMeasure())) {
            return totalBaseQuantity;
        }
        UnitOfMeasure uom = findUnitOfMeasure(product, uomName);
        if (uom == null || uom.getBaseUnitConversionRate() == null || uom.getBaseUnitConversionRate().compareTo(BigDecimal.ZERO) == 0) {
            return totalBaseQuantity;
        }
        BigDecimal remaining = BigDecimal.valueOf(totalBaseQuantity).multiply(uom.getBaseUnitConversionRate());
        return remaining.setScale(0, java.math.RoundingMode.FLOOR).intValue();
    }
}
