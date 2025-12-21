package com.entities;

import com.enums.InvoiceType;
import com.enums.PaymentMethod;
import com.enums.PromotionEnum;
import com.enums.PromotionEnum.ConditionType;
import com.enums.PromotionEnum.Target;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * @author Bùi Quốc Trụ
 */
@Entity
@Table(name = "Invoice")
public class Invoice {
    @Id
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer")
    private Customer customer;

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

    // --- UPDATE: QUẢN LÝ CA (SHIFT MANAGEMENT) ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shift", nullable = true) // Có thể null nếu là data cũ, nhưng logic mới nên bắt buộc
    private Shift shift;
    // ---------------------------------------------

    protected Invoice() {}

    /**
     * Updated Constructor to include Shift
     * @author Bùi Quốc Trụ
     */
    public Invoice(InvoiceType type, Staff creator, Shift shift) {
        // Don't set id manually - let Hibernate's @UuidGenerator handle it
        this.type = type;
        this.creationDate = LocalDateTime.now();
        this.creator = creator;
        this.shift = shift; // Gán ca làm việc hiện tại
        this.invoiceLineList = new ArrayList<>();
    }

    // Full constructor updated
    public Invoice(String id, InvoiceType type, LocalDateTime creationDate, Staff creator,
                   Customer customer, String notes, String prescriptionCode,
                   List<InvoiceLine> invoiceLineList, Promotion promotion,
                   PaymentMethod paymentMethod, Invoice referencedInvoice, Shift shift) {
        this.id = id;
        this.type = type;
        this.creationDate = creationDate;
        this.creator = creator;
        this.customer = customer;
        this.notes = notes;
        this.prescriptionCode = prescriptionCode;
        this.invoiceLineList = invoiceLineList != null ? invoiceLineList : new ArrayList<>();
        this.promotion = promotion;
        this.paymentMethod = paymentMethod;
        this.referencedInvoice = referencedInvoice;
        this.shift = shift;
    }

    // --- Getters and Setters for Shift ---
    public Shift getShift() {
        return shift;
    }

    public void setShift(Shift shift) {
        this.shift = shift;
    }
    // -------------------------------------

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

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
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

    // ... (Giữ nguyên toàn bộ các phương thức xử lý nghiệp vụ bên dưới của bạn: addInvoiceLine, calculateSubtotal, v.v...)

    /**
     * Adds an InvoiceLine to the invoice if it does not already exist.
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
     * Updates an existing InvoiceLine in the invoice.
     */
    public boolean updateInvoiceLine(String oldProductId, String oldUomName, InvoiceLine newInvoiceLine) {
        if (newInvoiceLine == null)
            return false;

        InvoiceLine oldLine = null;
        for (InvoiceLine line : invoiceLineList) {
            if (line.getProduct().getId().equals(oldProductId) &&
                    line.getUnitOfMeasure().getName().equals(oldUomName)) {
                oldLine = line;
                break;
            }
        }

        if (oldLine != null) {
            invoiceLineList.remove(oldLine);
            boolean exists = false;
            for (InvoiceLine existingLine : invoiceLineList) {
                if (existingLine.getProduct().getId().equals(newInvoiceLine.getProduct().getId()) &&
                        existingLine.getUnitOfMeasure().getName().equals(newInvoiceLine.getUnitOfMeasure().getName())) {
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
     * Removes an InvoiceLine from the invoice.
     */
    public boolean removeInvoiceLine(String productId, String unitOfMeasureName) {
        return invoiceLineList.removeIf(line ->
                line.getProduct().getId().equals(productId) &&
                        line.getUnitOfMeasure().getName().equals(unitOfMeasureName)
        );
    }

    // =====================================================
    // Money calculations (BigDecimal is the source of truth)
    // =====================================================

    /** Preferred BigDecimal subtotal. */
    public BigDecimal calculateSubtotal() {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (InvoiceLine line : invoiceLineList) {
            if (line != null) subtotal = subtotal.add(line.calculateSubtotal());
        }
        return subtotal;
    }



    /** Preferred BigDecimal VAT. */
    public BigDecimal calculateVatAmount() {
        BigDecimal vatAmount = BigDecimal.ZERO;
        for (InvoiceLine line : invoiceLineList) {
            if (line != null) vatAmount = vatAmount.add(line.calculateVatAmount());
        }
        return vatAmount;
    }



    /** Preferred BigDecimal subtotal including VAT. */
    public BigDecimal calculateSubtotalWithVat() {
        return calculateSubtotal().add(calculateVatAmount());
    }


    // =====================================================
    // Promotion checks & discounts
    // =====================================================

    /** Check if the invoice satisfies all promotion conditions */
    public boolean checkPromotionConditions() {
        if (promotion == null) return false;

        List<PromotionCondition> conditions = promotion.getConditions();
        if (conditions == null || conditions.isEmpty()) return true;

        for (PromotionCondition condition : conditions) {
            if (!checkSingleCondition(condition)) return false;
        }
        return true;
    }

    /** Check if a single promotion condition is satisfied */
    private boolean checkSingleCondition(PromotionCondition condition) {
        if (condition == null) return false;
        if (condition.getTarget() == Target.PRODUCT) {
            return checkProductCondition(condition);
        } else if (condition.getTarget() == Target.ORDER_SUBTOTAL) {
            return checkOrderCondition(condition);
        }
        return false;
    }

    /** Check product-targeted condition (e.g., buy X quantity of product Y) */
    private boolean checkProductCondition(PromotionCondition condition) {
        Product targetProduct = condition.getProduct();
        if (targetProduct == null) return false;

        int totalQuantity = 0;
        for (InvoiceLine line : invoiceLineList) {
            if (line != null && line.getProduct() != null && line.getProduct().getId().equals(targetProduct.getId())) {
                totalQuantity += line.getQuantity();
            }
        }

        if (condition.getConditionType() == ConditionType.PRODUCT_QTY) {
            // For quantity, compare using BigDecimal value.
            return compareValues(totalQuantity, condition.getPrimaryValue(), condition.getComparator());
        }
        return false;
    }

    /** Check order subtotal condition (e.g., order total >= X) */
    private boolean checkOrderCondition(PromotionCondition condition) {
        if (condition.getConditionType() == ConditionType.ORDER_SUBTOTAL) {
            BigDecimal subtotalWithVat = calculateSubtotalWithVat();
            return compareValues(subtotalWithVat, condition.getPrimaryValue(), condition.getComparator());
        }
        return false;
    }

    // =====================================================
    // Promotion discount calculation helpers
    // =====================================================

    /**
     * Calculate discount for PRODUCT-targeted action.
     * Supports percentage/fixed discounts and buy-x-get-y (if configured).
     */
    private BigDecimal calculateProductDiscount(PromotionAction action) {
        if (action == null) return BigDecimal.ZERO;
        Product targetProduct = action.getProduct();
        if (targetProduct == null) return BigDecimal.ZERO;

        BigDecimal discount = BigDecimal.ZERO;
        for (InvoiceLine line : invoiceLineList) {
            if (line == null || line.getProduct() == null) continue;
            if (!line.getProduct().getId().equals(targetProduct.getId())) continue;

            BigDecimal lineSubtotalWithVat = line.calculateTotalAmount();

            // Discount value semantics depend on action type
            switch (action.getType()) {
                case PERCENT_DISCOUNT -> {
                    BigDecimal rate = action.getValue().divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
                    discount = discount.add(lineSubtotalWithVat.multiply(rate));
                }
                case FIXED_DISCOUNT -> {
                    // Fixed discount amount for this product line
                    discount = discount.add(action.getValue());
                }
                default -> {
                    // Other types can be implemented later
                }
            }
        }

        return discount;
    }

    /**
     * Calculate discount for ORDER_SUBTOTAL-targeted action.
     */
    private BigDecimal calculateOrderDiscount(PromotionAction action) {
        if (action == null) return BigDecimal.ZERO;

        BigDecimal subtotalWithVat = calculateSubtotalWithVat();
        return switch (action.getType()) {
            case PERCENT_DISCOUNT -> {
                BigDecimal rate = action.getValue().divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
                yield subtotalWithVat.multiply(rate);
            }
            case FIXED_DISCOUNT -> action.getValue() != null ? action.getValue() : BigDecimal.ZERO;
            default -> BigDecimal.ZERO;
        };
    }

    /** Compare two values based on comparator */
    private boolean compareValues(BigDecimal actualValue, BigDecimal requiredValue, PromotionEnum.Comp comparator) {
        if (actualValue == null) actualValue = BigDecimal.ZERO;
        if (requiredValue == null) requiredValue = BigDecimal.ZERO;
        int cmp = actualValue.compareTo(requiredValue);

        return switch (comparator) {
            case EQUAL -> cmp == 0;
            case GREATER -> cmp > 0;
            case GREATER_EQUAL -> cmp >= 0;
            case LESS -> cmp < 0;
            case LESS_EQUAL -> cmp <= 0;
            case BETWEEN -> false; // BETWEEN requires secondaryValue - not implemented yet
        };
    }

    /** Compare int (e.g., qty) to a BigDecimal required value. */
    private boolean compareValues(int actualValue, BigDecimal requiredValue, PromotionEnum.Comp comparator) {
        if (comparator == null) return false;
        return compareValues(BigDecimal.valueOf(actualValue), requiredValue, comparator);
    }

    /** Preferred BigDecimal promotion discount. */
    public BigDecimal calculatePromotion() {
        if (promotion == null) return BigDecimal.ZERO;
        if (!checkPromotionConditions()) return BigDecimal.ZERO;

        BigDecimal totalDiscount = BigDecimal.ZERO;
        List<PromotionAction> sortedActionOrderList = promotion.getActions().stream()
                .sorted(Comparator.comparingInt(PromotionAction::getActionOrder))
                .toList();

        for (PromotionAction action : sortedActionOrderList) {
            if (action == null) continue;
            if (action.getTarget() == Target.PRODUCT) {
                totalDiscount = totalDiscount.add(calculateProductDiscount(action));
            } else if (action.getTarget() == Target.ORDER_SUBTOTAL) {
                totalDiscount = totalDiscount.add(calculateOrderDiscount(action));
            }
        }

        return totalDiscount;
    }


    /** Preferred BigDecimal total. */
    public BigDecimal calculateTotal() {
        BigDecimal total = calculateSubtotalWithVat().subtract(calculatePromotion());
        // Money scale = 2 for tax/discount; round half-up.
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
