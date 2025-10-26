package com.interfaces;

import com.entities.Staff;

import java.util.List;


/**
 * @author Tô Thanh Hậu
 */
public interface IStaff {

    public boolean addStaff(Staff s);
    public boolean updateStaff(Staff s);
    public List<Staff> getAllStaffs();
    public Staff getStaffById(String id);
    public Staff getStaffByUsername(String username);
    public Staff getStaffByEmail(String email);
    public boolean existsByUsername(String username);
    public boolean existsByEmail(String email);
    public boolean existsByPhoneNumber(String phoneNumber);
    public boolean existsByLicenseNumber(String licenseNumber);
    public boolean existsByUsernameExcludingId(String username, String excludeId);
    public boolean existsByEmailExcludingId(String email, String excludeId);
    public boolean existsByPhoneNumberExcludingId(String phoneNumber, String excludeId);
    public boolean existsByLicenseNumberExcludingId(String licenseNumber, String excludeId);
}
