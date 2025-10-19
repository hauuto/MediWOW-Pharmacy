package com.entities;

import java.time.LocalDateTime;

/**
 * @author Bùi Quốc Trụ
 */
public class PrescribedCustomer {
    private final String id;
    private String name;
    private String phoneNumber;
    private String address;
    private LocalDateTime creationDate;

    public PrescribedCustomer(String id, String name, String phoneNumber, String address, LocalDateTime creationDate) {
        this.id = id;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.address = address;
        this.creationDate = creationDate;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getAddress() {
        return address;
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
