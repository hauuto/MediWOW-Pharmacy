package com.bus;

import com.dao.DAO_Staff;
import com.entities.Staff;
import com.interfaces.IStaff;
import com.utils.EmailUltil;
import com.utils.PasswordUtil;
import io.github.cdimascio.dotenv.Dotenv;

import java.util.List;


/**
 * @author Tô Thanh Hậu
 */

public class BUS_Staff implements IStaff {
    private final DAO_Staff DAOStaff;
    private final EmailUltil emailUltil;

    public BUS_Staff() {
        Dotenv dotenv = Dotenv.load();

        this.DAOStaff = new DAO_Staff();
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

        boolean created = DAOStaff.addStaff(s);
        Staff staff = getStaffByEmail(s.getEmail());


        if (created) {
            String email = s.getEmail();
            if (email != null && !email.trim().isEmpty()) {
                emailUltil.sendPasswordEmail(email,staff.getFullName(),staff.getUsername(),password);

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
        Staff existingStaff = DAOStaff.getStaffById(s.getId());
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

        return DAOStaff.updateStaff(s);
    }

    @Override
    public List<Staff> getAllStaffs() {
        return DAOStaff.getAllStaffs();
    }

    @Override
    public Staff getStaffById(String id) {
        return DAOStaff.getStaffById(id);
    }

    @Override
    public Staff getStaffByUsername(String username) {
        return DAOStaff.getStaffByUsername(username);
    }

    @Override
    public Staff getStaffByEmail(String email) {
        return DAOStaff.getStaffByEmail(email);
    }

    public Staff login(String username, String password) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Tên đăng nhập không được để trống");
        }

        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Mật khẩu không được để tr��ng");
        }

        Staff staff = getStaffByUsername(username);

        if (staff == null) {
            throw new IllegalArgumentException("Tên đăng nhập hoặc mật khẩu không đúng");
        }

        if (!staff.isActive()) {
            throw new IllegalArgumentException("Tài khoản đã bị vô hiệu hóa. Vui lòng liên hệ quản trị viên");
        }

        if (!PasswordUtil.verifyPassword(password, staff.getPassword())) {
            throw new IllegalArgumentException("Tên đăng nhập hoặc mật khẩu không đúng");
        }

        return staff;
    }

    @Override
    public boolean existsByUsername(String username) {
        return DAOStaff.existsByUsername(username);
    }

    @Override
    public boolean existsByEmail(String email) {
        return DAOStaff.existsByEmail(email);
    }

    @Override
    public boolean existsByPhoneNumber(String phoneNumber) {
        return DAOStaff.existsByPhoneNumber(phoneNumber);
    }

    @Override
    public boolean existsByLicenseNumber(String licenseNumber) {
        return DAOStaff.existsByLicenseNumber(licenseNumber);
    }

    @Override
    public boolean existsByUsernameExcludingId(String username, String excludeId) {
        return DAOStaff.existsByUsernameExcludingId(username, excludeId);
    }

    @Override
    public boolean existsByEmailExcludingId(String email, String excludeId) {
        return DAOStaff.existsByEmailExcludingId(email, excludeId);
    }

    @Override
    public boolean existsByPhoneNumberExcludingId(String phoneNumber, String excludeId) {
        return DAOStaff.existsByPhoneNumberExcludingId(phoneNumber, excludeId);
    }

    @Override
    public boolean existsByLicenseNumberExcludingId(String licenseNumber, String excludeId) {
        return DAOStaff.existsByLicenseNumberExcludingId(licenseNumber, excludeId);
    }

    @Override
    public boolean isFirstLogin(Staff staff) {
        return DAOStaff.isFirstLogin(staff);
    }

    @Override
    public boolean isMustChangePassword(Staff staff) {
        return DAOStaff.isMustChangePassword(staff);
    }

    @Override
    public boolean updateChangePasswordFlag(Staff staff, boolean flag) {
        return DAOStaff.updateChangePasswordFlag(staff, flag);
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


        boolean hasPhoneNumber = s.getPhoneNumber() != null && !s.getPhoneNumber().trim().isEmpty();
        boolean hasEmail = s.getEmail() != null && !s.getEmail().trim().isEmpty();

        if (!hasPhoneNumber && !hasEmail){
            throw new IllegalArgumentException("Số điện thoại hoặc email phải được cung cấp");
        }

        if (s.getLicenseNumber().trim().isEmpty()){
            throw new IllegalArgumentException("Dược sĩ cần yêu cầu có mã số chứng chỉ hành nghề");
        }
    }


    public boolean changePassword(Staff staff, String oldPassword, String newPassword) {
        // Validate inputs
        if (staff == null || staff.getId() == null || staff.getId().trim().isEmpty()) {
            throw new IllegalArgumentException("Không tìm thấy thông tin nhân viên");
        }

        if (oldPassword == null || oldPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập mật khẩu cũ");
        }

        if (newPassword == null || newPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập mật khẩu mới");
        }

        if (newPassword.length() < 6) {
            throw new IllegalArgumentException("Mật khẩu mới phải có ít nhất 6 ký tự");
        }

        // Verify old password
        if (!PasswordUtil.verifyPassword(oldPassword, staff.getPassword())) {
            throw new IllegalArgumentException("Mật khẩu cũ không chính xác");
        }

        // Check if new password is same as old password
        if (oldPassword.equals(newPassword)) {
            throw new IllegalArgumentException("Mật khẩu mới không được trùng với mật khẩu cũ");
        }

        // Hash new password
        String hashedPassword = PasswordUtil.hashPassword(newPassword);
        staff.setPassword(hashedPassword);

        // Update in database
        boolean success = DAOStaff.updateStaff(staff);
        
        if (!success) {
            throw new RuntimeException("Đổi mật khẩu thất bại! Vui lòng thử lại");
        }

        return true;
    }

    /**
     * Reset password with full verification (username, email, and phone number)
     * All three fields must match the staff record
     * @param username Username of staff
     * @param email Email of staff
     * @param phoneNumber Phone number of staff
     * @return true if password reset successfully
     */
    public boolean resetPasswordWithVerification(String username, String email, String phoneNumber) {
        // Validate inputs
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập tên đăng nhập");
        }

        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập email");
        }

        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập số điện thoại");
        }

        // Validate email format
        if (!email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")) {
            throw new IllegalArgumentException("Email không hợp lệ");
        }

        // Validate phone number format
        if (!phoneNumber.matches("^(0[9|3|7|8|5|2])+([0-9]{8})$")) {
            throw new IllegalArgumentException("Số điện thoại không hợp lệ");
        }

        // Find staff by username
        Staff staff = getStaffByUsername(username);
        if (staff == null) {
            throw new IllegalArgumentException("Tên đăng nhập không chính xác. Vui lòng kiểm tra lại.");
        }

        // Verify all fields match
        if (staff.getEmail() == null || !staff.getEmail().equalsIgnoreCase(email)) {
            throw new IllegalArgumentException("Email không chính xác. Vui lòng kiểm tra lại.");
        }

        if (staff.getPhoneNumber() == null || !staff.getPhoneNumber().equals(phoneNumber)) {
            throw new IllegalArgumentException("Số điện thoại không chính xác. Vui lòng kiểm tra lại.");
        }

        // Check if staff is active
        if (!staff.isActive()) {
            throw new IllegalArgumentException("Tài khoản đã bị vô hiệu hóa. Vui lòng liên hệ quản trị viên");
        }

        // Generate new password
        String newPassword = PasswordUtil.generatePassword();
        String hashedPassword = PasswordUtil.hashPassword(newPassword);

        // Update password in database
        staff.setPassword(hashedPassword);
        boolean updated = DAOStaff.updateStaff(staff) && DAOStaff.updateChangePasswordFlag(staff, true);

        if (updated) {
            // Send new password to email
            try {
                emailUltil.sendPasswordEmail(
                    staff.getEmail(),
                    staff.getFullName(),
                    staff.getUsername(),
                    newPassword
                );
                return true;
            } catch (Exception e) {
                throw new RuntimeException("Đã đặt lại mật khẩu nhưng không thể gửi email. Vui lòng liên hệ quản trị viên");
            }
        }

        return false;
    }




}
