package com.ledgercore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ledgercore.domain.AccountType;
import com.ledgercore.domain.Currency;
import com.ledgercore.repository.AccountRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
class AccountControllerTest {

    @Autowired private WebApplicationContext context;
    @Autowired private AccountRepository accountRepository;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void treasuryAccountsAreSeededByFlyway() {
        UUID inrTreasury = UUID.fromString("00000000-0000-0000-0000-000000000001");
        var treasury = accountRepository.findById(inrTreasury).orElseThrow();
        assertThat(treasury.getAccountType()).isEqualTo(AccountType.TREASURY);
        assertThat(treasury.getCurrency()).isEqualTo(Currency.INR);
        assertThat(accountRepository.count()).isGreaterThanOrEqualTo(4);
    }

    @Test
    void createAndGetAccountSuccess() throws Exception {
        String body = """
                {"ownerName":"Rahul Sharma","currency":"INR"}
                """;
        String response = mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ownerName").value("Rahul Sharma"))
                .andExpect(jsonPath("$.currency").value("INR"))
                .andExpect(jsonPath("$.minorUnits").value(2))
                .andExpect(jsonPath("$.accountType").value("USER"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.availableBalance").value(0))
                .andExpect(jsonPath("$.heldBalance").value(0))
                .andReturn().getResponse().getContentAsString();

        JsonNode json = objectMapper.readTree(response);
        String id = json.get("id").asText();

        mockMvc.perform(get("/api/v1/accounts/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.ownerName").value("Rahul Sharma"));
    }

    @Test
    void jpyHasZeroMinorUnitsAndBhdHasThree() throws Exception {
        mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ownerName":"Kenji","currency":"JPY"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.minorUnits").value(0));

        mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ownerName":"Ali","currency":"BHD"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.minorUnits").value(3));
    }

    @Test
    void rejectsBlankOwnerAndUnsupportedCurrency() throws Exception {
        mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ownerName":"   ","currency":"INR"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details.ownerName").exists());

        mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ownerName":"Rahul","currency":"XYZ"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_REQUEST"));
    }

    @Test
    void returns404ForMissingAccount() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/" + UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("ACCOUNT_NOT_FOUND"));
    }

    @Test
    void databaseCheckConstraintBlocksNegativeUserBalance() {
        UUID id = UUID.randomUUID();
        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO accounts (id, owner_name, currency, account_type, status, available_balance, held_balance, created_at) " +
                        "VALUES (?, 'BadUser', 'INR', 'USER', 'ACTIVE', -100, 0, CURRENT_TIMESTAMP)",
                id
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void actuatorHealthIsUp() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
