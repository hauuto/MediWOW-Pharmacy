package com.dao;

import com.entities.Shift;
import com.entities.Staff;
import com.enums.ShiftStatus;
import com.utils.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class DAO_Shift {
    private final SessionFactory sessionFactory;

    public DAO_Shift() {
        this.sessionFactory = HibernateUtil.getSessionFactory();
    }

    public Shift getOpenShiftByStaffId(String staffId) {
        if (staffId == null || staffId.isBlank()) return null;
        Session session = null;
        try {
            session = sessionFactory.openSession();
            return session.createQuery(
                "FROM Shift s WHERE s.staff.id = :staffId AND s.status = :status ORDER BY s.startTime DESC",
                Shift.class
            ).setParameter("staffId", staffId)
             .setParameter("status", ShiftStatus.OPEN)
             .setMaxResults(1)
             .uniqueResult();
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    public Shift openShift(Staff staff, BigDecimal startCash, String notes) {
        Transaction transaction = null;
        Session session = null;
        try {
            session = sessionFactory.openSession();
            transaction = session.beginTransaction();

            Shift shift = new Shift();
            shift.setStaff(session.merge(staff));
            shift.setStartCash(startCash);
            shift.setStartTime(LocalDateTime.now());
            shift.setStatus(ShiftStatus.OPEN);
            shift.setNotes(notes);

            session.persist(shift);
            transaction.commit();
            return shift;
        } catch (Exception ex) {
            if (transaction != null) transaction.rollback();
            throw new RuntimeException("Không thể mở ca làm việc: " + ex.getMessage(), ex);
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    public Shift closeShift(String shiftId, BigDecimal endCash, BigDecimal systemCash, String notes) {
        Transaction transaction = null;
        Session session = null;
        try {
            session = sessionFactory.openSession();
            transaction = session.beginTransaction();

            Shift shift = session.get(Shift.class, shiftId);
            if (shift == null) {
                throw new IllegalArgumentException("Không tìm thấy ca làm việc");
            }
            if (shift.getStatus() == ShiftStatus.CLOSED) {
                throw new IllegalStateException("Ca làm việc đã được đóng");
            }

            shift.setEndTime(LocalDateTime.now());
            shift.setEndCash(endCash);
            shift.setSystemCash(systemCash);
            shift.setStatus(ShiftStatus.CLOSED);
            if (notes != null && !notes.trim().isEmpty()) {
                String existingNotes = shift.getNotes();
                shift.setNotes(existingNotes != null ? existingNotes + "\n" + notes : notes);
            }

            session.merge(shift);
            transaction.commit();
            return shift;
        } catch (Exception ex) {
            if (transaction != null) transaction.rollback();
            throw new RuntimeException("Không thể đóng ca làm việc: " + ex.getMessage(), ex);
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    public BigDecimal calculateSystemCashForShift(String shiftId) {
        Session session = null;
        try {
            session = sessionFactory.openSession();

            // Get shift's start cash
            Shift shift = session.get(Shift.class, shiftId);
            if (shift == null) return BigDecimal.ZERO;

            BigDecimal startCash = shift.getStartCash() != null ? shift.getStartCash() : BigDecimal.ZERO;

            // Calculate total cash from invoices in this shift
            String hql = "SELECT COALESCE(SUM(CASE " +
                        "WHEN i.type = 'SALES' AND i.paymentMethod = 'CASH' THEN " +
                        "  (SELECT COALESCE(SUM(il.quantity * il.unitPrice), 0) FROM InvoiceLine il WHERE il.invoice.id = i.id) " +
                        "WHEN i.type = 'RETURN' AND i.paymentMethod = 'CASH' THEN " +
                        "  -(SELECT COALESCE(SUM(il.quantity * il.unitPrice), 0) FROM InvoiceLine il WHERE il.invoice.id = i.id) " +
                        "WHEN i.type = 'EXCHANGE' AND i.paymentMethod = 'CASH' THEN " +
                        "  (SELECT COALESCE(SUM(il.quantity * il.unitPrice), 0) FROM InvoiceLine il WHERE il.invoice.id = i.id) " +
                        "ELSE 0 END), 0) " +
                        "FROM Invoice i WHERE i.shift.id = :shiftId";

            BigDecimal cashFromInvoices = session.createQuery(hql, BigDecimal.class)
                .setParameter("shiftId", shiftId)
                .uniqueResult();

            if (cashFromInvoices == null) cashFromInvoices = BigDecimal.ZERO;

            return startCash.add(cashFromInvoices);
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }
}

