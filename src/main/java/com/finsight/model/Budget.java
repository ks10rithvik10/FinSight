package com.finsight.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(
    name = "budgets",
    uniqueConstraints = @UniqueConstraint(columnNames = { "user_id", "category", "budget_month", "budget_year" })
)
public class Budget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private AppUser user;

    private String category;

    @Column(name = "budget_month", nullable = false)
    private int month;

    @Column(name = "budget_year", nullable = false)
    private int year;

    @Column(precision = 14, scale = 2)
    private BigDecimal limitAmount;

    protected Budget() {
    }

    public Budget(AppUser user, String category, int month, int year, BigDecimal limitAmount) {
        this.user = user;
        this.category = category;
        this.month = month;
        this.year = year;
        this.limitAmount = limitAmount;
    }

    public Long getId() { return id; }
    public AppUser getUser() { return user; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public int getMonth() { return month; }
    public void setMonth(int month) { this.month = month; }
    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }
    public BigDecimal getLimitAmount() { return limitAmount; }
    public void setLimitAmount(BigDecimal limitAmount) { this.limitAmount = limitAmount; }
}
