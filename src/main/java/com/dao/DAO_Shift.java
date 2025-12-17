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
}

