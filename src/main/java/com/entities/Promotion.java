package com.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Promotion")
public class Promotion {

    @Id
    @UuidGenerator
    @Column(name = "id",insertable = false, updatable = false, nullable = false, length = 50)
    private String id;

    @Column(nullable = false, name = "name")
    private String name;

    @Column(length = 300, name = "description")
    private String description;

    @Column(name = "effectiveDate")
    private LocalDate effectiveDate;

    @Column(name = "endDate")
    private LocalDate endDate;

    @Column(name = "isActive")
    private boolean isActive;

    @OneToMany(mappedBy = "promotion",
            cascade = {},
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    private List<PromotionCondition> conditions = new ArrayList<>();

    @OneToMany(mappedBy = "promotion",
            cascade = {},
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    @OrderBy("actionOrder ASC")
    private List<PromotionAction> actions = new ArrayList<>();

    // ======================
    // Constructors
    // ======================
    public Promotion() {}

    public Promotion(String name, LocalDate start, LocalDate end, boolean active, String desc) {
        this.name = name;
        this.description = desc;
        this.effectiveDate = start;
        this.endDate = end;
        this.isActive = active;
    }

    // ======================
    // Getters / Setters
    // ======================
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDate getEffectiveDate() { return effectiveDate; }
    public void setEffectiveDate(LocalDate effectiveDate) { this.effectiveDate = effectiveDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public boolean getIsActive() { return isActive; }
    public void setIsActive(boolean active) { this.isActive = active; }

    public List<PromotionCondition> getConditions() { return conditions; }
    public void setConditions(List<PromotionCondition> conditions) { this.conditions = conditions; }

    public List<PromotionAction> getActions() { return actions; }
    public void setActions(List<PromotionAction> actions) { this.actions = actions; }

    // ======================
    // Convenience helpers for bidirectional management
    // ======================
    public void addCondition(PromotionCondition condition) {
        if (condition == null) return;
        condition.setPromotion(this);
        this.conditions.add(condition);
    }

    public void removeCondition(PromotionCondition condition) {
        if (condition == null) return;
        this.conditions.remove(condition);
        condition.setPromotion(null);
    }

    public void addAction(PromotionAction action) {
        if (action == null) return;
        action.setPromotion(this);
        this.actions.add(action);
    }

    public void removeAction(PromotionAction action) {
        if (action == null) return;
        this.actions.remove(action);
        action.setPromotion(null);
    }

}
