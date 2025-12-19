package com.interfaces;

import com.entities.Shift;
import com.entities.Staff;

import java.math.BigDecimal;

/**
 * Single Shift contract used across the project.
 * Implemented by both BUS_Shift (business layer) and DAO_Shift (data layer).
 */
public interface IShift {

    // ===== Common/query operations =====
    Shift getCurrentOpenShiftForStaff(Staff staff);

    /**
     * Get any open shift on a specific workstation.
     */
    Shift getOpenShiftOnWorkstation(String workstation);

    /**
     * Get current workstation identifier.
     */
    String getCurrentWorkstation();

    // ===== Business operations =====
    Shift openShift(Staff staff, BigDecimal startCash, String notes);

    Shift openShift(Staff staff, BigDecimal startCash, String notes, String workstation);

    Shift closeShift(Shift shift, BigDecimal endCash, String notes, Staff closingStaff);

    Shift closeShift(Shift shift, BigDecimal endCash, String notes, Staff closingStaff, String closeReason);

    BigDecimal calculateSystemCashForShift(Shift shift);

    // ===== DAO-level operations (needed by BUS_Shift) =====
    Shift getOpenShiftByStaffId(String staffId);

    Shift getOpenShiftByWorkstation(String workstation);

    Shift closeShift(String shiftId,
                    BigDecimal endCash,
                    BigDecimal systemCash,
                    String notes,
                    Staff closedBy,
                    String closeReason);

    BigDecimal calculateSystemCashForShift(String shiftId);
}
