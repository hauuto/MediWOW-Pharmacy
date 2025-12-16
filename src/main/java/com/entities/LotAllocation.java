package com.entities;

import jakarta.persistence.*;
import java.util.Objects;

/**
 * @author Migration to new schema
 * LotAllocation tracks which lots are used for each invoice line
 */
@Entity
@Table(name = "LotAllocation")
public class LotAllocation {
    @Id
    @Column(name = "id", length = 50)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoiceLine", nullable = false)
    private InvoiceLine invoiceLine;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lot", nullable = false)
    private Lot lot;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    protected LotAllocation() {}

    public LotAllocation(String id, InvoiceLine invoiceLine, Lot lot, int quantity) {
        this.id = id;
        this.invoiceLine = invoiceLine;
        this.lot = lot;
        this.quantity = quantity;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public InvoiceLine getInvoiceLine() {
        return invoiceLine;
    }

    public void setInvoiceLine(InvoiceLine invoiceLine) {
        this.invoiceLine = invoiceLine;
    }

    public Lot getLot() {
        return lot;
    }

    public void setLot(Lot lot) {
        this.lot = lot;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
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

        LotAllocation other = (LotAllocation) o;
        return Objects.equals(id, other.id);
    }

    @Override
    public String toString() {
        return "LotAllocation{" +
                "id='" + id + '\'' +
                ", quantity=" + quantity +
                '}';
    }
}

