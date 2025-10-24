package com.interfaces;

import com.entities.Product;
import com.entities.Staff;

import java.util.List;

public interface IProduct {
    public boolean addProduct(Product p);
    public List<Product> getAllProducts();
}
