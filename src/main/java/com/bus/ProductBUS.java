package com.bus;

import com.dao.ProductDAO;
import com.entities.Product;
import com.enums.DosageForm;
import com.enums.ProductCategory;

import java.util.List;
import java.util.Optional;

public class ProductBUS {
    private final ProductDAO dao = ProductDAO.getInstance();

    public List<Product> getAll() { return dao.findAll(); }

    public List<Product> search(String keyword, ProductCategory cat, DosageForm form, Boolean active) {
        return dao.search(keyword, cat, form, active);
    }

    public Product add(Product p) {
        validate(p, true);
        return dao.create(p);
    }

    public boolean update(Product p) {
        validate(p, false);
        return dao.update(p);
    }

    public boolean setActive(String id, boolean active) { return dao.setActive(id, active); }

    public Optional<Product> findById(String id) { return dao.findById(id); }

    private void validate(Product p, boolean creating) {
        if (p == null) throw new IllegalArgumentException("Sản phẩm không hợp lệ");
        if (p.getName() == null || p.getName().isBlank())
            throw new IllegalArgumentException("Tên sản phẩm là bắt buộc");
        if (p.getCategory() == null)
            throw new IllegalArgumentException("Chưa chọn loại sản phẩm");
        if (p.getForm() == null)
            throw new IllegalArgumentException("Chưa chọn dạng bào chế");
        if (p.getBaseUnitOfMeasure() == null || p.getBaseUnitOfMeasure().isBlank())
            throw new IllegalArgumentException("Đơn vị cơ bản là bắt buộc");
        if (p.getVat() < 0 || p.getVat() > 100)
            throw new IllegalArgumentException("VAT phải trong khoảng 0–100");
    }
}
