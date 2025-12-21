package com.dao;

import com.entities.Invoice;
import com.entities.InvoiceLine;
import com.interfaces.IInvoice;
import com.utils.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Bùi Quốc Trụ
 */
public class DAO_Invoice implements IInvoice {
    private final SessionFactory sessionFactory;

    public DAO_Invoice() {
        this.sessionFactory = HibernateUtil.getSessionFactory();
    }

    @Override
    public String saveInvoice(Invoice invoice) {
        Transaction transaction = null;
        Session session = null;
        String generatedInvoiceId = null;

        try {
            session = sessionFactory.openSession();
            transaction = session.beginTransaction();

            // Log invoice details before saving
            System.out.println("========== ATTEMPTING TO SAVE INVOICE ==========");
            System.out.println("Invoice Type: " + invoice.getType());
            System.out.println("Creator: " + (invoice.getCreator() != null ? invoice.getCreator().getId() : "NULL"));
            System.out.println("Payment Method: " + invoice.getPaymentMethod());
            System.out.println("Invoice Lines: " + (invoice.getInvoiceLineList() != null ? invoice.getInvoiceLineList().size() : 0));
            System.out.println("Prescription Code: " + invoice.getPrescriptionCode());

            // Store invoice lines temporarily
            List<InvoiceLine> invoiceLines = new ArrayList<>(invoice.getInvoiceLineList());

            // Insert Invoice - trigger generates ID
            session.createNativeQuery(
                            "INSERT INTO Invoice (id, type, creationDate, creator, prescribedCustomer, prescriptionCode, referencedInvoice, promotion, paymentMethod, notes, shift) " +
                                    "VALUES (:id, :type, GETDATE(), :creator, :prescribedCustomer, :prescriptionCode, :referencedInvoice, :promotion, :paymentMethod, :notes, :shift)")
                    .setParameter("id", null)
                    .setParameter("type", invoice.getType().name())
                    .setParameter("creator", invoice.getCreator().getId())
                    .setParameter("prescribedCustomer",
                            invoice.getPrescribedCustomer() != null ? invoice.getPrescribedCustomer().getId() : null)
                    .setParameter("prescriptionCode", invoice.getPrescriptionCode())
                    .setParameter("referencedInvoice",
                            invoice.getReferencedInvoice() != null ? invoice.getReferencedInvoice().getId() : null)
                    .setParameter("promotion",
                            invoice.getPromotion() != null ? invoice.getPromotion().getId() : null)
                    .setParameter("paymentMethod", invoice.getPaymentMethod().name())
                    .setParameter("notes", invoice.getNotes())
                    .setParameter("shift",
                            invoice.getShift() != null ? invoice.getShift().getId() : null)
                    .executeUpdate();

            // Retrieve generated Invoice ID
            generatedInvoiceId = session.createNativeQuery(
                            "SELECT TOP 1 id FROM Invoice WHERE creator = :creatorId ORDER BY creationDate DESC",
                            String.class)
                    .setParameter("creatorId", invoice.getCreator().getId())
                    .uniqueResult();

            System.out.println("Generated Invoice ID from trigger: " + generatedInvoiceId);

            if (generatedInvoiceId == null) {
                throw new RuntimeException("Could not retrieve generated Invoice ID");
            }

            // Insert InvoiceLines
            for (InvoiceLine line : invoiceLines) {
                session.createNativeQuery(
                                "INSERT INTO InvoiceLine (id, invoice, product, unitOfMeasure, quantity, unitPrice, lineType) " +
                                        "VALUES (:id, :invoice, :product, :unitOfMeasure, :quantity, :unitPrice, :lineType)")
                        .setParameter("id", null)
                        .setParameter("invoice", generatedInvoiceId)
                        .setParameter("product", line.getProduct().getId())
                        .setParameter("unitOfMeasure", line.getUnitOfMeasure().getId())
                        .setParameter("quantity", line.getQuantity())
                        .setParameter("unitPrice", line.getUnitPrice())
                        .setParameter("lineType", line.getLineType().name())
                        .executeUpdate();

                // Retrieve generated InvoiceLine ID
                String generatedInvoiceLineId = session.createNativeQuery(
                                "SELECT TOP 1 id FROM InvoiceLine WHERE invoice = :invoiceId AND product = :productId AND unitOfMeasure = :uom ORDER BY id DESC",
                                String.class)
                        .setParameter("invoiceId", generatedInvoiceId)
                        .setParameter("productId", line.getProduct().getId())
                        .setParameter("uom", line.getUnitOfMeasure().getId())
                        .uniqueResult();

                // Save Lot Allocations
                if (generatedInvoiceLineId != null && line.getLotAllocations() != null) {
                    for (com.entities.LotAllocation allocation : line.getLotAllocations()) {
                        session.createNativeQuery(
                                        "INSERT INTO LotAllocation (id, invoiceLine, lot, quantity) " +
                                                "VALUES (:id, :invoiceLine, :lot, :quantity)")
                                .setParameter("id", allocation.getId())
                                .setParameter("invoiceLine", generatedInvoiceLineId)
                                .setParameter("lot", allocation.getLot().getId())
                                .setParameter("quantity", allocation.getQuantity())
                                .executeUpdate();
                    }
                    System.out.println("Saved " + line.getLotAllocations().size() +
                            " LotAllocations for InvoiceLine: " + generatedInvoiceLineId);
                }
            }

            transaction.commit();

            System.out.println("========== INVOICE SAVED SUCCESSFULLY ==========");
            System.out.println("Final Invoice ID: " + generatedInvoiceId);
            System.out.println("===============================================");

            return generatedInvoiceId;

        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            System.err.println("========== ERROR SAVING INVOICE ==========");
            System.err.println("Error message: " + e.getMessage());
            System.err.println("Error type: " + e.getClass().getName());
            e.printStackTrace();
            System.err.println("==========================================");
            throw new RuntimeException("Failed to save invoice: " + e.getMessage(), e);
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }


    @Override
    public Invoice getInvoice(String id) {
        Session session = null;
        try {
            session = sessionFactory.openSession();
            // Fetch invoice with invoiceLineList first
            Invoice invoice = session.createQuery(
                "SELECT DISTINCT i FROM Invoice i " +
                "LEFT JOIN FETCH i.invoiceLineList " +
                "WHERE i.id = :id",
                Invoice.class
            ).setParameter("id", id).uniqueResult();

            if (invoice != null) {
                // Fetch creator (Staff)
                session.createQuery(
                    "SELECT DISTINCT i FROM Invoice i " +
                    "LEFT JOIN FETCH i.creator " +
                    "WHERE i.id = :id",
                    Invoice.class
                ).setParameter("id", id).uniqueResult();

                // Fetch promotion
                session.createQuery(
                    "SELECT DISTINCT i FROM Invoice i " +
                    "LEFT JOIN FETCH i.promotion " +
                    "WHERE i.id = :id",
                    Invoice.class
                ).setParameter("id", id).uniqueResult();

                // Fetch referencedInvoice if exists
                session.createQuery(
                    "SELECT DISTINCT i FROM Invoice i " +
                    "LEFT JOIN FETCH i.referencedInvoice " +
                    "WHERE i.id = :id",
                    Invoice.class
                ).setParameter("id", id).uniqueResult();

                // Fetch promotion conditions if promotion exists
                if (invoice.getPromotion() != null) {
                    session.createQuery(
                        "SELECT DISTINCT p FROM Promotion p " +
                        "LEFT JOIN FETCH p.conditions " +
                        "WHERE p.id = :promotionId",
                        com.entities.Promotion.class
                    ).setParameter("promotionId", invoice.getPromotion().getId()).uniqueResult();

                    // Fetch promotion actions
                    session.createQuery(
                        "SELECT DISTINCT p FROM Promotion p " +
                        "LEFT JOIN FETCH p.actions " +
                        "WHERE p.id = :promotionId",
                        com.entities.Promotion.class
                    ).setParameter("promotionId", invoice.getPromotion().getId()).uniqueResult();
                }

                // Fetch products in invoice lines
                session.createQuery(
                    "SELECT DISTINCT il FROM InvoiceLine il " +
                    "LEFT JOIN FETCH il.product " +
                    "WHERE il.invoice.id = :id",
                    InvoiceLine.class
                ).setParameter("id", id).list();

                // Fetch lots for products in invoice lines
                session.createQuery(
                    "SELECT DISTINCT il FROM InvoiceLine il " +
                    "LEFT JOIN FETCH il.product p " +
                    "LEFT JOIN FETCH p.lotSet " +
                    "WHERE il.invoice.id = :id",
                    InvoiceLine.class
                ).setParameter("id", id).list();

                // Fetch lotAllocations for invoice lines
                session.createQuery(
                    "SELECT DISTINCT il FROM InvoiceLine il " +
                    "LEFT JOIN FETCH il.lotAllocations " +
                    "WHERE il.invoice.id = :id",
                    InvoiceLine.class
                ).setParameter("id", id).list();
            }

            return invoice;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    @Override
    public List<Invoice> getAllInvoices() {
        Session session = null;
        try {
            session = sessionFactory.openSession();
            // Fetch invoices with invoiceLineList first
            List<Invoice> invoices = session.createQuery(
                "SELECT DISTINCT i FROM Invoice i " +
                "LEFT JOIN FETCH i.invoiceLineList",
                Invoice.class
            ).list();

            if (!invoices.isEmpty()) {
                // Fetch creator (Staff) for all invoices
                session.createQuery(
                    "SELECT DISTINCT i FROM Invoice i " +
                    "LEFT JOIN FETCH i.creator " +
                    "WHERE i IN :invoices",
                    Invoice.class
                ).setParameter("invoices", invoices).list();

                // Fetch promotion for all invoices
                session.createQuery(
                    "SELECT DISTINCT i FROM Invoice i " +
                    "LEFT JOIN FETCH i.promotion " +
                    "WHERE i IN :invoices",
                    Invoice.class
                ).setParameter("invoices", invoices).list();

                // Fetch referencedInvoice for all invoices
                session.createQuery(
                    "SELECT DISTINCT i FROM Invoice i " +
                    "LEFT JOIN FETCH i.referencedInvoice " +
                    "WHERE i IN :invoices",
                    Invoice.class
                ).setParameter("invoices", invoices).list();

                // Get all promotions from invoices that have promotions
                List<com.entities.Promotion> promotions = invoices.stream()
                    .map(Invoice::getPromotion)
                    .filter(p -> p != null)
                    .distinct()
                    .toList();

                if (!promotions.isEmpty()) {
                    // Fetch promotion conditions
                    session.createQuery(
                        "SELECT DISTINCT p FROM Promotion p " +
                        "LEFT JOIN FETCH p.conditions " +
                        "WHERE p IN :promotions",
                        com.entities.Promotion.class
                    ).setParameter("promotions", promotions).list();

                    // Fetch promotion actions
                    session.createQuery(
                        "SELECT DISTINCT p FROM Promotion p " +
                        "LEFT JOIN FETCH p.actions " +
                        "WHERE p IN :promotions",
                        com.entities.Promotion.class
                    ).setParameter("promotions", promotions).list();
                }

                // Fetch products in invoice lines
                session.createQuery(
                    "SELECT DISTINCT il FROM InvoiceLine il " +
                    "LEFT JOIN FETCH il.product " +
                    "WHERE il.invoice IN :invoices",
                    InvoiceLine.class
                ).setParameter("invoices", invoices).list();

                // Fetch lots for products in invoice lines
                session.createQuery(
                    "SELECT DISTINCT il FROM InvoiceLine il " +
                    "LEFT JOIN FETCH il.product p " +
                    "LEFT JOIN FETCH p.lotSet " +
                    "WHERE il.invoice IN :invoices",
                    InvoiceLine.class
                ).setParameter("invoices", invoices).list();

                // Fetch lotAllocations for invoice lines
                session.createQuery(
                    "SELECT DISTINCT il FROM InvoiceLine il " +
                    "LEFT JOIN FETCH il.lotAllocations " +
                    "WHERE il.invoice IN :invoices",
                    InvoiceLine.class
                ).setParameter("invoices", invoices).list();
            }

            return invoices;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    @Override
    public List<InvoiceLine> getInvoiceLinesByInvoiceId(String invoiceId) {
        Session session = null;
        try {
            session = sessionFactory.openSession();
            // Fetch invoice lines with product first
            List<InvoiceLine> invoiceLines = session.createQuery(
                "SELECT DISTINCT il FROM InvoiceLine il " +
                "LEFT JOIN FETCH il.product " +
                "WHERE il.invoice.id = :invoiceId",
                InvoiceLine.class
            ).setParameter("invoiceId", invoiceId).list();

            if (!invoiceLines.isEmpty()) {
                // Fetch product's lots
                session.createQuery(
                    "SELECT DISTINCT il FROM InvoiceLine il " +
                    "LEFT JOIN FETCH il.product p " +
                    "LEFT JOIN FETCH p.lotSet " +
                    "WHERE il.invoice.id = :invoiceId",
                    InvoiceLine.class
                ).setParameter("invoiceId", invoiceId).list();

                // Fetch lotAllocations
                session.createQuery(
                    "SELECT DISTINCT il FROM InvoiceLine il " +
                    "LEFT JOIN FETCH il.lotAllocations " +
                    "WHERE il.invoice.id = :invoiceId",
                    InvoiceLine.class
                ).setParameter("invoiceId", invoiceId).list();

                // Fetch invoice
                session.createQuery(
                    "SELECT DISTINCT il FROM InvoiceLine il " +
                    "LEFT JOIN FETCH il.invoice " +
                    "WHERE il.invoice.id = :invoiceId",
                    InvoiceLine.class
                ).setParameter("invoiceId", invoiceId).list();
            }

            return invoiceLines;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    @Override
    public List<InvoiceLine> getAllInvoiceLines() {
        Session session = null;
        try {
            session = sessionFactory.openSession();
            // Fetch invoice lines with product first
            List<InvoiceLine> invoiceLines = session.createQuery(
                "SELECT DISTINCT il FROM InvoiceLine il " +
                "LEFT JOIN FETCH il.product",
                InvoiceLine.class
            ).list();

            if (!invoiceLines.isEmpty()) {
                // Fetch product's lots
                session.createQuery(
                    "SELECT DISTINCT il FROM InvoiceLine il " +
                    "LEFT JOIN FETCH il.product p " +
                    "LEFT JOIN FETCH p.lotSet " +
                    "WHERE il IN :invoiceLines",
                    InvoiceLine.class
                ).setParameter("invoiceLines", invoiceLines).list();

                // Fetch lotAllocations
                session.createQuery(
                    "SELECT DISTINCT il FROM InvoiceLine il " +
                    "LEFT JOIN FETCH il.lotAllocations " +
                    "WHERE il IN :invoiceLines",
                    InvoiceLine.class
                ).setParameter("invoiceLines", invoiceLines).list();

                // Fetch invoice
                session.createQuery(
                    "SELECT DISTINCT il FROM InvoiceLine il " +
                    "LEFT JOIN FETCH il.invoice " +
                    "WHERE il IN :invoiceLines",
                    InvoiceLine.class
                ).setParameter("invoiceLines", invoiceLines).list();
            }

            return invoiceLines;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    @Override
    public List<String> getAllPrescriptionCodes() {
        Session session = null;
        try {
            session = sessionFactory.openSession();
            List<String> prescriptionCodes = session.createQuery(
                "SELECT DISTINCT i.prescriptionCode FROM Invoice i " +
                "WHERE i.prescriptionCode IS NOT NULL " +
                "ORDER BY i.prescriptionCode",
                String.class
            ).list();

            // Convert all prescription codes to lowercase
            return prescriptionCodes.stream()
                .map(String::toLowerCase)
                .toList();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    /**
     * Search top 5 invoices by id or invoice number (contains).
     */
    public List<Invoice> searchTop5ById(String keyword) {
        if (keyword == null) return java.util.Collections.emptyList();
        String kw = keyword.trim();
        if (kw.isEmpty()) return java.util.Collections.emptyList();

        Session session = null;
        try {
            session = sessionFactory.openSession();
            String jpql = "FROM Invoice i WHERE lower(i.id) LIKE :kw OR lower(i.id) LIKE :shortId ORDER BY i.creationDate DESC";
            Query<Invoice> q = session.createQuery(jpql, Invoice.class);
            q.setParameter("kw", "%" + kw.toLowerCase() + "%");
            q.setParameter("shortId", "%" + kw.toLowerCase() + "%");
            q.setMaxResults(5);
            return q.list();
        } catch (Exception e) {
            e.printStackTrace();
            return java.util.Collections.emptyList();
        } finally {
            if (session != null && session.isOpen()) session.close();
        }
    }

}
