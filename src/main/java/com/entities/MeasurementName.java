package com.entities;

import jakarta.persistence.*;
import java.util.Objects;

/**
 * Dictionary table for measurement names
 */
@Entity
@Table(name = "MeasurementName")
public class MeasurementName {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id; // Use Integer for nullable before insert

    @Column(name = "name", length = 100, nullable = false, unique = true)
    private String name;

    protected MeasurementName() {
        // JPA default constructor
    }

    public MeasurementName(String name) {
        this.name = name;
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public int hashCode() {
        return (id == null) ? 0 : id.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MeasurementName)) return false;
        MeasurementName other = (MeasurementName) o;

        // If both ids are null, treat as non-equal (transient entities)
        if (this.id == null || other.id == null) {
            return false;
        }
        return Objects.equals(this.id, other.id);
    }

    @Override
    public String toString() {
        return name;
    }
}
