package com.interfaces;

import com.entities.Invoice;
import com.entities.InvoiceLine;

import java.util.List;

/**
 * @author Bùi Quốc Trụ
 */
public interface IInvoice {
    /**
     * Saves an invoice to the database.
     * @param invoice The invoice to be saved.
     */
    void saveInvoice(Invoice invoice);

    /**
     * Retrieves an invoice by its ID.
     * @param id The ID of the invoice.
     * @return Invoice
     */
    Invoice getInvoice(String id);

    /**
     * Retrieves all invoices from the database.
     * @return List of invoices.
     */
    List<Invoice> getAllInvoices();

    /**
     * Retrieves all invoice lines associated with a specific invoice ID.
     * @param invoiceId The ID of the invoice.
     * @return List of invoice lines.
     */
    List<InvoiceLine> getInvoiceLinesByInvoiceId(String invoiceId);

    /**
     * Retrieves all invoice lines from the database.
     * @return List of invoice lines.
     */
    List<InvoiceLine> getAllInvoiceLines();

    /**
     * Retrieves all prescription codes from the database.
     * @return List of prescription codes.
     */
    List<String> getAllPrescriptionCodes();
}
