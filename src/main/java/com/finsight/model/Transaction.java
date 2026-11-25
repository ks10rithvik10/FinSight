package com.finsight.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(
    name = "transactions",
    indexes = { @Index(name = "idx_tx_user_date", columnList = "user_id,transactionDate") }
)
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private AppUser user;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private String type; // INCOME or EXPENSE

    @Column(nullable = false)
    private String category;

    private String description;

    @Column(nullable = false)
    private LocalDate transactionDate;

    protected Transaction() {
    }

    public Transaction(AppUser user, BigDecimal amount, String type, String category,
                       String description, LocalDate transactionDate) {
        this.user = user;
        this.amount = amount;
        this.type = type;
        this.category = category;
        this.description = description;
        this.transactionDate = transactionDate;
    }

    public Long getId() { return id; }
    public AppUser getUser() { return user; }
    public void setUser(AppUser user) { this.user = user; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDate getTransactionDate() { return transactionDate; }
    public void setTransactionDate(LocalDate transactionDate) { this.transactionDate = transactionDate; }
}
