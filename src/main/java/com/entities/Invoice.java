package com.entities;

import com.enums.InvoiceType;
import com.enums.PaymentMethod;
import com.enums.PromotionEnum;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author Bùi Quốc Trụ
 */
@Entity
@Table(name = "Invoice")
public class Invoice {
    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false, length = 50)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private InvoiceType type;

    @Column(name = "notes", columnDefinition = "NVARCHAR(MAX)")
    private String notes;

    @CreationTimestamp
    @Column(name = "creationDate", updatable = false, nullable = false)
    private LocalDateTime creationDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator", nullable = false)
    private Staff creator;

    @Column(name = "prescriptionCode", length = 100)
    private String prescriptionCode;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<InvoiceLine> invoiceLineList = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promotion")
    private Promotion promotion;

    @Enumerated(EnumType.STRING)
    @Column(name = "paymentMethod", nullable = false, length = 50)
    private PaymentMethod paymentMethod;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "referencedInvoice")
    private Invoice referencedInvoice;

    protected Invoice() {}

    public Invoice(InvoiceType type, Staff creator) {
        // Don't set id manually - let Hibernate's @UuidGenerator handle it
        this.type = type;
        this.creationDate = LocalDateTime.now();
        this.creator = creator;
        this.invoiceLineList = new ArrayList<>();
    }

    public Invoice(String id, InvoiceType type, LocalDateTime creationDate, Staff creator, String notes, String prescriptionCode, List<InvoiceLine> invoiceLineList, Promotion promotion, PaymentMethod paymentMethod, Invoice referencedInvoice) {
        this.id = id;
        this.type = type;
        this.creationDate = creationDate;
        this.creator = creator;
        this.notes = notes;
        this.prescriptionCode = prescriptionCode;
        this.invoiceLineList = invoiceLineList != null ? invoiceLineList : new ArrayList<>();
        this.promotion = promotion;
        this.paymentMethod = paymentMethod;
        this.referencedInvoice = referencedInvoice;
    }

    public String getId() {
        return id;
    }

    public InvoiceType getType() {
        return type;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public Staff getCreator() {
        return creator;
    }

    public void setCreator(Staff creator) {
        this.creator = creator;
    }

    public String getPrescriptionCode() {
        return prescriptionCode;
    }

    public void setPrescriptionCode(String prescriptionCode) {
        this.prescriptionCode = prescriptionCode;
    }

    public List<InvoiceLine> getInvoiceLineList() {
        return invoiceLineList;
    }

    public void setInvoiceLineList(List<InvoiceLine> invoiceLineList) {
        this.invoiceLineList = invoiceLineList;
    }

    public Promotion getPromotion() {
        return promotion;
    }

    public void setPromotion(Promotion promotion) {
        this.promotion = promotion;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public Invoice getReferencedInvoice() {
        return referencedInvoice;
    }

    public void setReferencedInvoice(Invoice referencedInvoice) {
        this.referencedInvoice = referencedInvoice;
    }

    /**
     * @author Bùi Quốc Trụ
     *
     * Adds an InvoiceLine to the invoice if it does not already exist.
     *
     * @param invoiceLine The InvoiceLine to be added.
     * @return true if the InvoiceLine was added, false if it already exists or is null.
     */
    public boolean addInvoiceLine(InvoiceLine invoiceLine) {
        if (invoiceLine == null)
            return false;

        for (InvoiceLine existingLine : invoiceLineList)
            if (existingLine.equals(invoiceLine))
                return false;

        invoiceLineList.add(invoiceLine);
        return true;
    }

    /**
     * @author Bùi Quốc Trụ
     *
     * Updates an existing InvoiceLine in the invoice.
     * If the UOM changes, removes the old line and adds the new one.
     * If only quantity changes, updates the existing line.
     *
     * @param oldProductId The product ID of the line to update
     * @param oldUomId The old UOM ID
     * @param newInvoiceLine The new InvoiceLine with updated values
     * @return true if the InvoiceLine was found and updated, false otherwise.
     */
    public boolean updateInvoiceLine(String oldProductId, String oldUomId, InvoiceLine newInvoiceLine) {
        if (newInvoiceLine == null)
            return false;

        // Find and remove the old invoice line
        InvoiceLine oldLine = null;
        for (InvoiceLine line : invoiceLineList) {
            if (line.getProduct().getId().equals(oldProductId) &&
                line.getUnitOfMeasure().getId().equals(oldUomId)) {
                oldLine = line;
                break;
            }
        }

        if (oldLine != null) {
            invoiceLineList.remove(oldLine);

            // Check if the new line already exists (after UOM change)
            boolean exists = false;
            for (InvoiceLine existingLine : invoiceLineList) {
                if (existingLine.getProduct().getId().equals(newInvoiceLine.getProduct().getId()) &&
                    existingLine.getUnitOfMeasure().getId().equals(newInvoiceLine.getUnitOfMeasure().getId())) {
                    // Merge quantities
                    existingLine.setQuantity(existingLine.getQuantity() + newInvoiceLine.getQuantity());
                    exists = true;
                    break;
                }
            }

            if (!exists) {
                invoiceLineList.add(newInvoiceLine);
            }
            return true;
        }

        return false;
    }

    /**
     * @author Bùi Quốc Trụ
     *
     * Removes an InvoiceLine from the invoice.
     *
     * @param productId The ID of the product in the InvoiceLine to be removed.
     * @param unitOfMeasureId The ID of the unit of measure in the InvoiceLine to be removed.
     * @return true if the InvoiceLine was found and removed, false otherwise
     */
    public boolean removeInvoiceLine(String productId, String unitOfMeasureId) {
        return invoiceLineList.removeIf(line ->
                line.getProduct().getId().equals(productId) &&
                line.getUnitOfMeasure().getId().equals(unitOfMeasureId)
        );
    }

    /**
     * @author Bùi Quốc Trụ
     *
     * Calculate the subtotal amount of the invoice.
     *
     * @return The subtotal amount.
     */
    public double calculateSubtotal() {
        double subtotal = 0.0;

        for (InvoiceLine line : invoiceLineList) {
            subtotal += line.calculateSubtotal();
        }

        return subtotal;
    }

    /**
     * @author Bùi Quốc Trụ
     *
     * Calculate the total VAT amount of the invoice.
     *
     * @return The total VAT amount.
     */
    public double calculateVatAmount() {
        double vatAmount = 0.0;

        for (InvoiceLine line : invoiceLineList) {
            vatAmount += line.calculateVatAmount();
        }

        return vatAmount;
    }

    /**
     * @author Bùi Quốc Trụ
     *
     * Calculate the subtotal including VAT.
     *
     * @return The subtotal with VAT.
     */
    public double calculateSubtotalWithVat() {
        return calculateSubtotal() + calculateVatAmount();
    }

    /**
     * Check if the invoice satisfies all promotion conditions
     *
     * @return true if all conditions are met, false otherwise
     */
    public boolean checkPromotionConditions() {
        if (promotion == null)
            return false;

        Set<PromotionCondition> conditions = promotion.getConditions();
        if (conditions == null || conditions.isEmpty())
            return true; // No conditions means promotion is always applicable

        // All conditions must be satisfied
        for (PromotionCondition condition : conditions) {
            if (!checkSingleCondition(condition)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Check if a single promotion condition is satisfied
     */
    private boolean checkSingleCondition(PromotionCondition condition) {
        if (condition.getTarget() == PromotionEnum.Target.PRODUCT) {
            return checkProductCondition(condition);
        } else if (condition.getTarget() == PromotionEnum.Target.ORDER_SUBTOTAL) {
            return checkOrderCondition(condition);
        }
        return false;
    }

    /**
     * Check product-targeted condition (e.g., buy X quantity of product Y)
     */
    private boolean checkProductCondition(PromotionCondition condition) {
        Product targetProduct = condition.getProduct();
        if (targetProduct == null)
            return false;

        // Calculate total quantity of target product in invoice
        int totalQuantity = 0;
        for (InvoiceLine line : invoiceLineList) {
            if (line.getProduct().getId().equals(targetProduct.getId())) {
                totalQuantity += line.getQuantity();
            }
        }

        // Compare based on condition type and comparator
        if (condition.getConditionType() == PromotionEnum.ConditionType.PRODUCT_QTY) {
            return compareValues(totalQuantity, condition.getPrimaryValue(), condition.getComparator());
        }

        return false;
    }

    /**
     * Check order subtotal condition (e.g., order total >= X)
     */
    private boolean checkOrderCondition(PromotionCondition condition) {
        if (condition.getConditionType() == PromotionEnum.ConditionType.ORDER_SUBTOTAL) {
            double subtotalWithVat = calculateSubtotalWithVat();
            return compareValues(subtotalWithVat, condition.getPrimaryValue(), condition.getComparator());
        }

        return false;
    }

    /**
     * Compare two values based on comparator
     */
    private boolean compareValues(double actualValue, double requiredValue, PromotionEnum.Comp comparator) {
        switch (comparator) {
            case EQUAL:
                return actualValue == requiredValue;
            case GREATER:
                return actualValue > requiredValue;
            case GREATER_EQUAL:
                return actualValue >= requiredValue;
            case LESS:
                return actualValue < requiredValue;
            case LESS_EQUAL:
                return actualValue <= requiredValue;
            case BETWEEN:
                // BETWEEN requires secondaryValue - not implemented yet
                return false;
            default:
                return false;
        }
    }

    /**
     * @author Bùi Quốc Trụ
     *
     * Calculate the total discount applied to the invoice.
     * Only applies discount if conditions are satisfied.
     *
     * @return The total discount.
     */
    public double calculatePromotion() {
        if (promotion == null)
            return 0.0;

        // Check if conditions are satisfied before calculating discount
        if (!checkPromotionConditions())
            return 0.0;

        double totalDiscount = 0.0;
        List<PromotionAction> sortedActionOrderList = promotion.getActions().stream()
                .sorted((a, b) -> Integer.compare(a.getActionOrder(), b.getActionOrder()))
                .toList();

        for (PromotionAction action : sortedActionOrderList) {
            if (action.getTarget() == PromotionEnum.Target.PRODUCT) {
                // Apply discount to specific product(s)
                totalDiscount += calculateProductDiscount(action);
            } else if (action.getTarget() == PromotionEnum.Target.ORDER_SUBTOTAL) {
                // Apply discount to order subtotal with VAT
                totalDiscount += calculateOrderDiscount(action);
            }
        }

        return totalDiscount;
    }

    /**
     * Calculate discount for product-targeted promotion action
     */
    private double calculateProductDiscount(PromotionAction action) {
        double discount = 0.0;
        Product targetProduct = action.getProduct();

        if (targetProduct == null)
            return 0.0;

        // Find matching invoice lines for the target product
        for (InvoiceLine line : invoiceLineList) {
            if (line.getProduct().getId().equals(targetProduct.getId())) {
                double lineSubtotalWithVat = line.calculateTotalAmount();

                if (action.getType() == PromotionEnum.ActionType.FIXED_DISCOUNT) {
                    discount += action.getPrimaryValue();
                } else if (action.getType() == PromotionEnum.ActionType.PERCENT_DISCOUNT) {
                    discount += lineSubtotalWithVat * (action.getPrimaryValue() / 100);
                }
                // Note: PRODUCT_GIFT is handled separately, not as a discount
            }
        }

        return discount;
    }

    /**
     * Calculate discount for order subtotal-targeted promotion action
     */
    private double calculateOrderDiscount(PromotionAction action) {
        double discount = 0.0;
        double subtotalWithVat = calculateSubtotalWithVat();

        if (action.getType() == PromotionEnum.ActionType.FIXED_DISCOUNT) {
            discount = action.getPrimaryValue();
        } else if (action.getType() == PromotionEnum.ActionType.PERCENT_DISCOUNT) {
            discount = subtotalWithVat * (action.getPrimaryValue() / 100);
        }

        return discount;
    }

    /**
     * @author Bùi Quốc Trụ
     *
     * Calculate the total amount of the invoice after applying promotions.
     *
     * @return The total amount.
     */
    public double calculateTotal() {
        return Math.ceil(calculateSubtotalWithVat() - calculatePromotion());
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
