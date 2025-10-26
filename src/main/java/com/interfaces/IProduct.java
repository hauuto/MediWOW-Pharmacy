package com.interfaces;

import com.entities.Lot;
import com.entities.Product;
import com.entities.Staff;
import com.entities.UnitOfMeasure;

import java.util.List;

public interface IProduct {
    public Product getProductById(String id);
    public Lot getLotByBatchNumber(String batchNumber);
    public List<Lot> getAllLots();
    public UnitOfMeasure getUnitOfMeasureById(String id);
    public List<UnitOfMeasure> getAllUnitOfMeasures();
    public boolean addProduct(Product p);
    public List<Product> getAllProducts();
}
