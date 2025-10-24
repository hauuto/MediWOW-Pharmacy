package com.bus;

import com.dao.StaffDAO;
import com.entities.Staff;
import com.interfaces.IStaff;
import com.utils.EmailUltil;
import com.utils.PasswordUtil;
import io.github.cdimascio.dotenv.Dotenv;

import java.util.List;


/**
 * @author Tô Thanh Hậu
 */

public class StaffBUS implements IStaff {
    private final StaffDAO staffDAO;
    private final EmailUltil emailUltil;

    public StaffBUS() {
        Dotenv dotenv = Dotenv.load();

        this.staffDAO = new StaffDAO();
        this.emailUltil = new EmailUltil(
                dotenv.get("SMTP_HOST") != null ? dotenv.get("SMTP_HOST") : "smtp.gmail.com",
                dotenv.get("SMTP_PORT") != null ? Integer.parseInt(dotenv.get("SMTP_PORT")) : 587,
                dotenv.get("SMTP_USERNAME"),
                dotenv.get("SMTP_PASSWORD"),
                dotenv.get("SMTP_FROM") != null ? dotenv.get("SMTP_FROM") : "no-reply@yourdomain.com"
        );
    }


    @Override
    public boolean addStaff(Staff s) {
        validateStaff(s);
        checkDuplicates(s);

        String password = PasswordUtil.generatePassword();
        String hashedPassword = PasswordUtil.hashPassword(password);
        s.setPassword(hashedPassword);

        boolean created = staffDAO.addStaff(s);


        if (created) {
            String email = s.getEmail();
            if (email != null && !email.trim().isEmpty()) {
                emailUltil.sendPasswordEmail(email,s.getFullName(),s.getUsername(),password);

            }

        }

        return created;
    }

    @Override
    public List<Staff> getAllStaffs() {
        return staffDAO.getAllStaffs();
    }

    @Override
    public boolean existsByUsername(String username) {
        return staffDAO.existsByUsername(username);
    }

    @Override
    public boolean existsByEmail(String email) {
        return staffDAO.existsByEmail(email);
    }

    @Override
    public boolean existsByPhoneNumber(String phoneNumber) {
        return staffDAO.existsByPhoneNumber(phoneNumber);
    }

    @Override
    public boolean existsByLicenseNumber(String licenseNumber) {
        return staffDAO.existsByLicenseNumber(licenseNumber);
    }

    private void checkDuplicates(Staff s) {
        // Check duplicate username
        if (existsByUsername(s.getUsername())) {
            throw new IllegalArgumentException("Tên đăng nhập '" + s.getUsername() + "' đã tồn tại trong hệ thống");
        }

        // Check duplicate email
        if (s.getEmail() != null && !s.getEmail().trim().isEmpty()) {
            if (existsByEmail(s.getEmail())) {
                throw new IllegalArgumentException("Email '" + s.getEmail() + "' đã được sử dụng bởi nhân viên khác");
            }
        }

        // Check duplicate phone number
        if (s.getPhoneNumber() != null && !s.getPhoneNumber().trim().isEmpty()) {
            if (existsByPhoneNumber(s.getPhoneNumber())) {
                throw new IllegalArgumentException("Số điện thoại '" + s.getPhoneNumber() + "' đã được sử dụng bởi nhân viên khác");
            }
        }

        // Check duplicate license number
        if (s.getLicenseNumber() != null && !s.getLicenseNumber().trim().isEmpty()) {
            if (existsByLicenseNumber(s.getLicenseNumber())) {
                throw new IllegalArgumentException("Số chứng chỉ '" + s.getLicenseNumber() + "' đã được sử dụng bởi nhân viên khác");
            }
        }
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
