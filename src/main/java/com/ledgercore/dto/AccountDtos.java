package com.ledgercore.dto;

import com.ledgercore.domain.Account;
import com.ledgercore.domain.AccountStatus;
import com.ledgercore.domain.AccountType;
import com.ledgercore.domain.Currency;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public final class AccountDtos {

    private AccountDtos() {}

    public record CreateAccountRequest(
            @NotBlank(message = "ownerName must not be blank")
            @Size(max = 120, message = "ownerName must be at most 120 characters")
            String ownerName,

            @NotNull(message = "currency is required (INR, USD, JPY, BHD)")
            Currency currency
    ) {}

    public record AccountResponse(
            UUID id,
            String ownerName,
            Currency currency,
            int minorUnits,
            AccountType accountType,
            AccountStatus status,
            long availableBalance,
            long heldBalance,
            Instant createdAt
    ) {
        public static AccountResponse from(Account a) {
            return new AccountResponse(
                    a.getId(),
                    a.getOwnerName(),
                    a.getCurrency(),
                    a.getCurrency().minorUnits(),
                    a.getAccountType(),
                    a.getStatus(),
                    a.getAvailableBalance(),
                    a.getHeldBalance(),
                    a.getCreatedAt()
            );
        }
    }
}
