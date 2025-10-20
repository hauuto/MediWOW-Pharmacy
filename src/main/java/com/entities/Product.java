package com.entities;

import com.enums.DosageForm;
import com.enums.ProductCategory;
import com.enums.LotStatus;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/** Entity Product (in-memory) — phục vụ CRU + tính giá cơ bản */
public class Product {
    private String id;                     // auto-gen bởi DAO
    private String barcode;
    private ProductCategory category;
    private DosageForm form;
    private String name;
    private String shortName;
    private String manufacturer;
    private String activeIngredient;
    private double vat;                    // %
    private String strength;
    private String description;
    private String baseUnitOfMeasure;      // ví dụ: "Viên", "Hộp"
    private String imagePath;
    private final List<UnitOfMeasure> unitOfMeasureList = new ArrayList<>();
    private final List<Lot> lotList = new ArrayList<>();
    private final LocalDateTime createdAt = LocalDateTime.now();

    /** Trạng thái kinh doanh (true: đang bán, false: ngừng bán) */
    private boolean active = true;

    public Product() {}

    public Product(String id, String barcode, ProductCategory category, DosageForm form,
                   String name, String shortName, String manufacturer, String activeIngredient,
                   double vat, String strength, String description, String baseUnitOfMeasure) {
        this.id = id;
        this.barcode = barcode;
        this.category = category;
        this.form = form;
        this.name = name;
        this.shortName = shortName;
        this.manufacturer = manufacturer;
        this.activeIngredient = activeIngredient;
        this.vat = vat;
        this.strength = strength;
        this.description = description;
        this.baseUnitOfMeasure = baseUnitOfMeasure;
    }

    // ---- getters/setters ----
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }
    public ProductCategory getCategory() { return category; }
    public void setCategory(ProductCategory category) { this.category = category; }
    public DosageForm getForm() { return form; }
    public void setForm(DosageForm form) { this.form = form; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getShortName() { return shortName; }
    public void setShortName(String shortName) { this.shortName = shortName; }
    public String getManufacturer() { return manufacturer; }
    public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; }
    public String getActiveIngredient() { return activeIngredient; }
    public void setActiveIngredient(String activeIngredient) { this.activeIngredient = activeIngredient; }
    public double getVat() { return vat; }
    public void setVat(double vat) { this.vat = vat; }
    public String getStrength() { return strength; }
    public void setStrength(String strength) { this.strength = strength; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getBaseUnitOfMeasure() { return baseUnitOfMeasure; }
    public void setBaseUnitOfMeasure(String baseUnitOfMeasure) { this.baseUnitOfMeasure = baseUnitOfMeasure; }
    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }
    public List<UnitOfMeasure> getUnitOfMeasureList() { return unitOfMeasureList; }
    public List<Lot> getLotList() { return lotList; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    // ---- compose helpers (đủ dùng cho demo) ----
    public boolean addUnitOfMeasure(UnitOfMeasure uom) {
        if (uom == null) return false;
        return unitOfMeasureList.add(uom);
    }
    public boolean addLot(Lot lot) {
        if (lot == null) return false;
        return lotList.add(lot);
    }

    /** Dùng cho tính giá bán (tham chiếu từ InvoiceLine), ưu tiên lô sắp hết hạn nhưng AVAILABLE */
    public Lot getOldestLotAvailable() {
        return lotList.stream()
                .filter(l -> l.getStatus() == LotStatus.AVAILABLE)
                .sorted(Comparator.comparing(Lot::getExpiryDate))
                .findFirst().orElse(null);
    }

    @Override public String toString() {
        return name + (strength != null && !strength.isBlank() ? " " + strength : "");
    }

    @Override public int hashCode() { return Objects.hashCode(id); }
    @Override public boolean equals(Object o){
        if (this == o) return true;
        if (!(o instanceof Product p)) return false;
        return Objects.equals(id, p.id);
    }
}
