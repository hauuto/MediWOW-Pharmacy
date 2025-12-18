package com.bus;

import com.dao.DAO_Shift;
import com.entities.Shift;
import com.entities.Staff;
import com.enums.Role;

import java.math.BigDecimal;
import java.net.InetAddress;

public class BUS_Shift {
    private final DAO_Shift daoShift;

    public BUS_Shift() {
        this.daoShift = new DAO_Shift();
    }

    public Shift getCurrentOpenShiftForStaff(Staff staff) {
        if (staff == null) return null;
        return daoShift.getOpenShiftByStaffId(staff.getId());
    }

    /**
     * Get any open shift on current workstation
     */
    public Shift getOpenShiftOnWorkstation(String workstation) {
        if (workstation == null || workstation.isBlank()) return null;
        return daoShift.getOpenShiftByWorkstation(workstation);
    }

    /**
     * Get current workstation identifier
     */
    public String getCurrentWorkstation() {
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            return localHost.getHostName();
        } catch (Exception e) {
            return "UNKNOWN_WORKSTATION";
        }
    }

    public Shift openShift(Staff staff, BigDecimal startCash, String notes) {
        return openShift(staff, startCash, notes, getCurrentWorkstation());
    }

    public Shift openShift(Staff staff, BigDecimal startCash, String notes, String workstation) {
        if (staff == null) {
            throw new IllegalArgumentException("Không tìm thấy thông tin nhân viên hiện tại");
        }
        if (startCash == null || startCash.signum() < 0) {
            throw new IllegalArgumentException("Tiền đầu ca phải lớn hơn hoặc bằng 0");
        }

        // Debug logging
        System.out.println("=== DEBUG: BUS_Shift.openShift ===");
        System.out.println("Workstation: " + workstation);
        System.out.println("Staff: " + staff.getFullName() + " (" + staff.getId() + ")");

        // Check if there's already an open shift on this workstation
        Shift existingShift = getOpenShiftOnWorkstation(workstation);
        System.out.println("Existing shift found: " + (existingShift != null));
        if (existingShift != null) {
            System.out.println("Existing shift ID: " + existingShift.getId());
            System.out.println("Existing shift staff: " + existingShift.getStaff().getFullName());
            System.out.println("Existing shift workstation: " + existingShift.getWorkstation());

            throw new IllegalStateException("Máy này đang có ca mở bởi nhân viên: " +
                existingShift.getStaff().getFullName());
        }

        System.out.println("No existing shift found - opening new shift");
        Shift newShift = daoShift.openShift(staff, startCash, notes, workstation);
        System.out.println("New shift created with ID: " + newShift.getId());
        System.out.println("=== END DEBUG ===");

        return newShift;
    }

    public Shift closeShift(Shift shift, BigDecimal endCash, String notes, Staff closingStaff) {
        return closeShift(shift, endCash, notes, closingStaff, null);
    }

    public Shift closeShift(Shift shift, BigDecimal endCash, String notes, Staff closingStaff, String closeReason) {
        if (shift == null) {
            throw new IllegalArgumentException("Không tìm thấy thông tin ca làm việc");
        }
        if (endCash == null || endCash.signum() < 0) {
            throw new IllegalArgumentException("Tiền cuối ca phải lớn hơn hoặc bằng 0");
        }

        // Check permissions: shift owner, manager, or staff on same workstation can close
        boolean isShiftOwner = closingStaff != null &&
            shift.getStaff() != null &&
            closingStaff.getId().equals(shift.getStaff().getId());
        boolean isManager = closingStaff != null && closingStaff.getRole() == Role.MANAGER;
        boolean isSameWorkstation = shift.getWorkstation() != null &&
            shift.getWorkstation().equals(getCurrentWorkstation());

        if (!isShiftOwner && !isManager && !isSameWorkstation) {
            throw new IllegalArgumentException("Bạn không có quyền đóng ca này. " +
                "Chỉ người mở ca, Quản lý hoặc nhân viên trên cùng máy mới có thể đóng ca.");
        }

        // If not shift owner, require close reason
        if (!isShiftOwner && (closeReason == null || closeReason.trim().isEmpty())) {
            throw new IllegalArgumentException("Vui lòng nhập lý do đóng ca");
        }

        BigDecimal systemCash = daoShift.calculateSystemCashForShift(shift.getId());
        return daoShift.closeShift(shift.getId(), endCash, systemCash, notes, closingStaff, closeReason);
    }

    public BigDecimal calculateSystemCashForShift(Shift shift) {
        if (shift == null) return BigDecimal.ZERO;
        return daoShift.calculateSystemCashForShift(shift.getId());
    }
}

