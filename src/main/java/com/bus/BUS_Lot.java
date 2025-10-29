package com.bus;

import com.dao.DAO_Lot;
import com.entities.Lot;
import com.entities.Product;
import com.enums.LotStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Business Logic Layer for Lot entity
 * @author Tô Thanh Hậu
 */
public class BUS_Lot {
    private final DAO_Lot daoLot;

    public BUS_Lot() {
        this.daoLot = new DAO_Lot();
    }

    /**
     * Add a new lot
     * @param lot the lot to add
     * @return true if successful, false otherwise
     */
    public boolean addLot(Lot lot) {
        if (lot == null || lot.getBatchNumber() == null || lot.getBatchNumber().isEmpty()) {
            return false;
        }

        // Check if batch number already exists
        if (daoLot.findByBatchNumber(lot.getBatchNumber()) != null) {
            return false;
        }

        return daoLot.add(lot);
    }

    /**
     * Update an existing lot
     * @param lot the lot to update
     * @return true if successful, false otherwise
     */
    public boolean updateLot(Lot lot) {
        if (lot == null || lot.getBatchNumber() == null || lot.getBatchNumber().isEmpty()) {
            return false;
        }

        return daoLot.update(lot);
    }

    /**
     * Delete a lot by batch number
     * @param batchNumber the batch number of the lot to delete
     * @return true if successful, false otherwise
     */
    public boolean deleteLot(String batchNumber) {
        if (batchNumber == null || batchNumber.isEmpty()) {
            return false;
        }

        return daoLot.delete(batchNumber);
    }

    /**
     * Find a lot by batch number
     * @param batchNumber the batch number to search for
     * @return the lot if found, null otherwise
     */
    public Lot getLotByBatchNumber(String batchNumber) {
        if (batchNumber == null || batchNumber.isEmpty()) {
            return null;
        }

        return daoLot.findByBatchNumber(batchNumber);
    }

    /**
     * Get all lots
     * @return list of all lots
     */
    public List<Lot> getAllLots() {
        return daoLot.getAll();
    }

    /**
     * Get all lots for a specific product
     * @param product the product to get lots for
     * @return list of lots for the product
     */
    public List<Lot> getLotsByProduct(Product product) {
        if (product == null) {
            return List.of();
        }

        return daoLot.getLotsByProduct(product);
    }

    /**
     * Get all lots by status
     * @param status the status to filter by
     * @return list of lots with the specified status
     */
    public List<Lot> getLotsByStatus(LotStatus status) {
        if (status == null) {
            return List.of();
        }

        return daoLot.getLotsByStatus(status);
    }

    /**
     * Get all available lots for a product
     * @param product the product to get available lots for
     * @return list of available lots
     */
    public List<Lot> getAvailableLotsByProduct(Product product) {
        if (product == null) {
            return List.of();
        }

        return daoLot.getAvailableLotsByProduct(product);
    }

    /**
     * Get lots expiring before a certain date
     * @param expiryDate the date to check against
     * @return list of lots expiring before the date
     */
    public List<Lot> getLotsExpiringBefore(LocalDateTime expiryDate) {
        if (expiryDate == null) {
            return List.of();
        }

        return daoLot.getLotsExpiringBefore(expiryDate);
    }

    /**
     * Get the oldest available lot for a product (FIFO - First In First Out)
     * @param product the product to get the oldest lot for
     * @return the oldest available lot or null if none available
     */
    public Lot getOldestAvailableLot(Product product) {
        if (product == null) {
            return null;
        }

        return daoLot.getOldestAvailableLot(product);
    }

    /**
     * Update lot quantity
     * @param batchNumber the batch number of the lot
     * @param newQuantity the new quantity
     * @return true if successful, false otherwise
     */
    public boolean updateLotQuantity(String batchNumber, int newQuantity) {
        if (batchNumber == null || batchNumber.isEmpty() || newQuantity < 0) {
            return false;
        }

        return daoLot.updateQuantity(batchNumber, newQuantity);
    }

    /**
     * Reduce lot quantity (used when selling products)
     * @param batchNumber the batch number of the lot
     * @param quantityToReduce the quantity to reduce
     * @return true if successful, false otherwise
     */
    public boolean reduceLotQuantity(String batchNumber, int quantityToReduce) {
        if (batchNumber == null || batchNumber.isEmpty() || quantityToReduce <= 0) {
            return false;
        }

        Lot lot = daoLot.findByBatchNumber(batchNumber);
        if (lot == null || lot.getQuantity() < quantityToReduce) {
            return false;
        }

        int newQuantity = lot.getQuantity() - quantityToReduce;
        return daoLot.updateQuantity(batchNumber, newQuantity);
    }

    /**
     * Increase lot quantity (used when receiving new stock)
     * @param batchNumber the batch number of the lot
     * @param quantityToAdd the quantity to add
     * @return true if successful, false otherwise
     */
    public boolean increaseLotQuantity(String batchNumber, int quantityToAdd) {
        if (batchNumber == null || batchNumber.isEmpty() || quantityToAdd <= 0) {
            return false;
        }

        Lot lot = daoLot.findByBatchNumber(batchNumber);
        if (lot == null) {
            return false;
        }

        int newQuantity = lot.getQuantity() + quantityToAdd;
        return daoLot.updateQuantity(batchNumber, newQuantity);
    }

    /**
     * Update lot status
     * @param batchNumber the batch number of the lot
     * @param newStatus the new status
     * @return true if successful, false otherwise
     */
    public boolean updateLotStatus(String batchNumber, LotStatus newStatus) {
        if (batchNumber == null || batchNumber.isEmpty() || newStatus == null) {
            return false;
        }

        return daoLot.updateStatus(batchNumber, newStatus);
    }

    /**
     * Mark expired lots as EXPIRED
     * @return number of lots marked as expired
     */
    public int markExpiredLots() {
        LocalDateTime now = LocalDateTime.now();
        List<Lot> availableLots = daoLot.getLotsByStatus(LotStatus.AVAILABLE);

        int count = 0;
        for (Lot lot : availableLots) {
            if (lot.getExpiryDate().isBefore(now)) {
                if (daoLot.updateStatus(lot.getBatchNumber(), LotStatus.EXPIRED)) {
                    count++;
                }
            }
        }

        return count;
    }

    /**
     * Get lots expiring within a certain number of days
     * @param daysFromNow number of days from now
     * @return list of lots expiring within the specified days
     */
    public List<Lot> getLotsExpiringSoon(int daysFromNow) {
        if (daysFromNow < 0) {
            return List.of();
        }

        LocalDateTime targetDate = LocalDateTime.now().plusDays(daysFromNow);
        return daoLot.getLotsExpiringBefore(targetDate);
    }

    /**
     * Check if a product has available stock
     * @param product the product to check
     * @return true if product has available stock, false otherwise
     */
    public boolean hasAvailableStock(Product product) {
        if (product == null) {
            return false;
        }

        List<Lot> availableLots = daoLot.getAvailableLotsByProduct(product);
        return !availableLots.isEmpty();
    }

    /**
     * Get total available quantity for a product across all lots
     * @param product the product
     * @return total available quantity
     */
    public int getTotalAvailableQuantity(Product product) {
        if (product == null) {
            return 0;
        }

        List<Lot> availableLots = daoLot.getAvailableLotsByProduct(product);
        return availableLots.stream()
                .mapToInt(Lot::getQuantity)
                .sum();
    }
}

