package com.entities;

import com.enums.DosageForm;
import com.enums.ProductCategory;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "Product")
public class Product {

    @Id
    @Column(name = "id", updatable = false, nullable = false, length = 50)
    private String id;

    @Column(name = "barcode", unique = true)
    private String barcode;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private ProductCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "form", nullable = false)
    private DosageForm form;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "shortName")
    private String shortName;

    @Column(name = "manufacturer")
    private String manufacturer;

    @Column(name = "activeIngredient")
    private String activeIngredient;

    @Column(name = "vat")
    private double vat;

    @Column(name = "strength")
    private String strength;

    @Column(name = "description")
    private String description;

    @Column(name = "baseUnitOfMeasure", nullable = false)
    private String baseUnitOfMeasure;

    @Column(name = "image")
    private String image;

    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Lot> lotList = new HashSet<>();

    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<UnitOfMeasure> unitOfMeasureList = new HashSet<>();


    @CreationTimestamp
    @Column(name = "creationDate", updatable = false)
    private LocalDateTime creationDate;

    @UpdateTimestamp
    @Column(name = "updateDate")
    private LocalDateTime updateDate;

    public Product() {}

    public Product(String id, String barcode, ProductCategory category, DosageForm form, String name, String shortName,
                   String manufacturer, String activeIngredient, double vat, String strength, String description,
                   String baseUnitOfMeasure, Set<UnitOfMeasure> unitOfMeasureList, Set<Lot> lotList, LocalDateTime updateDate) {
        this.id = id;
        setBarcode(barcode);
        setCategory(category);
        setForm(form);
        setName(name);
        this.shortName = shortName;
        this.manufacturer = manufacturer;
        this.activeIngredient = activeIngredient;
        setVat(vat);
        this.strength = strength;
        this.description = description;
        setBaseUnitOfMeasure(baseUnitOfMeasure);
        setUnitOfMeasureList(unitOfMeasureList);
        setLotList(lotList);
        this.updateDate = updateDate;
    }

    public String getId() { return id; }

    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) {
        if (barcode == null || barcode.trim().isEmpty())
            throw new IllegalArgumentException("Mã vạch không được để trống");
        // Cho phép số 8–20 ký tự (có thể mở rộng nếu cần)
        if (!barcode.matches("\\d{8,20}"))
            throw new IllegalArgumentException("Mã vạch chỉ gồm 8–20 chữ số");
        this.barcode = barcode.trim();
    }

    public ProductCategory getCategory() { return category; }
    public void setCategory(ProductCategory category) {
        if (category == null) throw new IllegalArgumentException("Vui lòng chọn Loại sản phẩm");
        this.category = category;
    }

    public DosageForm getForm() { return form; }
    public void setForm(DosageForm form) {
        if (form == null) throw new IllegalArgumentException("Vui lòng chọn Dạng bào chế");
        this.form = form;
    }

    public String getName() { return name; }
    public void setName(String name) {
        if (name == null || name.trim().isEmpty())
            throw new IllegalArgumentException("Tên sản phẩm không được để trống");
        this.name = name.trim();
    }

    public String getShortName() { return shortName; }
    public void setShortName(String shortName) { this.shortName = (shortName == null) ? null : shortName.trim(); }

    public String getManufacturer() { return manufacturer; }
    public void setManufacturer(String manufacturer) { this.manufacturer = (manufacturer == null) ? null : manufacturer.trim(); }

    public String getActiveIngredient() { return activeIngredient; }
    public void setActiveIngredient(String activeIngredient) { this.activeIngredient = (activeIngredient == null) ? null : activeIngredient.trim(); }

    public double getVat() { return vat; }
    public void setVat(double vat) {
        if (vat < 0 || vat > 100) throw new IllegalArgumentException("VAT phải nằm trong khoảng 0–100%");
        this.vat = vat;
    }

    public String getStrength() { return strength; }
    public void setStrength(String strength) { this.strength = (strength == null) ? null : strength.trim(); }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = (description == null) ? null : description.trim(); }

    public String getBaseUnitOfMeasure() { return baseUnitOfMeasure; }
    public void setBaseUnitOfMeasure(String baseUnitOfMeasure) {
        if (baseUnitOfMeasure == null || baseUnitOfMeasure.trim().isEmpty())
            throw new IllegalArgumentException("Đơn vị tính gốc không được để trống");
        this.baseUnitOfMeasure = baseUnitOfMeasure.trim();
    }

    public String getImage() { return image; }
    public void setImage(String image) { this.image = (image == null) ? null : image.trim(); }

    public Set<UnitOfMeasure> getUnitOfMeasureList() { return unitOfMeasureList; }
    public void setUnitOfMeasureList(Set<UnitOfMeasure> unitOfMeasureList) {
        this.unitOfMeasureList = (unitOfMeasureList == null) ? new HashSet<>() : new HashSet<>(unitOfMeasureList);
        for (UnitOfMeasure u : this.unitOfMeasureList) if (u != null) u.setProduct(this); // quan hệ 2 chiều
    }

    public Set<Lot> getLotList() { return lotList; }
    public void setLotList(Set<Lot> lotList) {
        this.lotList = (lotList == null) ? new HashSet<>() : new HashSet<>(lotList);
        for (Lot l : this.lotList) if (l != null) l.setProduct(this); // quan hệ 2 chiều
    }

    public LocalDateTime getCreationDate() { return creationDate; }
    public LocalDateTime getUpdateDate() { return updateDate; }
    public void setUpdateDate(LocalDateTime updateDate) { this.updateDate = updateDate; }



    /**
     * @author Bùi Quốc Trụ
     *
     * Adds a new Lot to the product if it does not already exist.
     *
     * @param lot The Lot to add.
     * @return true if the Lot was added, false if it already exists or is null.
     */
    public boolean addLot(Lot lot) {
        if (lot == null)
            return false;

        for (Lot existingLot : lotList)
            if (existingLot.equals(lot))
                return false;

        lotList.add(lot);
        return true;
    }

    /**
     * @author Bùi Quốc Trụ
     *
     * Retrieves the oldest available Lot based on expiry date.
     *
     * @return The oldest available Lot, or null if none are available.
     */
    public Lot getOldestLotAvailable() {
        return lotList.stream()
                .filter(lot -> lot.getStatus().equals(com.enums.LotStatus.AVAILABLE) && lot.getQuantity() > 0)
                .min((lot1, lot2) -> lot1.getExpiryDate().compareTo(lot2.getExpiryDate()))
                .orElse(null);
    }

    /**
     * @author Bùi Quốc Trụ
     *
     * Updates an existing Lot with new details.
     *
     * @param updatedLot The Lot containing updated details.
     * @return true if the Lot was found and updated, false otherwise.
     */
    public boolean updateLot(Lot updatedLot) {
        if (updatedLot == null)
            return false;

        for (Lot lot : lotList) {
            if (lot.equals(updatedLot)) {
                lot.setExpiryDate(updatedLot.getExpiryDate());
                lot.setQuantity(updatedLot.getQuantity());
                lot.setRawPrice(updatedLot.getRawPrice());
                lot.setStatus(updatedLot.getStatus());

                return true;
            }
        }

        return false;
    }

    /**
     * @author Bùi Quốc Trụ
     *
     * Removes a Lot by its batch number.
     *
     * @param batchNumber The batch number of the Lot to remove.
     * @return true if the Lot was found and removed, false otherwise.
     */
    public boolean removeLot(String batchNumber) {
        return lotList.removeIf(lot -> lot.getBatchNumber().equals(batchNumber));
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
