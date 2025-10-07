package com.entities;

import com.enums.PromotionEnum;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import com.enums.PromotionEnum.*;

public class Promotion {
    private String id, name, description;
    private LocalDate start, end;
    private PromotionEnum.Status status;
    private final List<PromotionCondition> conditions = new ArrayList<>();
    private final List<PromotionAction> actions = new ArrayList<>();

    public Promotion(String id, String name, LocalDate s, LocalDate e, PromotionEnum.Status st, String desc){
        this.id=id; this.name=name; start=s; end=e; status=st; description=desc;
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

    public LocalDate getStart() {
        return start;
    }

    public void setStart(LocalDate start) {
        this.start = start;
    }

    public LocalDate getEnd() {
        return end;
    }

    public void setEnd(LocalDate end) {
        this.end = end;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public List<PromotionCondition> getConditions() {
        return conditions;
    }

    public List<PromotionAction> getActions() {
        return actions;
    }
}