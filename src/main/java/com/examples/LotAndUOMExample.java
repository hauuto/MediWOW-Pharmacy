package com.examples;

import com.bus.BUS_Lot;
import com.bus.BUS_Product;
import com.bus.BUS_UnitOfMeasure;
import com.entities.Lot;
import com.entities.Product;
import com.entities.UnitOfMeasure;
import com.enums.LotStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Example class demonstrating how to use Lot and UnitOfMeasure
 * @author Bùi Quốc Trụ, Nguyễn Thanh Khôi
 */
public class LotAndUOMExample {

    public static void main(String[] args) {
        // Initialize business logic layers
        BUS_Product busProduct = new BUS_Product();
        BUS_Lot busLot = new BUS_Lot();
        BUS_UnitOfMeasure busUOM = new BUS_UnitOfMeasure();

        // Example 1: Get a product and work with its Units of Measure
        System.out.println("=== EXAMPLE 1: Unit of Measure Management ===");
        Product product = busProduct.getProductById("some-product-id");

        if (product != null) {
            // Create base unit (e.g., "Viên" - pill) - ID auto-generated
            UnitOfMeasure baseUnit = busUOM.createBaseUnit(product, "Viên");

            if (baseUnit != null) {
                System.out.println("Created base unit: " + baseUnit.getName());
                System.out.println("Auto-generated ID: " + baseUnit.getId());
            }

            // Create derived unit (e.g., "Hộp" - box, 1 box = 10 pills) - ID auto-generated
            UnitOfMeasure boxUnit = busUOM.createDerivedUnit(product, "Hộp", 10.0);

            if (boxUnit != null) {
                System.out.println("Created derived unit: " + boxUnit.getName());
                System.out.println("Auto-generated ID: " + boxUnit.getId());
                System.out.println("Conversion rate: 1 Hộp = " + boxUnit.getBaseUnitConversionRate() + " Viên");
            }

            // Convert quantities between units
            if (baseUnit != null && boxUnit != null) {
                double quantity = 5.0; // 5 boxes
                double convertedQty = busUOM.convertQuantity(boxUnit, baseUnit, quantity);
                System.out.println(quantity + " Hộp = " + convertedQty + " Viên");

                // Convert price from base unit to box unit
                double basePrice = 1000.0; // 1000 VND per pill
                double boxPrice = busUOM.getPriceForUnit(boxUnit, basePrice);
                System.out.println("Base price: " + basePrice + " VND/Viên");
                System.out.println("Box price: " + boxPrice + " VND/Hộp");
            }

            // Get all units for the product
            List<UnitOfMeasure> allUnits = busUOM.getUnitsByProduct(product);
            System.out.println("\nAll units for product: " + allUnits.size());
            for (UnitOfMeasure uom : allUnits) {
                System.out.println("- " + uom.getName() + " (rate: " + uom.getBaseUnitConversionRate() + ")");
            }
        }

        // Example 2: Lot Management
        System.out.println("\n=== EXAMPLE 2: Lot Management ===");

        if (product != null) {
            // Create a new lot
            String batchNumber = "BATCH-" + System.currentTimeMillis();
            LocalDateTime expiryDate = LocalDateTime.now().plusMonths(12); // Expires in 1 year

            Lot newLot = new Lot(
                batchNumber,
                product,
                1000, // quantity
                15000.0, // MediWOW price
                expiryDate,
                LotStatus.AVAILABLE
            );

            boolean added = busLot.addLot(newLot);
            if (added) {
                System.out.println("Successfully added new lot: " + batchNumber);
                System.out.println("Quantity: " + newLot.getQuantity());
                System.out.println("Expiry Date: " + newLot.getExpiryDate());
                System.out.println("Status: " + newLot.getStatus());
            }

            // Get all lots for the product
            List<Lot> productLots = busLot.getLotsByProduct(product);
            System.out.println("\nTotal lots for product: " + productLots.size());

            // Get available lots
            List<Lot> availableLots = busLot.getAvailableLotsByProduct(product);
            System.out.println("Available lots: " + availableLots.size());

            // Get oldest available lot (FIFO)
            Lot oldestLot = busLot.getOldestAvailableLot(product);
            if (oldestLot != null) {
                System.out.println("\nOldest available lot:");
                System.out.println("- Batch: " + oldestLot.getBatchNumber());
                System.out.println("- Quantity: " + oldestLot.getQuantity());
                System.out.println("- Expiry: " + oldestLot.getExpiryDate());
            }

            // Check total available quantity
            int totalQty = busLot.getTotalAvailableQuantity(product);
            System.out.println("\nTotal available quantity: " + totalQty);

            // Reduce quantity (simulate selling)
            if (oldestLot != null) {
                boolean reduced = busLot.reduceLotQuantity(oldestLot.getBatchNumber(), 50);
                if (reduced) {
                    System.out.println("\nReduced 50 units from oldest lot");
                    Lot updatedLot = busLot.getLotByBatchNumber(oldestLot.getBatchNumber());
                    System.out.println("New quantity: " + updatedLot.getQuantity());
                }
            }
        }

        // Example 3: Check expiring lots
        System.out.println("\n=== EXAMPLE 3: Expiring Lots ===");

        // Get lots expiring in 30 days
        List<Lot> expiringSoon = busLot.getLotsExpiringSoon(30);
        System.out.println("Lots expiring in 30 days: " + expiringSoon.size());

        for (Lot lot : expiringSoon) {
            System.out.println("- Batch: " + lot.getBatchNumber());
            System.out.println("  Product: " + lot.getProduct().getName());
            System.out.println("  Expiry: " + lot.getExpiryDate());
            System.out.println("  Quantity: " + lot.getQuantity());
        }

        // Mark expired lots
        int expiredCount = busLot.markExpiredLots();
        System.out.println("\nMarked " + expiredCount + " lots as EXPIRED");

        // Example 4: Practical use case - Processing an order
        System.out.println("\n=== EXAMPLE 4: Processing Order with UOM ===");

        if (product != null) {
            // Customer wants to buy 3 boxes
            UnitOfMeasure boxUnit = busUOM.findByProductAndName(product, "Hộp");
            UnitOfMeasure baseUnit = busUOM.getBaseUnit(product);

            if (boxUnit != null && baseUnit != null) {
                double orderedBoxes = 3.0;
                double orderedPills = busUOM.convertQuantity(boxUnit, baseUnit, orderedBoxes);

                System.out.println("Customer order: " + orderedBoxes + " Hộp");
                System.out.println("Equivalent to: " + orderedPills + " Viên");

                // Check if we have enough stock
                int availableQty = busLot.getTotalAvailableQuantity(product);
                if (availableQty >= orderedPills) {
                    System.out.println("✓ Stock available!");

                    // Get oldest lot for FIFO
                    Lot lotToUse = busLot.getOldestAvailableLot(product);
                    if (lotToUse != null) {
                        System.out.println("Will use lot: " + lotToUse.getBatchNumber());

                        // Calculate price
                        double basePricePerPill = lotToUse.getRawPrice();
                        double pricePerBox = busUOM.getPriceForUnit(boxUnit, basePricePerPill);
                        double totalPrice = pricePerBox * orderedBoxes;

                        System.out.println("Price per box: " + pricePerBox + " VND");
                        System.out.println("Total price: " + totalPrice + " VND");
                    }
                } else {
                    System.out.println("✗ Insufficient stock!");
                    System.out.println("Available: " + availableQty + ", Needed: " + orderedPills);
                }
            }
        }

        System.out.println("\n=== END OF EXAMPLES ===");
    }
}
