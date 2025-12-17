package com.entities;

import com.enums.ShiftStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "Shift")
public class Shift {
    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false, length = 50)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff", nullable = false)
    private Staff staff;

    @Column(name = "startTime", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "endTime")
    private LocalDateTime endTime;

    @Column(name = "startCash", nullable = false)
    private BigDecimal startCash; // Tiền đầu ca

    @Column(name = "endCash")
    private BigDecimal endCash;   // Tiền thực tế cuối ca

    @Column(name = "systemCash")
    private BigDecimal systemCash; // Tiền hệ thống tính toán

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ShiftStatus status;

    @Column(name = "notes", columnDefinition = "NVARCHAR(MAX)")
    private String notes;

    // Constructors
    public Shift() {}

    public Shift(Staff staff, BigDecimal startCash) {
        this.staff = staff;
        this.startCash = startCash;
        this.startTime = LocalDateTime.now();
        this.status = ShiftStatus.OPEN;
    }

    // Getters and Setters (Bạn tự generate nhé)
    public String getId() { return id; }
    public Staff getStaff() { return staff; }
    public void setStaff(Staff staff) { this.staff = staff; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public BigDecimal getStartCash() { return startCash; }
    public void setStartCash(BigDecimal startCash) { this.startCash = startCash; }
    public BigDecimal getEndCash() { return endCash; }
    public void setEndCash(BigDecimal endCash) { this.endCash = endCash; }
    public BigDecimal getSystemCash() { return systemCash; }
    public void setSystemCash(BigDecimal systemCash) { this.systemCash = systemCash; }
    public ShiftStatus getStatus() { return status; }
    public void setStatus(ShiftStatus status) { this.status = status; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}