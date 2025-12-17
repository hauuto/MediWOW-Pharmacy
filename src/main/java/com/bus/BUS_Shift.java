package com.bus;

import com.dao.DAO_Shift;
import com.entities.Shift;
import com.entities.Staff;

import java.math.BigDecimal;

public class BUS_Shift {
    private final DAO_Shift daoShift;

    public BUS_Shift() {
        this.daoShift = new DAO_Shift();
    }

    public Shift getCurrentOpenShiftForStaff(Staff staff) {
        if (staff == null) return null;
        return daoShift.getOpenShiftByStaffId(staff.getId());
    }

    public Shift openShift(Staff staff, BigDecimal startCash, String notes) {
        if (staff == null) {
            throw new IllegalArgumentException("Không tìm thấy thông tin nhân viên hiện tại");
        }
        if (startCash == null || startCash.signum() < 0) {
            throw new IllegalArgumentException("Tiền đầu ca phải lớn hơn hoặc bằng 0");
        }
        return daoShift.openShift(staff, startCash, notes);
    }
}

