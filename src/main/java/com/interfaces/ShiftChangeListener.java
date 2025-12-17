package com.interfaces;

import com.entities.Shift;

/**
 * Interface listener các thay đổi về ca làm việc
 * @author Tô Thanh Hậu
 */
public interface ShiftChangeListener {
    /**
     * Được gọi khi một ca mới được mở
     * @param shift Ca làm việc mới được mở
     */
    void onShiftOpened(Shift shift);

    /**
     * Được gọi khi một ca được đóng
     * @param shift Ca làm việc được đóng
     */
    void onShiftClosed(Shift shift);
}

