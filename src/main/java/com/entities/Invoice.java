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
        this.id = "placeholder-id"; // Placeholder ID, should be replaced with actual ID generation logic
        this.type = type;
        this.creationDate = LocalDateTime.now();
        this.creator = creator;
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
     * Updates the quantity of an existing InvoiceLine in the invoice.
     *
     * @param invoiceLine The InvoiceLine with updated quantity.
     * @return true if the InvoiceLine was found and updated, false otherwise.
     */
    public boolean updateInvoiceLine(InvoiceLine invoiceLine) {
        if (invoiceLine == null)
            return false;

        for (InvoiceLine existingLine : invoiceLineList) {
            if (existingLine.equals(invoiceLine)) {
                existingLine.setQuantity(invoiceLine.getQuantity());
                return true;
            }
        }

        return false;
    }

    /**
     * @author Bùi Quốc Trụ
     *
     * Removes an InvoiceLine from the invoice.
     *
     * @param invoiceLine The InvoiceLine to be removed.
     * @return true if the InvoiceLine was removed, false otherwise.
     */
    public boolean removeInvoiceLine(InvoiceLine invoiceLine) {
        return invoiceLineList.remove(invoiceLine);
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
     * @author Bùi Quốc Trụ
     *
     * Calculate the total discount applied to the invoice.
     *
     * @return The total discount.
     */
    public double calculatePromotion() {
        if (promotion == null)
            return 0.0;

        double discount = calculateSubtotalWithVat();
        List<PromotionAction> sortedActionOrderList = promotion.getActions().stream()
                .sorted((a, b) -> Integer.compare(a.getActionOrder(), b.getActionOrder()))
                .toList();

        for (PromotionAction action : sortedActionOrderList) {
            if (action.getType().equals(PromotionEnum.ActionType.FIXED_DISCOUNT))
                discount -= action.getPrimaryValue();
            else if (action.getType().equals(PromotionEnum.ActionType.PERCENT_DISCOUNT))
                discount -= discount * (action.getPrimaryValue() / 100);
        }

        return calculateSubtotalWithVat() - discount;
    }

    /**
     * @author Bùi Quốc Trụ
     *
     * Calculate the total amount of the invoice after applying promotions.
     *
     * @return The total amount.
     */
    public double calculateTotal() {
        return calculateSubtotalWithVat() - calculatePromotion();
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
