package com.entities;

import jakarta.persistence.*;
import java.util.Objects;

/**
 * @author Migration to new schema
 * Dictionary table for measurement names
 */
@Entity
@Table(name = "MeasurementName")
public class MeasurementName {
    @Id
    @Column(name = "name", length = 100)
    private String name;

    protected MeasurementName() {}

    public MeasurementName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;

        if (o == null || getClass() != o.getClass())
            return false;

        MeasurementName other = (MeasurementName) o;
        return Objects.equals(name, other.name);
    }

    @Override
    public String toString() {
        return "MeasurementName{" +
                "name='" + name + '\'' +
                '}';
    }
}

