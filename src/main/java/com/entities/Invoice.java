package com.entities;

import com.enums.InvoiceType;
import com.enums.PaymentMethod;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author Bùi Quốc Trụ
 */
public class Invoice {
    private final String id;
    private InvoiceType type;
    private String notes;
    private LocalDateTime creationDate;
    private Staff creator;
    private String prescriptionCode;
    private PrescribedCustomer prescribedCustomer;
    private List<InvoiceLine> invoiceLineList;
    private Promotion promotion;
    private PaymentMethod paymentMethod;
    private Invoice referencedInvoice;

    public Invoice(String id, InvoiceType type, String notes, LocalDateTime creationDate, Staff creator, String prescriptionCode, PrescribedCustomer prescribedCustomer, List<InvoiceLine> invoiceLineList, Promotion promotion, PaymentMethod paymentMethod, Invoice referencedInvoice) {
        this.id = id;
        this.type = type;
        this.notes = notes;
        this.creationDate = creationDate;
        this.creator = creator;
        this.prescriptionCode = prescriptionCode;
        this.prescribedCustomer = prescribedCustomer;
        this.invoiceLineList = invoiceLineList;
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

    public void setType(InvoiceType type) {
        this.type = type;
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

    public void setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
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

    public PrescribedCustomer getPrescribedCustomer() {
        return prescribedCustomer;
    }

    public void setPrescribedCustomer(PrescribedCustomer prescribedCustomer) {
        this.prescribedCustomer = prescribedCustomer;
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

    @Override
    public String toString() {
        return super.toString();
    }
}
