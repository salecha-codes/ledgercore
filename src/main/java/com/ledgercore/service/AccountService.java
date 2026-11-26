package com.ledgercore.service;

import com.ledgercore.domain.Account;
import com.ledgercore.dto.AccountDtos.AccountResponse;
import com.ledgercore.dto.AccountDtos.CreateAccountRequest;
import com.ledgercore.exception.AccountNotFoundException;
import com.ledgercore.repository.AccountRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {

    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {
        Account saved = accountRepository.save(Account.openUserAccount(request.ownerName(), request.currency()));
        return AccountResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccount(UUID id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException(id));
        return AccountResponse.from(account);
    }
}
