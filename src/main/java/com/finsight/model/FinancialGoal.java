package com.finsight.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "financial_goals")
public class FinancialGoal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private AppUser user;

    private String name;

    @Column(precision = 14, scale = 2)
    private BigDecimal targetAmount;

    @Column(precision = 14, scale = 2)
    private BigDecimal currentAmount;

    private LocalDate targetDate;

    protected FinancialGoal() {
    }

    public FinancialGoal(AppUser user, String name, BigDecimal targetAmount,
                         BigDecimal currentAmount, LocalDate targetDate) {
        this.user = user;
        this.name = name;
        this.targetAmount = targetAmount;
        this.currentAmount = currentAmount;
        this.targetDate = targetDate;
    }

    public Long getId() { return id; }
    public AppUser getUser() { return user; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getTargetAmount() { return targetAmount; }
    public void setTargetAmount(BigDecimal targetAmount) { this.targetAmount = targetAmount; }
    public BigDecimal getCurrentAmount() { return currentAmount; }
    public void setCurrentAmount(BigDecimal currentAmount) { this.currentAmount = currentAmount; }
    public LocalDate getTargetDate() { return targetDate; }
    public void setTargetDate(LocalDate targetDate) { this.targetDate = targetDate; }
}
