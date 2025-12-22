package com.entities;

import com.dao.DAO_UnitOfMeasure;
import com.enums.InvoiceType;
import com.enums.LineType;
import com.enums.PaymentMethod;
import com.enums.PromotionEnum;
import com.enums.PromotionEnum.ConditionType;
import com.enums.PromotionEnum.Target;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import javax.swing.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
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

    // --- QUẢN LÝ CA (SHIFT MANAGEMENT) ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shift", nullable = true)
    private Shift shift;
    // -------------------------------------

    protected Invoice() {}

    public Invoice(InvoiceType type, Staff creator, Shift shift) {
        this.type = type;
        this.creationDate = LocalDateTime.now();
        this.creator = creator;
        this.shift = shift;
        this.invoiceLineList = new ArrayList<>();
    }

    public Invoice(String id,
                   InvoiceType type,
                   LocalDateTime creationDate,
                   Staff creator,
                   Customer customer,
                   String notes,
                   String prescriptionCode,
                   List<InvoiceLine> invoiceLineList,
                   Promotion promotion,
                   PaymentMethod paymentMethod,
                   Invoice referencedInvoice,
                   Shift shift) {
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

    // --- Getters / Setters cơ bản ---

    public Shift getShift() {
        return shift;
    }

    public void setShift(Shift shift) {
        this.shift = shift;
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

    // =====================================================
    // Invoice lines helpers
    // =====================================================

    public boolean addInvoiceLine(InvoiceLine invoiceLine) {
        if (invoiceLine == null)
            return false;

        for (InvoiceLine existingLine : invoiceLineList)
            if (existingLine.equals(invoiceLine))
                return false;

        invoiceLineList.add(invoiceLine);
        return true;
    }

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

    public boolean removeInvoiceLine(String productId, String unitOfMeasureName) {
        return invoiceLineList.removeIf(line ->
                line.getProduct().getId().equals(productId) &&
                        line.getUnitOfMeasure().getName().equals(unitOfMeasureName)
        );
    }

    // =====================================================
    // Money calculations (BigDecimal là source of truth)
    // =====================================================

    public BigDecimal calculateSubtotal() {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (InvoiceLine line : invoiceLineList) {
            if (line != null) subtotal = subtotal.add(line.calculateSubtotal());
        }
        return subtotal;
    }

    public BigDecimal calculateVatAmount() {
        BigDecimal vatAmount = BigDecimal.ZERO;
        for (InvoiceLine line : invoiceLineList) {
            if (line != null) vatAmount = vatAmount.add(line.calculateVatAmount());
        }
        return vatAmount;
    }

    public BigDecimal calculateSubtotalWithVat() {
        return calculateSubtotal().add(calculateVatAmount());
    }

    // =====================================================
    // Promotion checks & discounts (đÃ SỬA CHO PRODUCT_UOM)
    // =====================================================

    /** Check if the invoice satisfies all promotion conditions (using current this.promotion). */
    public boolean checkPromotionConditions() {
        if (promotion == null) return false;
        return checkPromotionConditions(promotion);
    }

    /** Check if the invoice satisfies all conditions for a given promotion (no mutate). */
    private boolean checkPromotionConditions(Promotion promo) {
        if (promo == null) return false;
        List<PromotionCondition> conditions = promo.getConditions();
        if (conditions == null || conditions.isEmpty()) return true;

        for (PromotionCondition condition : conditions) {
            if (!checkSingleCondition(condition)) return false;
        }
        return true;
    }

    /** Check a single promotion condition (route by target). */
    private boolean checkSingleCondition(PromotionCondition condition) {
        if (condition == null) return false;

        Target target = condition.getTarget();
        if (target == Target.PRODUCT) {
            return checkProductCondition(condition);
        } else if (target == Target.ORDER_SUBTOTAL) {
            return checkOrderCondition(condition);
        }
        // TODO: other targets (ORDER_TOTAL, CATEGORY, etc.) if you add them
        return false;
    }

    /**
     * Check PRODUCT-targeted condition.
     * Ưu tiên productUOM (UnitOfMeasure) -> chính xác theo Product + Measurement.
     * Nếu productUOM null nhưng getProduct() != null -> áp dụng cho mọi UOM của Product đó.
     */
    private boolean checkProductCondition(PromotionCondition condition) {
        ConditionType type = condition.getConditionType();
        if (type != ConditionType.PRODUCT_QTY) {
            return false; // hiện tại mới handle PRODUCT_QTY
        }

        UnitOfMeasure targetUOM = condition.getProductUOM();
        Product targetProduct = condition.getProduct(); // helper: productUOM != null ? productUOM.getProduct() : null

        if (targetUOM == null && targetProduct == null) {
            // không có product / productUOM thì condition không hợp lệ
            return false;
        }

        BigDecimal totalQty = BigDecimal.ZERO;

        for (InvoiceLine line : invoiceLineList) {
            if (line == null) continue;

            Product lineProduct = line.getProduct();
            UnitOfMeasure lineUOM = line.getUnitOfMeasure();
            if (lineProduct == null) continue;

            // 1) Nếu condition có product cụ thể, phải match product
            if (targetProduct != null) {
                String tgtId = targetProduct.getId();
                String lineId = lineProduct.getId();
                if (tgtId == null || lineId == null || !tgtId.equals(lineId)) {
                    continue;
                }
            }

            // 2) Nếu condition có productUOM cụ thể, phải match cả product + measurement
            if (targetUOM != null) {
                if (lineUOM == null) continue;
                if (!targetUOM.equals(lineUOM)) continue; // equals() đã so sánh theo productId + measurementId
            }

            // Nếu qua được tất cả filter ở trên, cộng quantity
            totalQty = totalQty.add(BigDecimal.valueOf(line.getQuantity()));
        }

        return compareValues(totalQty, condition.getPrimaryValue(), condition.getComparator());
    }

    /** Check ORDER_SUBTOTAL condition (e.g., order total >= X). */
    private boolean checkOrderCondition(PromotionCondition condition) {
        if (condition.getConditionType() != ConditionType.ORDER_SUBTOTAL) {
            return false;
        }

        BigDecimal subtotalWithVat = calculateSubtotalWithVat();
        return compareValues(subtotalWithVat, condition.getPrimaryValue(), condition.getComparator());
    }

    // =====================================================
    // Promotion discount calculation helpers
    // =====================================================

    /**
     * Calculate discount for PRODUCT-targeted action.
     * Ưu tiên productUOM (UnitOfMeasure). Nếu null thì áp dụng theo Product.
     */
    private BigDecimal calculateProductDiscount(PromotionAction action) {
        if (action == null) return BigDecimal.ZERO;

        UnitOfMeasure targetUOM = action.getProductUOM();
        Product targetProduct = action.getProduct(); // helper: productUOM != null ? productUOM.getProduct() : null

        if (targetUOM == null && targetProduct == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal discount = BigDecimal.ZERO;

        for (InvoiceLine line : invoiceLineList) {
            if (line == null) continue;

            Product lineProduct = line.getProduct();
            UnitOfMeasure lineUOM = line.getUnitOfMeasure();
            if (lineProduct == null) continue;

            // 1) filter theo Product nếu có
            if (targetProduct != null) {
                String tgtId = targetProduct.getId();
                String lineId = lineProduct.getId();
                if (tgtId == null || lineId == null || !tgtId.equals(lineId)) {
                    continue;
                }
            }

            // 2) filter thêm theo ProductUOM nếu có
            if (targetUOM != null) {
                if (lineUOM == null) continue;
                if (!targetUOM.equals(lineUOM)) continue;
            }

            // Lúc này line chính xác là line cần discount
            BigDecimal lineSubtotalWithVat = line.calculateTotalAmount();

            switch (action.getType()) {
                case PERCENT_DISCOUNT -> {
                    if (action.getValue() != null) {
                        BigDecimal rate = action.getValue()
                                .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
                        discount = discount.add(lineSubtotalWithVat.multiply(rate));
                    }
                }
                case FIXED_DISCOUNT -> {
                    if (action.getValue() != null) {
                        discount = discount.add(action.getValue());
                    }
                }
                // TODO: implement other action types if needed
                default -> { /* no-op */ }
            }
        }

        return discount;
    }

    /** Calculate discount for ORDER_SUBTOTAL-targeted action. */
    private BigDecimal calculateOrderDiscount(PromotionAction action) {
        if (action == null) return BigDecimal.ZERO;

        BigDecimal subtotalWithVat = calculateSubtotalWithVat();

        return switch (action.getType()) {
            case PERCENT_DISCOUNT -> {
                if (action.getValue() == null) yield BigDecimal.ZERO;
                BigDecimal rate = action.getValue()
                        .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
                yield subtotalWithVat.multiply(rate);
            }
            case FIXED_DISCOUNT -> action.getValue() != null ? action.getValue() : BigDecimal.ZERO;
            default -> BigDecimal.ZERO;
        };
    }

    /** Compare two BigDecimal values using comparator. */
    private boolean compareValues(BigDecimal actualValue,
                                  BigDecimal requiredValue,
                                  PromotionEnum.Comp comparator) {
        if (comparator == null) return false;

        if (actualValue == null) actualValue = BigDecimal.ZERO;
        if (requiredValue == null) requiredValue = BigDecimal.ZERO;

        int cmp = actualValue.compareTo(requiredValue);

        return switch (comparator) {
            case EQUAL -> cmp == 0;
            case GREATER -> cmp > 0;
            case GREATER_EQUAL -> cmp >= 0;
            case LESS -> cmp < 0;
            case LESS_EQUAL -> cmp <= 0;
            case BETWEEN -> false; // TODO: cần secondaryValue trong PromotionCondition để implement BETWEEN
        };
    }

    /** Int overload (nếu sau này còn dùng). */
    private boolean compareValues(int actualValue,
                                  BigDecimal requiredValue,
                                  PromotionEnum.Comp comparator) {
        return compareValues(BigDecimal.valueOf(actualValue), requiredValue, comparator);
    }

    /** Calculate discount for the currently attached promotion. */
    public BigDecimal calculatePromotion() {
        return calculatePromotion(this.promotion);
    }

    /** Calculate discount for a specific promotion (không mutate state). */
    private BigDecimal calculatePromotion(Promotion promo) {
        if (promo == null) return BigDecimal.ZERO;
        if (!checkPromotionConditions(promo)) return BigDecimal.ZERO;

        BigDecimal totalDiscount = BigDecimal.ZERO;

        List<PromotionAction> sortedActionOrderList = promo.getActions() == null
                ? List.of()
                : promo.getActions().stream()
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

    private List<InvoiceLine> calculateGifts(Promotion promotion) {
        List<InvoiceLine> giftLines = new ArrayList<>();
        if (promotion == null) return giftLines;

        List<PromotionAction> sortedActionOrderList = promotion.getActions() == null
                ? List.of()
                : promotion.getActions().stream()
                .sorted(Comparator.comparingInt(PromotionAction::getActionOrder))
                .toList();

        for (PromotionAction action : sortedActionOrderList) {
            if (action == null) continue;

            Product prod = action.getProduct();
            UnitOfMeasure uom = action.getProductUOM();
            int quantity = action.getValue() != null ? action.getValue().intValue() : 0;

            if (action.getType() == PromotionEnum.ActionType.PRODUCT_GIFT && prod != null && uom != null && quantity > 0) {
                // Gift line has 0 price and uses action quantity.
                InvoiceLine giftLine = new InvoiceLine(this, uom, quantity, BigDecimal.ZERO, LineType.SALE, new ArrayList<>());
                if (!giftLine.allocateLots()) {
                    // Not enough inventory for the gift -> skip.
                    continue;
                }
                giftLines.add(giftLine);
            }
        }
        return giftLines;
    }

    /**
     * Calculate gift lines for a promotion (does not mutate invoice state).
     * The UI layer can add/remove these lines to the invoice based on user selection.
     */
    public List<InvoiceLine> getGiftLinesForPromotion(Promotion promotion) {
        return calculateGifts(promotion);
    }

    // --- Result holder for applyPromotion ---
    public static class ApplyPromotionResult {
        private final BigDecimal totalDiscount;
        private final List<Promotion> validPromotions;

        public ApplyPromotionResult(BigDecimal totalDiscount, List<Promotion> validPromotions) {
            this.totalDiscount = totalDiscount == null
                    ? BigDecimal.ZERO
                    : totalDiscount.setScale(2, RoundingMode.HALF_UP);
            this.validPromotions = validPromotions == null
                    ? List.of()
                    : List.copyOf(validPromotions);
        }

        public BigDecimal getTotalDiscount() { return totalDiscount; }
        public List<Promotion> getValidPromotions() { return validPromotions; }
    }

    /**
     * Apply all active promotions to this invoice at its creation date.
     * - Lọc theo isActive + effectiveDate / endDate.
     * - Check điều kiện theo ProductUOM / Order subtotal.
     * - Cộng dồn discount theo thứ tự actionOrder.
     */
    public ApplyPromotionResult applyPromotion(List<Promotion> promotions) {
        if (promotions == null || promotions.isEmpty()) {
            return new ApplyPromotionResult(BigDecimal.ZERO, List.of());
        }

        LocalDate invoiceDate = this.creationDate != null
                ? this.creationDate.toLocalDate()
                : LocalDate.now();

        BigDecimal total = BigDecimal.ZERO;
        List<Promotion> valid = new ArrayList<>();

        for (Promotion promo : promotions) {
            if (promo == null) continue;

            // 1) Active flag
            if (!promo.getIsActive()) continue;

            // 2) Date window
            LocalDate start = promo.getEffectiveDate();
            LocalDate end = promo.getEndDate();
            boolean withinStart = (start == null) || !invoiceDate.isBefore(start);
            boolean withinEnd = (end == null) || !invoiceDate.isAfter(end);
            if (!(withinStart && withinEnd)) continue;

            // 3) Check conditions
            if (!checkPromotionConditions(promo)) continue;

            // 4) Calculate discount
            BigDecimal discount = calculatePromotion(promo);
            if (discount.compareTo(BigDecimal.ZERO) > 0) {
                total = total.add(discount);
                valid.add(promo);
            }
        }

        total = total.setScale(2, RoundingMode.HALF_UP);
        return new ApplyPromotionResult(total, valid);
    }



    /** Preferred BigDecimal total (subtotal + VAT - promotion). */
    public BigDecimal calculateTotal() {
        BigDecimal total = calculateSubtotalWithVat().subtract(calculatePromotion());
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
