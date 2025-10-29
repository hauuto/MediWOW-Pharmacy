package com.bus;

import com.dao.DAO_UnitOfMeasure;
import com.entities.Product;
import com.entities.UnitOfMeasure;

import java.util.List;

/**
 * Business Logic Layer for UnitOfMeasure entity
 * @author Tô Thanh Hậu
 */
public class BUS_UnitOfMeasure {
    private final DAO_UnitOfMeasure daoUnitOfMeasure;

    public BUS_UnitOfMeasure() {
        this.daoUnitOfMeasure = new DAO_UnitOfMeasure();
    }

    /**
     * Add a new unit of measure
     * @param uom the unit of measure to add
     * @return true if successful, false otherwise
     */
    public boolean addUnitOfMeasure(UnitOfMeasure uom) {
        if (uom == null) {
            return false;
        }

        // Check if ID already exists (only if ID is manually set)
        if (uom.getId() != null && !uom.getId().isEmpty()) {
            if (daoUnitOfMeasure.findById(uom.getId()) != null) {
                return false;
            }
        }

        // Validate conversion rate
        if (uom.getBaseUnitConversionRate() <= 0) {
            return false;
        }

        return daoUnitOfMeasure.add(uom);
    }

    /**
     * Update an existing unit of measure
     * @param uom the unit of measure to update
     * @return true if successful, false otherwise
     */
    public boolean updateUnitOfMeasure(UnitOfMeasure uom) {
        if (uom == null || uom.getId() == null || uom.getId().isEmpty()) {
            return false;
        }

        // Validate conversion rate
        if (uom.getBaseUnitConversionRate() <= 0) {
            return false;
        }

        return daoUnitOfMeasure.update(uom);
    }

    /**
     * Delete a unit of measure by ID
     * @param id the ID of the unit of measure to delete
     * @return true if successful, false otherwise
     */
    public boolean deleteUnitOfMeasure(String id) {
        if (id == null || id.isEmpty()) {
            return false;
        }

        return daoUnitOfMeasure.delete(id);
    }

    /**
     * Find a unit of measure by ID
     * @param id the ID to search for
     * @return the unit of measure if found, null otherwise
     */
    public UnitOfMeasure getUnitOfMeasureById(String id) {
        if (id == null || id.isEmpty()) {
            return null;
        }

        return daoUnitOfMeasure.findById(id);
    }

    /**
     * Get all units of measure
     * @return list of all units of measure
     */
    public List<UnitOfMeasure> getAllUnitsOfMeasure() {
        return daoUnitOfMeasure.getAll();
    }

    /**
     * Get all units of measure for a specific product
     * @param product the product to get units for
     * @return list of units of measure for the product
     */
    public List<UnitOfMeasure> getUnitsByProduct(Product product) {
        if (product == null) {
            return List.of();
        }

        return daoUnitOfMeasure.getByProduct(product);
    }

    /**
     * Get units of measure by product ID
     * @param productId the product ID to search for
     * @return list of units of measure for the product
     */
    public List<UnitOfMeasure> getUnitsByProductId(String productId) {
        if (productId == null || productId.isEmpty()) {
            return List.of();
        }

        return daoUnitOfMeasure.getByProductId(productId);
    }

    /**
     * Find unit of measure by name for a specific product
     * @param product the product
     * @param name the unit name to search for
     * @return the unit of measure if found, null otherwise
     */
    public UnitOfMeasure findByProductAndName(Product product, String name) {
        if (product == null || name == null || name.isEmpty()) {
            return null;
        }

        return daoUnitOfMeasure.findByProductAndName(product, name);
    }

    /**
     * Get the base unit of measure for a product (conversion rate = 1)
     * @param product the product
     * @return the base unit of measure or null if not found
     */
    public UnitOfMeasure getBaseUnit(Product product) {
        if (product == null) {
            return null;
        }

        return daoUnitOfMeasure.getBaseUnit(product);
    }

    /**
     * Convert quantity from one unit to another
     * @param fromUnit the source unit
     * @param toUnit the target unit
     * @param quantity the quantity to convert
     * @return the converted quantity
     */
    public double convertQuantity(UnitOfMeasure fromUnit, UnitOfMeasure toUnit, double quantity) {
        if (fromUnit == null || toUnit == null || quantity < 0) {
            return 0;
        }

        // Check if units belong to the same product
        if (!fromUnit.getProduct().equals(toUnit.getProduct())) {
            return 0;
        }

        // Convert to base unit first, then to target unit
        double baseQuantity = quantity * fromUnit.getBaseUnitConversionRate();
        return baseQuantity / toUnit.getBaseUnitConversionRate();
    }

    /**
     * Convert price from one unit to another
     * @param fromUnit the source unit
     * @param toUnit the target unit
     * @param price the price to convert
     * @return the converted price
     */
    public double convertPrice(UnitOfMeasure fromUnit, UnitOfMeasure toUnit, double price) {
        if (fromUnit == null || toUnit == null || price < 0) {
            return 0;
        }

        // Check if units belong to the same product
        if (!fromUnit.getProduct().equals(toUnit.getProduct())) {
            return 0;
        }

        // Convert to base unit price first, then to target unit price
        double basePrice = price * fromUnit.getBasePriceConversionRate();
        return basePrice / toUnit.getBasePriceConversionRate();
    }

    /**
     * Get price for a specific unit based on base unit price
     * @param unit the unit to calculate price for
     * @param baseUnitPrice the price of the base unit
     * @return the price for the specified unit
     */
    public double getPriceForUnit(UnitOfMeasure unit, double baseUnitPrice) {
        if (unit == null || baseUnitPrice < 0) {
            return 0;
        }

        return baseUnitPrice * unit.getBaseUnitConversionRate();
    }

    /**
     * Calculate base unit price from a unit's price
     * @param unit the unit
     * @param unitPrice the price of the unit
     * @return the base unit price
     */
    public double getBaseUnitPrice(UnitOfMeasure unit, double unitPrice) {
        if (unit == null || unitPrice < 0) {
            return 0;
        }

        return unitPrice * unit.getBasePriceConversionRate();
    }

    /**
     * Delete all units of measure for a specific product
     * @param product the product
     * @return true if successful, false otherwise
     */
    public boolean deleteByProduct(Product product) {
        if (product == null) {
            return false;
        }

        return daoUnitOfMeasure.deleteByProduct(product);
    }

    /**
     * Check if a product has any units of measure defined
     * @param product the product
     * @return true if product has units defined, false otherwise
     */
    public boolean hasUnitsOfMeasure(Product product) {
        if (product == null) {
            return false;
        }

        List<UnitOfMeasure> units = daoUnitOfMeasure.getByProduct(product);
        return !units.isEmpty();
    }

    /**
     * Validate if a unit of measure exists for a product
     * @param productId the product ID
     * @param unitId the unit ID
     * @return true if the unit exists for the product, false otherwise
     */
    public boolean isValidUnitForProduct(String productId, String unitId) {
        if (productId == null || unitId == null || productId.isEmpty() || unitId.isEmpty()) {
            return false;
        }

        UnitOfMeasure unit = daoUnitOfMeasure.findById(unitId);
        return unit != null && unit.getProduct().getId().equals(productId);
    }

    /**
     * Create a base unit of measure for a product
     * @param id the unit ID
     * @param product the product
     * @param name the unit name (e.g., "Viên", "Hộp", "Chai")
     * @return the created base unit or null if failed
     */
    public UnitOfMeasure createBaseUnit(String id, Product product, String name) {
        if (id == null || id.isEmpty() || product == null || name == null || name.isEmpty()) {
            return null;
        }

        UnitOfMeasure baseUnit = new UnitOfMeasure(id, product, name, 1.0);

        if (addUnitOfMeasure(baseUnit)) {
            return baseUnit;
        }

        return null;
    }

    /**
     * Create a base unit of measure for a product (auto-generate ID)
     * @param product the product
     * @param name the unit name (e.g., "Viên", "Hộp", "Chai")
     * @return the created base unit or null if failed
     */
    public UnitOfMeasure createBaseUnit(Product product, String name) {
        if (product == null || name == null || name.isEmpty()) {
            return null;
        }

        UnitOfMeasure baseUnit = new UnitOfMeasure(product, name, 1.0);

        if (addUnitOfMeasure(baseUnit)) {
            return baseUnit;
        }

        return null;
    }

    /**
     * Create a derived unit of measure for a product
     * @param id the unit ID
     * @param product the product
     * @param name the unit name
     * @param conversionRate the conversion rate to base unit (e.g., 1 hộp = 10 viên -> rate = 10)
     * @return the created unit or null if failed
     */
    public UnitOfMeasure createDerivedUnit(String id, Product product, String name, double conversionRate) {
        if (id == null || id.isEmpty() || product == null || name == null || name.isEmpty() || conversionRate <= 0) {
            return null;
        }

        UnitOfMeasure derivedUnit = new UnitOfMeasure(id, product, name, conversionRate);

        if (addUnitOfMeasure(derivedUnit)) {
            return derivedUnit;
        }

        return null;
    }

    /**
     * Create a derived unit of measure for a product (auto-generate ID)
     * @param product the product
     * @param name the unit name
     * @param conversionRate the conversion rate to base unit (e.g., 1 hộp = 10 viên -> rate = 10)
     * @return the created unit or null if failed
     */
    public UnitOfMeasure createDerivedUnit(Product product, String name, double conversionRate) {
        if (product == null || name == null || name.isEmpty() || conversionRate <= 0) {
            return null;
        }

        UnitOfMeasure derivedUnit = new UnitOfMeasure(product, name, conversionRate);

        if (addUnitOfMeasure(derivedUnit)) {
            return derivedUnit;
        }

        return null;
    }
}
