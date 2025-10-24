package com.interfaces;

import com.entities.Lot;
import com.entities.Product;
import com.entities.UnitOfMeasure;

import java.util.List;

public interface IProduct {
    Product getProductById(String id);
    List<Product> getAllProducts();
    Lot getLotByBatchNumber(String batchNumber);
    List<Lot> getAllLots();
    UnitOfMeasure getUnitOfMeasureById(String id);
    List<UnitOfMeasure> getAllUnitOfMeasures();
}
