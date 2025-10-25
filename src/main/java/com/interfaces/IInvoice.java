package com.interfaces;

import com.entities.Invoice;

import java.util.List;

/**
 * @author Bùi Quốc Trụ
 */
public interface IInvoice {
    /**
     * Saves an invoice to the database.
     * @param invoice
     */
    void saveInvoice(Invoice invoice);

    /**
     * Retrieves an invoice by its ID.
     * @param id
     * @return
     */
    Invoice getInvoice(String id);

    /**
     * Retrieves all invoices from the database.
     * @return
     */
    List<Invoice> getAllInvoices();
}
