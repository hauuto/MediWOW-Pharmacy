package com.dao;

import com.entities.Invoice;
import com.entities.InvoiceLine;
import com.interfaces.IInvoice;
import com.utils.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

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
    public void saveInvoice(Invoice invoice) {
        Transaction transaction = null;
        Session session = null;
        try {
            session = sessionFactory.openSession();
            transaction = session.beginTransaction();
            session.persist(invoice);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            e.printStackTrace();
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

                    // Fetch products in promotion conditions
                    session.createQuery(
                        "SELECT DISTINCT pc FROM PromotionCondition pc " +
                        "LEFT JOIN FETCH pc.product " +
                        "WHERE pc.promotion.id = :promotionId",
                        com.entities.PromotionCondition.class
                    ).setParameter("promotionId", invoice.getPromotion().getId()).list();

                    // Fetch products in promotion actions
                    session.createQuery(
                        "SELECT DISTINCT pa FROM PromotionAction pa " +
                        "LEFT JOIN FETCH pa.product " +
                        "WHERE pa.promotion.id = :promotionId",
                        com.entities.PromotionAction.class
                    ).setParameter("promotionId", invoice.getPromotion().getId()).list();
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
                    "LEFT JOIN FETCH p.lotList " +
                    "WHERE il.invoice.id = :id",
                    InvoiceLine.class
                ).setParameter("id", id).list();

                // Fetch unitOfMeasure for invoice lines
                session.createQuery(
                    "SELECT DISTINCT il FROM InvoiceLine il " +
                    "LEFT JOIN FETCH il.unitOfMeasure " +
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

                    // Fetch products in promotion conditions
                    session.createQuery(
                        "SELECT DISTINCT pc FROM PromotionCondition pc " +
                        "LEFT JOIN FETCH pc.product " +
                        "WHERE pc.promotion IN :promotions",
                        com.entities.PromotionCondition.class
                    ).setParameter("promotions", promotions).list();

                    // Fetch products in promotion actions
                    session.createQuery(
                        "SELECT DISTINCT pa FROM PromotionAction pa " +
                        "LEFT JOIN FETCH pa.product " +
                        "WHERE pa.promotion IN :promotions",
                        com.entities.PromotionAction.class
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
                    "LEFT JOIN FETCH p.lotList " +
                    "WHERE il.invoice IN :invoices",
                    InvoiceLine.class
                ).setParameter("invoices", invoices).list();

                // Fetch unitOfMeasure for invoice lines
                session.createQuery(
                    "SELECT DISTINCT il FROM InvoiceLine il " +
                    "LEFT JOIN FETCH il.unitOfMeasure " +
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
                    "LEFT JOIN FETCH p.lotList " +
                    "WHERE il.invoice.id = :invoiceId",
                    InvoiceLine.class
                ).setParameter("invoiceId", invoiceId).list();

                // Fetch unitOfMeasure
                session.createQuery(
                    "SELECT DISTINCT il FROM InvoiceLine il " +
                    "LEFT JOIN FETCH il.unitOfMeasure " +
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
                    "LEFT JOIN FETCH p.lotList " +
                    "WHERE il IN :invoiceLines",
                    InvoiceLine.class
                ).setParameter("invoiceLines", invoiceLines).list();

                // Fetch unitOfMeasure
                session.createQuery(
                    "SELECT DISTINCT il FROM InvoiceLine il " +
                    "LEFT JOIN FETCH il.unitOfMeasure " +
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
}
