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
    public boolean updateStaff(Staff s) {
        if (s == null || s.getId() == null || s.getId().trim().isEmpty()) {
            throw new IllegalArgumentException("Không tìm thấy thông tin nhân viên cần cập nhật");
        }

        // Kiểm tra nhân viên có tồn tại không
        Staff existingStaff = staffDAO.getStaffById(s.getId());
        if (existingStaff == null) {
            throw new IllegalArgumentException("Nhân viên không tồn tại trong hệ thống");
        }

        // Validate thông tin
        validateStaff(s);

        // Kiểm tra trùng lặp (loại trừ chính nhân viên đang cập nhật)
        checkDuplicatesForUpdate(s);

        // Giữ nguyên mật khẩu cũ nếu không thay đổi
        if (s.getPassword() == null || s.getPassword().trim().isEmpty()) {
            s.setPassword(existingStaff.getPassword());
        }

        return staffDAO.updateStaff(s);
    }

    @Override
    public List<Staff> getAllStaffs() {
        return staffDAO.getAllStaffs();
    }

    @Override
    public Staff getStaffById(String id) {
        return staffDAO.getStaffById(id);
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

    @Override
    public boolean existsByUsernameExcludingId(String username, String excludeId) {
        return staffDAO.existsByUsernameExcludingId(username, excludeId);
    }

    @Override
    public boolean existsByEmailExcludingId(String email, String excludeId) {
        return staffDAO.existsByEmailExcludingId(email, excludeId);
    }

    @Override
    public boolean existsByPhoneNumberExcludingId(String phoneNumber, String excludeId) {
        return staffDAO.existsByPhoneNumberExcludingId(phoneNumber, excludeId);
    }

    @Override
    public boolean existsByLicenseNumberExcludingId(String licenseNumber, String excludeId) {
        return staffDAO.existsByLicenseNumberExcludingId(licenseNumber, excludeId);
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

    private void checkDuplicatesForUpdate(Staff s) {
        // Check duplicate username (loại trừ chính nhân viên đang cập nhật)
        if (existsByUsernameExcludingId(s.getUsername(), s.getId())) {
            throw new IllegalArgumentException("Tên đăng nhập '" + s.getUsername() + "' đã tồn tại trong hệ thống");
        }

        // Check duplicate email
        if (s.getEmail() != null && !s.getEmail().trim().isEmpty()) {
            if (existsByEmailExcludingId(s.getEmail(), s.getId())) {
                throw new IllegalArgumentException("Email '" + s.getEmail() + "' đã được sử dụng bởi nhân viên khác");
            }
        }

        // Check duplicate phone number
        if (s.getPhoneNumber() != null && !s.getPhoneNumber().trim().isEmpty()) {
            if (existsByPhoneNumberExcludingId(s.getPhoneNumber(), s.getId())) {
                throw new IllegalArgumentException("Số điện thoại '" + s.getPhoneNumber() + "' đã được sử dụng bởi nhân viên khác");
            }
        }

        // Check duplicate license number
        if (s.getLicenseNumber() != null && !s.getLicenseNumber().trim().isEmpty()) {
            if (existsByLicenseNumberExcludingId(s.getLicenseNumber(), s.getId())) {
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
