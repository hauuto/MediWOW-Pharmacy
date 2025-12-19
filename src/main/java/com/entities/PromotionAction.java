package com.entities;

import com.enums.PromotionEnum.*;
import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;

@Entity
@Table(name = "PromotionAction")
public class PromotionAction {

    @Id
    @UuidGenerator
    @Column(name = "id", insertable = false, updatable = false, nullable = false, length = 50)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promotion", nullable = false)
    private Promotion promotion;

    @Column(name = "actionOrder", nullable = false)
    private int actionOrder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Target target;

    @Column(name = "value", precision = 18, scale = 2)
    private BigDecimal value;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "product", referencedColumnName = "product", insertable = true, updatable = true),
            @JoinColumn(name = "unitOfMeasure", referencedColumnName = "name", insertable = true, updatable = true)
    })
    private UnitOfMeasure productUOM;

    // ======================
    // Constructors
    // ======================
    public PromotionAction() {}

    public PromotionAction(
            ActionType type,
            Target target,
            BigDecimal value,
            UnitOfMeasure productUOM,
            int actionOrder
    ) {
        this.type = type;
        this.target = target;
        this.value = value;
        this.productUOM = productUOM;
        this.actionOrder = actionOrder;
    }

    // ======================
    // Getters / Setters
    // ======================
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Promotion getPromotion() { return promotion; }
    public void setPromotion(Promotion promotion) { this.promotion = promotion; }

    public int getActionOrder() { return actionOrder; }
    public void setActionOrder(int actionOrder) { this.actionOrder = actionOrder; }

    public ActionType getType() { return type; }
    public void setType(ActionType type) { this.type = type; }

    public Target getTarget() { return target; }
    public void setTarget(Target target) { this.target = target; }

    public BigDecimal getValue() { return value; }
    public void setValue(BigDecimal value) { this.value = value; }

    public UnitOfMeasure getProductUOM() { return productUOM; }
    public void setProductUOM(UnitOfMeasure productUOM) { this.productUOM = productUOM; }

    // ======================
    // Compatibility Helpers
    // ======================
    public Product getProduct() {
        return (productUOM != null) ? productUOM.getProduct() : null;
    }

    public double getPrimaryValue() {
        return (value == null) ? 0.0 : value.doubleValue();
    }
}
