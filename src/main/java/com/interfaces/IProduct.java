// File: /MediWOW-Pharmacy/src/main/java/com/interfaces/IProduct.java
package com.interfaces;

import com.entities.Lot;
import com.entities.Product;
import com.entities.UnitOfMeasure;

import java.util.List;

public interface IProduct {
    Product getProductById(String id);
    Product getProductByBarcode(String barcode);

    Lot getLotByBatchNumber(String batchNumber);
    List<Lot> getAllLots();

    UnitOfMeasure getUnitOfMeasureById(String id);
    List<UnitOfMeasure> getAllUnitOfMeasures();

    boolean addProduct(Product p);
    List<Product> getAllProducts();

    // ==== Duplicate checks for Create/Update ====
    boolean existsByBarcode(String barcode);
    boolean existsByNameAndManufacturer(String name, String manufacturer);
    boolean existsLotByBatchNumber(String batchNumber);
}
