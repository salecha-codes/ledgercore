package com.ledgercore.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "accounts")
public class Account {

    @Id
    private UUID id;

    @Column(name = "owner_name", nullable = false, length = 120)
    private String ownerName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private Currency currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 16)
    private AccountType accountType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AccountStatus status;

    @Column(name = "available_balance", nullable = false)
    private long availableBalance;

    @Column(name = "held_balance", nullable = false)
    private long heldBalance;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Account() {}

    public static Account openUserAccount(String ownerName, Currency currency) {
        Account account = new Account();
        account.id = UUID.randomUUID();
        account.ownerName = ownerName.trim();
        account.currency = currency;
        account.accountType = AccountType.USER;
        account.status = AccountStatus.ACTIVE;
        account.availableBalance = 0L;
        account.heldBalance = 0L;
        account.createdAt = Instant.now();
        return account;
    }

    public UUID getId() { return id; }
    public String getOwnerName() { return ownerName; }
    public Currency getCurrency() { return currency; }
    public AccountType getAccountType() { return accountType; }
    public AccountStatus getStatus() { return status; }
    public long getAvailableBalance() { return availableBalance; }
    public long getHeldBalance() { return heldBalance; }
    public Instant getCreatedAt() { return createdAt; }
}
