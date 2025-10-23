package com.bus;

import com.dao.StaffDAO;
import com.entities.Staff;
import com.interfaces.IStaff;
import com.utils.PasswordUtil;


/**
 * @author Tô Thanh Hậu
 */

public class StaffBUS implements IStaff {
    private final StaffDAO staffDAO;

    public StaffBUS() {
        this.staffDAO = new StaffDAO();
    }

    @Override
    public boolean addStaff(Staff s) {
        validateStaff(s);

        String password = PasswordUtil.generatePassword();
        String hashedPassword = PasswordUtil.hashPassword(password);
        s.setPassword(hashedPassword);

        return staffDAO.addStaff(s);
    }



    public void validateStaff(Staff s){
        if (s == null){
            throw new IllegalArgumentException("Không có thông tin nào được thêm vào");
        }
        if (s.getFullName() == null || s.getFullName().trim().isEmpty()){
            throw new IllegalArgumentException("Họ và tên không được để trống");
        }

        if (s.getUsername() == null || s.getUsername().trim().isEmpty()){
            throw new IllegalArgumentException("Tên đăng nhập không được để trống");
        }

        boolean hasPhoneNumber = s.getPhoneNumber() != null && !s.getPhoneNumber().trim().isEmpty();
        boolean hasEmail = s.getEmail() != null && !s.getEmail().trim().isEmpty();

        if (!hasPhoneNumber && !hasEmail){
            throw new IllegalArgumentException("Số điện thoại hoặc email phải được cung cấp");
        }

        if (s.getLicenseNumber().trim().isEmpty()){
            throw new IllegalArgumentException("Dược sĩ cần yêu cầu có mã số chứng chỉ hành nghề");
        }
    }


}
