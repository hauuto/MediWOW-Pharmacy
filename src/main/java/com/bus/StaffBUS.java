package com.bus;

import com.dao.StaffDAO;
import com.entities.Staff;
import com.interfaces.IStaff;
import com.utils.EmailUltil;
import com.utils.PasswordUtil;
import io.github.cdimascio.dotenv.Dotenv;


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
                System.getenv().getOrDefault("SMTP_HOST","smtp.gmail.com"),
                Integer.parseInt(System.getenv().getOrDefault("SMTP_PORT","587")),
                System.getenv("SMTP_USERNAME"),
                System.getenv("SMTP_PASSWORD"),
                System.getenv().getOrDefault("SMTP_FROM","no-reply@yourdomain.com")
        );
    }

    @Override
    public boolean addStaff(Staff s) {
        validateStaff(s);

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
