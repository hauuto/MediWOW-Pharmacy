package com.bus;

import com.dao.DAO_Product;
import com.entities.Lot;
import com.entities.Product;
import com.entities.UnitOfMeasure;
import com.interfaces.IProduct;

import java.util.List;

public class BUS_Product implements IProduct {
    private final DAO_Product daoProduct;

    public BUS_Product() {
        this.daoProduct = new DAO_Product();
    }

    @Override
    public Product getProductById(String id) {
        return daoProduct.getProductById(id);
    }

    @Override
    public Lot getLotByBatchNumber(String batchNumber) {
        return daoProduct.getLotByBatchNumber(batchNumber);
    }

    @Override
    public List<Lot> getAllLots() {
        return daoProduct.getAllLots();
    }

    @Override
    public UnitOfMeasure getUnitOfMeasureById(String id) {
        return daoProduct.getUnitOfMeasureById(id);
    }

    @Override
    public List<UnitOfMeasure> getAllUnitOfMeasures() {
        return daoProduct.getAllUnitOfMeasures();
    }

    @Override
    public boolean addProduct(Product p) {
        return daoProduct.addProduct(p);
    }

    @Override
    public List<Product> getAllProducts() {
        return daoProduct.getAllProducts();
    }
}
