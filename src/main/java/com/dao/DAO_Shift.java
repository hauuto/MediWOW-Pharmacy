package com.dao;

import com.entities.Shift;
import com.entities.Staff;
import com.enums.ShiftStatus;
import com.interfaces.IShift;
import com.utils.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public class DAO_Shift implements IShift {
    private final SessionFactory sessionFactory;

    public DAO_Shift() {
        this.sessionFactory = HibernateUtil.getSessionFactory();
    }

    // ===== DAO-level operations =====

    @Override
    public Shift getOpenShiftByStaffId(String staffId) {
        if (staffId == null || staffId.isBlank()) return null;
        Session session = null;
        try {
            session = sessionFactory.openSession();
            return session.createQuery(
                "FROM Shift s JOIN FETCH s.staff WHERE s.staff.id = :staffId AND s.status = :status ORDER BY s.startTime DESC",
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

    /**
     * Get any open shift on a specific workstation
     * @param workstation Workstation identifier
     * @return Open shift on that workstation, or null
     */
    @Override
    public Shift getOpenShiftByWorkstation(String workstation) {
        if (workstation == null || workstation.isBlank()) return null;
        Session session = null;
        try {
            session = sessionFactory.openSession();

            System.out.println("=== DEBUG: DAO_Shift.getOpenShiftByWorkstation ===");
            System.out.println("Searching for workstation: " + workstation);

            Shift result = session.createQuery(
                "FROM Shift s JOIN FETCH s.staff WHERE s.workstation = :workstation AND s.status = :status ORDER BY s.startTime DESC",
                Shift.class
            ).setParameter("workstation", workstation)
             .setParameter("status", ShiftStatus.OPEN)
             .setMaxResults(1)
             .uniqueResult();

            System.out.println("Query result: " + (result != null ? "Found shift ID " + result.getId() : "No shift found"));
            System.out.println("=== END DEBUG ===");

            return result;
        } catch (Exception e) {
            System.err.println("ERROR in getOpenShiftByWorkstation: " + e.getMessage());
            e.printStackTrace();
            return null;
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    @Override
    public Shift openShift(Staff staff, BigDecimal startCash, String notes, String workstation) {
        Transaction transaction = null;
        Session session = null;
        try {
            session = sessionFactory.openSession();
            transaction = session.beginTransaction();

            System.out.println("=== DEBUG: DAO_Shift.openShift ===");
            System.out.println("Creating shift with workstation: " + workstation);

            Shift shift = new Shift();
            shift.setStaff(session.merge(staff));
            shift.setStartCash(startCash);
            shift.setStartTime(LocalDateTime.now());
            shift.setStatus(ShiftStatus.OPEN);
            shift.setNotes(notes);
            shift.setWorkstation(workstation);

            System.out.println("Shift object created, workstation set to: " + shift.getWorkstation());

            session.persist(shift);
            transaction.commit();

            System.out.println("Shift persisted successfully with ID: " + shift.getId());
            System.out.println("=== END DEBUG ===");

            return shift;
        } catch (Exception ex) {
            System.err.println("ERROR in openShift: " + ex.getMessage());
            ex.printStackTrace();
            if (transaction != null) transaction.rollback();
            throw new RuntimeException("Không thể mở ca làm việc: " + ex.getMessage(), ex);
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    @Override
    public Shift closeShift(String shiftId, BigDecimal endCash, BigDecimal systemCash, String notes, Staff closedBy, String closeReason) {
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

            // Set who closed the shift
            if (closedBy != null) {
                shift.setClosedBy(session.merge(closedBy));
            }

            // Set close reason if provided
            if (closeReason != null && !closeReason.trim().isEmpty()) {
                shift.setCloseReason(closeReason);
            }

            // Append notes
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

    @Override
    public BigDecimal calculateSystemCashForShift(String shiftId) {
        Session session = null;
        try {
            session = sessionFactory.openSession();

            // Get shift's start cash
            Shift shift = session.get(Shift.class, shiftId);
            if (shift == null) return BigDecimal.ZERO;

            BigDecimal startCash = shift.getStartCash() != null ? shift.getStartCash() : BigDecimal.ZERO;

            // Get all CASH invoices for this shift
            String hql = "FROM Invoice i WHERE i.shift.id = :shiftId AND i.paymentMethod = 'CASH'";
            List<com.entities.Invoice> invoices = session.createQuery(hql, com.entities.Invoice.class)
                .setParameter("shiftId", shiftId)
                .list();

            // Calculate cash using BigDecimal totals
            BigDecimal salesCash = BigDecimal.ZERO;
            BigDecimal returnCash = BigDecimal.ZERO;
            BigDecimal exchangeCash = BigDecimal.ZERO;

            for (com.entities.Invoice invoice : invoices) {
                BigDecimal total = invoice.calculateTotal();

                switch (invoice.getType()) {
                    case SALES:
                        salesCash = salesCash.add(total);
                        break;
                    case RETURN:
                        returnCash = returnCash.add(total);
                        break;
                    case EXCHANGE:
                        exchangeCash = exchangeCash.add(total);
                        break;
                }
            }

            // Calculate total: startCash + sales - return + exchange
            return startCash
                .add(salesCash)
                .subtract(returnCash)
                .add(exchangeCash);

        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    /**
     * List shifts opened within a specific day (00:00..23:59:59.999).
     * Fetches staff and closedBy to avoid lazy-loading issues in UI.
     */
    public List<Shift> listShiftsOpenedOn(LocalDate day) {
        if (day == null) return List.of();
        Session session = null;
        try {
            session = sessionFactory.openSession();
            LocalDateTime from = day.atStartOfDay();
            LocalDateTime to = day.atTime(LocalTime.MAX);

            return session.createQuery(
                    "SELECT DISTINCT s FROM Shift s " +
                        "LEFT JOIN FETCH s.staff " +
                        "LEFT JOIN FETCH s.closedBy " +
                        "WHERE s.startTime >= :from AND s.startTime <= :to " +
                        "ORDER BY s.startTime DESC",
                    Shift.class)
                .setParameter("from", from)
                .setParameter("to", to)
                .list();
        } finally {
            if (session != null && session.isOpen()) session.close();
        }
    }

    // ===== Business-layer methods are not supported by DAO_Shift =====

    @Override
    public Shift getCurrentOpenShiftForStaff(Staff staff) {
        throw new UnsupportedOperationException("DAO_Shift does not support business operations");
    }

    @Override
    public Shift getOpenShiftOnWorkstation(String workstation) {
        throw new UnsupportedOperationException("DAO_Shift does not support business operations");
    }

    @Override
    public String getCurrentWorkstation() {
        throw new UnsupportedOperationException("DAO_Shift does not support business operations");
    }

    @Override
    public Shift openShift(Staff staff, BigDecimal startCash, String notes) {
        throw new UnsupportedOperationException("DAO_Shift does not support business operations");
    }

    @Override
    public Shift closeShift(Shift shift, BigDecimal endCash, String notes, Staff closingStaff) {
        throw new UnsupportedOperationException("DAO_Shift does not support business operations");
    }

    @Override
    public Shift closeShift(Shift shift, BigDecimal endCash, String notes, Staff closingStaff, String closeReason) {
        throw new UnsupportedOperationException("DAO_Shift does not support business operations");
    }

    @Override
    public BigDecimal calculateSystemCashForShift(Shift shift) {
        throw new UnsupportedOperationException("DAO_Shift does not support business operations");
    }
}
