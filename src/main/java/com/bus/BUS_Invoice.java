package com.bus;

import com.dao.DAO_Invoice;
import com.entities.Invoice;
import com.entities.InvoiceLine;
import com.interfaces.IInvoice;

import java.util.List;

public class BUS_Invoice  implements IInvoice {
    private final DAO_Invoice daoInvoice;

    public BUS_Invoice() {
        this.daoInvoice = new DAO_Invoice();
    }

    @Override
    public void saveInvoice(Invoice invoice) {
        daoInvoice.saveInvoice(invoice);
    }

    @Override
    public Invoice getInvoice(String id) {
        return daoInvoice.getInvoice(id);
    }

    @Override
    public List<Invoice> getAllInvoices() {
        return daoInvoice.getAllInvoices();
    }

    @Override
    public List<InvoiceLine> getInvoiceLinesByInvoiceId(String invoiceId) {
        return daoInvoice.getInvoiceLinesByInvoiceId(invoiceId);
    }

    @Override
    public List<InvoiceLine> getAllInvoiceLines() {
        return daoInvoice.getAllInvoiceLines();
    }

    @Override
    public List<String> getAllPrescriptionCodes() {
        return daoInvoice.getAllPrescriptionCodes();
    }
}
