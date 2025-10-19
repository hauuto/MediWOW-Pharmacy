package com.entities;

import com.enums.DosageForm;
import com.enums.ProductCategory;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author Bùi Quốc Trụ
 */
public class Product {
    private final String id;
    private String barcode;
    private ProductCategory category;
    private DosageForm form;
    private String name;
    private String shortName;
    private String manufaturer;
    private String activeIngredient;
    private double vat;
    private String strength;
    private String description;
    private String baseUnitOfMeasure;
    private List<UnitOfMeasure> unitOfMeasureList;
    private List<Lot> lotList;
    private LocalDateTime creationDate;
    private LocalDateTime updateDate;

    public Product(String id, String barcode, ProductCategory category, DosageForm form, String name, String shortName, String manufaturer, String activeIngredient, double vat, String strength, String description, String baseUnitOfMeasure, List<UnitOfMeasure> unitOfMeasureList, List<Lot> lotList, LocalDateTime creationDate, LocalDateTime updateDate) {
        this.id = id;
        this.barcode = barcode;
        this.category = category;
        this.form = form;
        this.name = name;
        this.shortName = shortName;
        this.manufaturer = manufaturer;
        this.activeIngredient = activeIngredient;
        this.vat = vat;
        this.strength = strength;
        this.description = description;
        this.baseUnitOfMeasure = baseUnitOfMeasure;
        this.unitOfMeasureList = unitOfMeasureList;
        this.lotList = lotList;
        this.creationDate = creationDate;
        this.updateDate = updateDate;
    }

    public String getId() {
        return id;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public ProductCategory getCategory() {
        return category;
    }

    public void setCategory(ProductCategory category) {
        this.category = category;
    }

    public DosageForm getForm() {
        return form;
    }

    public void setForm(DosageForm form) {
        this.form = form;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getShortName() {
        return shortName;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    public String getManufaturer() {
        return manufaturer;
    }

    public void setManufaturer(String manufaturer) {
        this.manufaturer = manufaturer;
    }

    public String getActiveIngredient() {
        return activeIngredient;
    }

    public void setActiveIngredient(String activeIngredient) {
        this.activeIngredient = activeIngredient;
    }

    public double getVat() {
        return vat;
    }

    public void setVat(double vat) {
        this.vat = vat;
    }

    public String getStrength() {
        return strength;
    }

    public void setStrength(String strength) {
        this.strength = strength;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getBaseUnitOfMeasure() {
        return baseUnitOfMeasure;
    }

    public void setBaseUnitOfMeasure(String baseUnitOfMeasure) {
        this.baseUnitOfMeasure = baseUnitOfMeasure;
    }

    public List<UnitOfMeasure> getUnitOfMeasureList() {
        return unitOfMeasureList;
    }

    public void setUnitOfMeasureList(List<UnitOfMeasure> unitOfMeasureList) {
        this.unitOfMeasureList = unitOfMeasureList;
    }

    public List<Lot> getLotList() {
        return lotList;
    }

    public void setLotList(List<Lot> lotList) {
        this.lotList = lotList;
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
    }

    public LocalDateTime getUpdateDate() {
        return updateDate;
    }

    public void setUpdateDate(LocalDateTime updateDate) {
        this.updateDate = updateDate;
    }

//    /**
//     * Adds a new UnitOfMeasure to the product if it does not already exist.
//     *
//     * @param uom The UnitOfMeasure to add.
//     * @return true if the UnitOfMeasure was added, false if it already exists or is null.
//     */
//    public boolean addUnitOfMeasure(UnitOfMeasure uom) {
//        if (uom == null)
//            return false;
//
//        for (UnitOfMeasure existingUom : unitOfMeasureList)
//            if (existingUom.equals(uom))
//                return false;
//
//        unitOfMeasureList.add(uom);
//        return true;
//    }
//
//    /**
//     * Retrieves a UnitOfMeasure by its ID.
//     *
//     * @param uomId The ID of the UnitOfMeasure to retrieve.
//     * @return The UnitOfMeasure with the specified ID, or null if not found.
//     */
//    public UnitOfMeasure getUnitOfMeasureById(String uomId) {
//        for (UnitOfMeasure uom : unitOfMeasureList)
//            if (uom.getId().equals(uomId))
//                return uom;
//
//        return null;
//    }
//
//    public boolean updateUnitOfMeasure(UnitOfMeasure updatedUom) {
//        for (UnitOfMeasure uom : unitOfMeasureList) {
//            if (uom.equals(updatedUom)) {
//                uom.set
//            }
//        }
//
//        return false;
//    }

    @Override
    public String toString() {
        return super.toString();
    }
}
