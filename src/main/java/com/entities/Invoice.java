package com.entities;

import com.enums.InvoiceType;
import com.enums.PaymentMethod;
import com.enums.PromotionEnum;
import com.enums.PromotionEnum.Target; // Import thêm để code gọn hơn
import com.enums.PromotionEnum.ConditionType;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

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
    @JoinColumn(name = "prescribedCustomer")
    private PrescribedCustomer prescribedCustomer;

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
                   PrescribedCustomer prescribedCustomer, String notes, String prescriptionCode,
                   List<InvoiceLine> invoiceLineList, Promotion promotion,
                   PaymentMethod paymentMethod, Invoice referencedInvoice, Shift shift) {
        this.id = id;
        this.type = type;
        this.creationDate = creationDate;
        this.creator = creator;
        this.prescribedCustomer = prescribedCustomer;
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

    public PrescribedCustomer getPrescribedCustomer() {
        return prescribedCustomer;
    }

    public void setPrescribedCustomer(PrescribedCustomer prescribedCustomer) {
        this.prescribedCustomer = prescribedCustomer;
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
                    line.getUnitOfMeasure().equals(oldUomName)) {
                oldLine = line;
                break;
            }
        }

        if (oldLine != null) {
            invoiceLineList.remove(oldLine);
            boolean exists = false;
            for (InvoiceLine existingLine : invoiceLineList) {
                if (existingLine.getProduct().getId().equals(newInvoiceLine.getProduct().getId()) &&
                        existingLine.getUnitOfMeasure().equals(newInvoiceLine.getUnitOfMeasure())) {
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
                        line.getUnitOfMeasure().equals(unitOfMeasureName)
        );
    }

    /** Calculate the subtotal amount of the invoice. */
    public double calculateSubtotal() {
        double subtotal = 0.0;
        for (InvoiceLine line : invoiceLineList) {
            subtotal += line.calculateSubtotal();
        }
        return subtotal;
    }

    /** Calculate the total VAT amount of the invoice. */
    public double calculateVatAmount() {
        double vatAmount = 0.0;
        for (InvoiceLine line : invoiceLineList) {
            vatAmount += line.calculateVatAmount();
        }
        return vatAmount;
    }

    /** Calculate the subtotal including VAT. */
    public double calculateSubtotalWithVat() {
        return calculateSubtotal() + calculateVatAmount();
    }

    /** Check if the invoice satisfies all promotion conditions */
    public boolean checkPromotionConditions() {
        if (promotion == null)
            return false;

        List<PromotionCondition> conditions = promotion.getConditions();
        if (conditions == null || conditions.isEmpty())
            return true;
        for (PromotionCondition condition : conditions) {
            if (!checkSingleCondition(condition)) {
                return false;
            }
        }
        return true;
    }

    /** Check if a single promotion condition is satisfied */
    private boolean checkSingleCondition(PromotionCondition condition) {
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
        if (targetProduct == null)
            return false;
        int totalQuantity = 0;
        for (InvoiceLine line : invoiceLineList) {
            if (line.getProduct().getId().equals(targetProduct.getId())) {
                totalQuantity += line.getQuantity();
            }
        }
        if (condition.getConditionType() == ConditionType.PRODUCT_QTY) {
            return compareValues(totalQuantity, condition.getPrimaryValue(), condition.getComparator());
        }
        return false;
    }

    /** Check order subtotal condition (e.g., order total >= X) */
    private boolean checkOrderCondition(PromotionCondition condition) {
        if (condition.getConditionType() == ConditionType.ORDER_SUBTOTAL) {
            double subtotalWithVat = calculateSubtotalWithVat();
            return compareValues(subtotalWithVat, condition.getPrimaryValue(), condition.getComparator());
        }
        return false;
    }

    /** Compare two values based on comparator */
    private boolean compareValues(double actualValue, double requiredValue, PromotionEnum.Comp comparator) {
        return switch (comparator) {
            case EQUAL -> actualValue == requiredValue;
            case GREATER -> actualValue > requiredValue;
            case GREATER_EQUAL -> actualValue >= requiredValue;
            case LESS -> actualValue < requiredValue;
            case LESS_EQUAL -> actualValue <= requiredValue;
            case BETWEEN -> false; // BETWEEN requires secondaryValue - not implemented yet
            default -> false;
        };
    }

    /** Calculate the total discount applied to the invoice. */
    public double calculatePromotion() {
        if (promotion == null) return 0.0;
        if (!checkPromotionConditions()) return 0.0;

        double totalDiscount = 0.0;
        List<PromotionAction> sortedActionOrderList = promotion.getActions().stream()
                .sorted(Comparator.comparingInt(PromotionAction::getActionOrder))
                .toList();

        for (PromotionAction action : sortedActionOrderList) {
            if (action.getTarget() == Target.PRODUCT) {
                totalDiscount += calculateProductDiscount(action);
            } else if (action.getTarget() == Target.ORDER_SUBTOTAL) {
                totalDiscount += calculateOrderDiscount(action);
            }
        }
        return totalDiscount;
    }

    /** Calculate discount for product-targeted promotion action */
    private double calculateProductDiscount(PromotionAction action) {
        double discount = 0.0;
        Product targetProduct = (action.getProductUOM() != null && action.getProductUOM().getProduct() != null)
                ? action.getProductUOM().getProduct() : null;

        for (InvoiceLine line : invoiceLineList) {
            if (line.getProduct().getId().equals(targetProduct.getId())) {
                double lineSubtotalWithVat = line.calculateTotalAmount();

                double primaryValue = (action.getValue() != null) ? action.getValue().doubleValue() : 0.0;

                if (action.getType() == PromotionEnum.ActionType.FIXED_DISCOUNT) {
                    discount += primaryValue;
                } else if (action.getType() == PromotionEnum.ActionType.PERCENT_DISCOUNT) {
                    discount += lineSubtotalWithVat * (primaryValue / 100);
                }
            }
        }
        return discount;
    }

    /** Calculate discount for order subtotal-targeted promotion action */
    private double calculateOrderDiscount(PromotionAction action) {
        double discount = 0.0;
        double subtotalWithVat = calculateSubtotalWithVat();

        double primaryValue = (action.getValue() != null) ? action.getValue().doubleValue() : 0.0;

        if (action.getType() == PromotionEnum.ActionType.FIXED_DISCOUNT) {
            discount = primaryValue;
        } else if (action.getType() == PromotionEnum.ActionType.PERCENT_DISCOUNT) {
            discount = subtotalWithVat * (primaryValue / 100);
        }
        return discount;
    }

    /** Calculate the total amount of the invoice after applying promotions. */
    public double calculateTotal() {
        return Math.ceil(calculateSubtotalWithVat() - calculatePromotion());
    }

    @Override
    public String toString() {
        return super.toString();
    }
}