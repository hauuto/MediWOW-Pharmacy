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

    /**
     * Search top 5 lots by batch number (contains, case-insensitive) for omni-search.
     */
    List<Lot> searchTop5LotsByBatchNumber(String keyword);

    UnitOfMeasure getUnitOfMeasureById(String productId, Integer measurementId);

    Lot getLotById(String id);
    List<Lot> getAllLots();

    UnitOfMeasure getUnitOfMeasureById(String productId, String name);
    List<UnitOfMeasure> getAllUnitOfMeasures();

    boolean addProduct(Product p);
    boolean updateProduct(Product p);
    List<Product> getAllProducts();

    // ==== Duplicate checks for Create/Update ====
    boolean existsByBarcode(String barcode);
    boolean existsByNameAndManufacturer(String name, String manufacturer);
    boolean existsLotByBatchNumber(String batchNumber);
}
