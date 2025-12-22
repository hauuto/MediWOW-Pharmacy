package com.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * @author Migration to new schema
 * Entity for Customer table
 */
@Entity
@Table(name = "Customer")
public class Customer {
    @Id
    @Column(name = "id", updatable = false, nullable = false, length = 50)
    private String id;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "phoneNumber", length = 20)
    private String phoneNumber;

    @Column(name = "address", length = 500)
    private String address;

    @CreationTimestamp
    @Column(name = "creationDate", updatable = false, nullable = false)
    private LocalDateTime creationDate;

    protected Customer() {}

    /**
     * Constructor for creating customer with phone number only
     * Name will be set to "Khách hàng" by default
     * @param phoneNumber Customer phone number (required)
     * @param isPhoneNumber Flag to distinguish from name-only constructor (must be true)
     */
    public Customer(String phoneNumber, boolean isPhoneNumber) {
        if (isPhoneNumber) {
            if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
                throw new IllegalArgumentException("Số điện thoại không được để trống");
            }
            this.phoneNumber = phoneNumber.trim();
            this.name = "Khách hàng"; // Default name
            this.address = null;
        } else {
            // Treat as name constructor
            if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
                throw new IllegalArgumentException("Tên khách hàng không được để trống");
            }
            this.name = phoneNumber.trim();
            this.phoneNumber = null;
            this.address = null;
        }
    }

    public Customer(String id, String name, String phoneNumber, String address) {
        this.id = id;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.address = address;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Tên khách hàng không được để trống");
        }
        this.name = name.trim();
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = (phoneNumber == null) ? null : phoneNumber.trim();
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = (address == null) ? null : address.trim();
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;

        if (o == null || getClass() != o.getClass())
            return false;

        Customer other = (Customer) o;
        return Objects.equals(id, other.id);
    }

    @Override
    public String toString() {
        return "Customer{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                '}';
    }
}
