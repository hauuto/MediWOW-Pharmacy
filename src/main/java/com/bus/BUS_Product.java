package com.bus;

import com.dao.DAO_Product;
import com.entities.Lot;
import com.entities.Product;
import com.entities.UnitOfMeasure;
import com.interfaces.IProduct;

import java.util.List;

/**
 * @author Nguyễn Thanh Khôi
 */
public class BUS_Product implements IProduct {

    private final DAO_Product dao;

    public BUS_Product() {
        this.dao = new DAO_Product();
    }

    @Override
    public Product getProductById(String id) {
        if (id == null || id.isBlank()) return null;
        return dao.getProductById(id);
    }

    @Override
    public Lot getLotByBatchNumber(String batchNumber) {
        if (batchNumber == null || batchNumber.isBlank()) return null;
        return dao.getLotByBatchNumber(batchNumber);
    }

    @Override
    public List<Lot> getAllLots() {
        return dao.getAllLots();
    }

    @Override
    public UnitOfMeasure getUnitOfMeasureById(String id) {
        if (id == null || id.isBlank()) return null;
        return dao.getUnitOfMeasureById(id);
    }

    @Override
    public List<UnitOfMeasure> getAllUnitOfMeasures() {
        return dao.getAllUnitOfMeasures();
    }

    @Override
    public boolean addProduct(Product p) {

        // ==== VALIDATION ====
        if (p == null) {
            System.err.println("❌ Product null");
            return false;
        }

        if (p.getName() == null || p.getName().isBlank()) {
            System.err.println("❌ Tên sản phẩm không được để trống");
            return false;
        }

        if (p.getCategory() == null) {
            System.err.println("❌ Danh mục chưa chọn");
            return false;
        }

        if (p.getForm() == null) {
            System.err.println("❌ Dạng bào chế chưa chọn");
            return false;
        }

        if (p.getVat() < 0) {
            System.err.println("❌ VAT không thể âm");
            return false;
        }

        // Nếu có UnitOfMeasure đi kèm → gán quan hệ 2 chiều
        for (UnitOfMeasure u : p.getUnitOfMeasureList()) {
            u.setProduct(p);
        }

        // Nếu có Lot đi kèm → gán quan hệ 2 chiều
        for (Lot lot : p.getLotList()) {
            lot.setProduct(p);
        }

        return dao.addProduct(p);
    }

    @Override
    public List<Product> getAllProducts() {
        return dao.getAllProducts();
    }
}
