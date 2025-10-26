package com.entities;

import com.enums.PromotionEnum;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.enums.PromotionEnum.*;
import jakarta.persistence.*;
/*
@author Nguyễn Thanh Khôi
 */

@Entity
@Table(name = "Promotion")
public class Promotion {

    @Id
    @Column(length = 50, name="id")
    private String id;

    @Column(nullable = false, name="name")
    private String name;

    @Column(length = 300, name="description")
    private String description;


    @Column(name = "effectiveDate")
    private LocalDate effectiveDate;

    @Column(name = "endDate")
    private LocalDate endDate;

    @Column(name = "isActive")
    private boolean isActive;


    @OneToMany(mappedBy = "promotion", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<PromotionCondition> conditions = new HashSet<>();

    @OneToMany(mappedBy = "promotion", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<PromotionAction> actions = new HashSet<>();


    public Promotion() {}
    public Promotion(String id, String name, LocalDate s, LocalDate e, boolean st, String desc){
        this.id=id; this.name=name; effectiveDate=s; endDate=e; isActive=st; description=desc;
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
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(LocalDate effectiveDate) {
        this.effectiveDate = effectiveDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate end) {
        this.endDate = end;
    }

    public boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(boolean isActive) {
        this.isActive = isActive;
    }

    public Set<PromotionCondition> getConditions() {
        return conditions;
    }

    public Set<PromotionAction> getActions() {
        return actions;
    }

    public void setConditions(Set<PromotionCondition> conditions) {
        this.conditions = conditions;
    }

    public void setActions(Set<PromotionAction> actions) {
        this.actions = actions;
    }
}