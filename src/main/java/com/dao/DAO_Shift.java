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

            // Calculate cash from SALES invoices (add to cash)
            String hqlSales = "SELECT COALESCE(SUM(il.quantity * il.unitPrice), 0) " +
                            "FROM InvoiceLine il " +
                            "WHERE il.invoice.shift.id = :shiftId " +
                            "AND il.invoice.type = 'SALES' " +
                            "AND il.invoice.paymentMethod = 'CASH'";

            Double salesCashDouble = session.createQuery(hqlSales, Double.class)
                .setParameter("shiftId", shiftId)
                .uniqueResult();

            BigDecimal salesCash = salesCashDouble != null ? BigDecimal.valueOf(salesCashDouble) : BigDecimal.ZERO;

            // Calculate cash from RETURN invoices (subtract from cash)
            String hqlReturn = "SELECT COALESCE(SUM(il.quantity * il.unitPrice), 0) " +
                             "FROM InvoiceLine il " +
                             "WHERE il.invoice.shift.id = :shiftId " +
                             "AND il.invoice.type = 'RETURN' " +
                             "AND il.invoice.paymentMethod = 'CASH'";

            Double returnCashDouble = session.createQuery(hqlReturn, Double.class)
                .setParameter("shiftId", shiftId)
                .uniqueResult();

            BigDecimal returnCash = returnCashDouble != null ? BigDecimal.valueOf(returnCashDouble) : BigDecimal.ZERO;

            // Calculate cash from EXCHANGE invoices (add to cash)
            String hqlExchange = "SELECT COALESCE(SUM(il.quantity * il.unitPrice), 0) " +
                               "FROM InvoiceLine il " +
                               "WHERE il.invoice.shift.id = :shiftId " +
                               "AND il.invoice.type = 'EXCHANGE' " +
                               "AND il.invoice.paymentMethod = 'CASH'";

            Double exchangeCashDouble = session.createQuery(hqlExchange, Double.class)
                .setParameter("shiftId", shiftId)
                .uniqueResult();

            BigDecimal exchangeCash = exchangeCashDouble != null ? BigDecimal.valueOf(exchangeCashDouble) : BigDecimal.ZERO;

            // Calculate total: startCash + sales - return + exchange
            return startCash.add(salesCash).subtract(returnCash).add(exchangeCash);

        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }
}

