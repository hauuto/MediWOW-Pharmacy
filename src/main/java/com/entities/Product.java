package com.entities;

import com.enums.DosageForm;
import com.enums.ProductCategory;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Bùi Quốc Trụ, Nguyễn Thanh Khôi
 */
@Entity
@Table(name = "Product")
public class Product {


    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false, length = 50)
    private String id;

    @Column(name = "barcode")
    private String barcode;

    @Enumerated(EnumType.STRING)
    @Column(name = "category")
    private ProductCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "form")
    private DosageForm form;

    @Column(name = "name")
    private String name;

    @Column(name = "shortName") // ✅ FIX
    private String shortName;

    @Column(name = "manufacturer")
    private String manufacturer;

    @Column(name = "activeIngredient") // ✅ FIX
    private String activeIngredient;

    @Column(name = "vat")
    private double vat;

    @Column(name = "strength")
    private String strength;

    @Column(name = "description")
    private String description;

    @Column(name = "baseUnitOfMeasure") // ✅ FIX
    private String baseUnitOfMeasure;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<UnitOfMeasure> unitOfMeasureList = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Lot> lotList = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "creationDate", updatable = false)
    private LocalDateTime creationDate;

    @UpdateTimestamp
    @Column(name = "updateDate")
    private LocalDateTime updateDate;

    protected Product() {}

    // Existing constructor updated to no longer forcibly set creationDate (Hibernate will handle it)
    public Product(String id, String barcode, ProductCategory category, DosageForm form, String name, String shortName, String manufacturer, String activeIngredient, double vat, String strength, String description, String baseUnitOfMeasure, List<UnitOfMeasure> unitOfMeasureList, List<Lot> lotList, LocalDateTime updateDate) {
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
        this.unitOfMeasureList = unitOfMeasureList;
        this.lotList = lotList;
        // creationDate will be populated by @CreationTimestamp
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

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
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

    public LocalDateTime getUpdateDate() {
        return updateDate;
    }

    public void setUpdateDate(LocalDateTime updateDate) {
        this.updateDate = updateDate;
    }

    /**
     * @author Bùi Quốc Trụ
     *
     * Adds a new UnitOfMeasure to the product if it does not already exist.
     *
     * @param uom The UnitOfMeasure to add.
     * @return true if the UnitOfMeasure was added, false if it already exists or is null.
     */
    public boolean addUnitOfMeasure(UnitOfMeasure uom) {
        if (uom == null)
            return false;

        for (UnitOfMeasure existingUom : unitOfMeasureList)
            if (existingUom.equals(uom))
                return false;

        unitOfMeasureList.add(uom);
        return true;
    }

    /**
     * @author Bùi Quốc Trụ
     *
     * Updates an existing UnitOfMeasure with new details.
     *
     * @param updatedUom The UnitOfMeasure containing updated details.
     * @return true if the UnitOfMeasure was found and updated, false otherwise.
     */
    public boolean updateUnitOfMeasure(UnitOfMeasure updatedUom) {
        if (updatedUom == null)
            return false;

        for (UnitOfMeasure uom : unitOfMeasureList) {
            if (uom.equals(updatedUom)) {
                uom.setBaseUnitConversionRate(updatedUom.getBaseUnitConversionRate());
                uom.setName(updatedUom.getName());

                return true;
            }
        }

        return false;
    }

    /**
     * @author Bùi Quốc Trụ
     *
     * Removes a UnitOfMeasure by its ID.
     *
     * @param uomId The ID of the UnitOfMeasure to remove.
     * @return true if the UnitOfMeasure was found and removed, false otherwise.
     */
    public boolean removeUnitOfMeasure(String uomId) {
        return unitOfMeasureList.removeIf(uom -> uom.getId().equals(uomId));
    }

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
