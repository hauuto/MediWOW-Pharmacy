package com.interfaces;

import com.entities.Staff;

import java.util.List;


/**
 * @author Tô Thanh Hậu
 */
public interface IStaff {

    public boolean addStaff(Staff s);
    public List<Staff> getAllStaffs();
    public boolean existsByUsername(String username);
    public boolean existsByEmail(String email);
    public boolean existsByPhoneNumber(String phoneNumber);
    public boolean existsByLicenseNumber(String licenseNumber);
}
