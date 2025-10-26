package com.entities;

import com.enums.Role;
import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDate;

/**
 * @author Bùi Quốc Trụ, Tô Thanh Hậu
 */
@Entity
@Table(name = "Staff")
public class Staff {
    @Id
    @UuidGenerator
    @Column(name = "id",insertable = false, updatable = false, nullable = false, length = 50)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 50)
    private Role role;

    @Column(name = "username", nullable = false, unique = true)
    private String username;

    @Column(name="password", nullable = false)
    private String password;

    @Column(name = "fullName", nullable = false)
    private String fullName;

    @Column(name = "licenseNumber", length = 100)
    private String licenseNumber;

    @Column(name = "phoneNumber", length = 20)
    private String phoneNumber;

    @Column(name = "email")
    private String email;

    @Column(name = "hireDate", nullable = false)
    private LocalDate hireDate;

    @Column(name = "isActive", nullable = false)
    private boolean isActive;

    public Staff() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {

        if (role == null) {
            this.role = Role.PHARMACIST; // Default role
        } else {
            this.role = role;
        }
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {

        if (username == null || username.isEmpty()) {
            throw new IllegalArgumentException("Tên đăng nhập không được để trống");
        }

        if (username.length() < 5 || username.length() > 20) {
            throw new IllegalArgumentException("Tên đăng nhập phải từ 5 đến 20 ký tự");
        }

        if (!username.matches("^[a-zA-Z0-9._-]+$")) {
            throw new IllegalArgumentException("Tên đăng nhập chỉ được chứa chữ cái, số và các ký tự đặc biệt như . _ -");
        }

        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {

        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Mật khẩu không được để trống");
        }

        if (password.length() < 6) {
            throw new IllegalArgumentException("Mật khẩu phải có ít nhất 6 ký tự");
        }
        this.password = password;
    }

    public String getFullName() {
        return fullName;
    }


    public void setFullName(String fullName) {

        if (fullName == null || fullName.isEmpty()) {
            throw new IllegalArgumentException("Họ và tên không được để trống");
        }

        if (!fullName.matches("^([A-ZÀ-ỹ][a-zà-ỹA-ZÀ-ỹ]*)(\\s[A-ZÀ-Ỹ][a-zà-ỹA-ZÀ-Ỹ]*)*$")) {
            throw new IllegalArgumentException("Họ và tên phải có chữ cái đầu viết hoa và không chứa số hoặc ký tự đặc biệt");
        }
        this.fullName = fullName;

    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public void setLicenseNumber(String licenseNumber) {
        this.licenseNumber = licenseNumber;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }
    
    public void setPhoneNumber(String phoneNumber) {

        if (!phoneNumber.matches("^(0[9|3|7|8|5|2])+([0-9]{8})$")) {
            throw new IllegalArgumentException("Số điện thoại không hợp lệ");
        }
        this.phoneNumber = phoneNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {

        if (!email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")) {
            throw new IllegalArgumentException("Email không hợp lệ");
        }
        this.email = email;
    }

    public LocalDate getHireDate() {
        return hireDate;
    }

    public void setHireDate(LocalDate hireDate) {

        if (hireDate == null){
            this.hireDate = LocalDate.now();
            return;
        }


        this.hireDate = hireDate;
    }


    public boolean isActive() {
        return isActive;
    }
    public void setActive(boolean isActive) {
        this.isActive = isActive;
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
