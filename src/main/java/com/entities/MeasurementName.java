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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id; // Use Integer for nullable before insert

    @Column(name = "name", length = 100, unique = true, nullable = false)
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

    public Integer getId() {return id;}
    public void setId(Integer id) {this.id = id;}

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }



    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MeasurementName other)) return false;

        if (this.id == null || other.id == null) {
            return false;
        }
        return Objects.equals(this.id, other.id);
    }

    @Override
    public String toString() {
        return "MeasurementName{" +
                "name='" + name + '\'' +
                '}';
    }
}

