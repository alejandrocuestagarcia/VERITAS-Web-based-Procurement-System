package com.veritas.backend.budget;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.veritas.backend.BaseDBIntegrationTest;
import com.veritas.backend.auth.service.JwtService;
import com.veritas.backend.budget.dto.BudgetDto;
import com.veritas.backend.budget.entity.BudgetType;
import com.veritas.backend.budget.entity.InternalBudget;
import com.veritas.backend.budget.repository.InternalBudgetRepository;
import com.veritas.backend.user.entity.User;
import com.veritas.backend.user.entity.UserRole;
import com.veritas.backend.user.repository.UserRepository;
import com.veritas.backend.department.entity.Department;
import com.veritas.backend.department.repository.DepartmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class BudgetControllerIntegrationTest extends BaseDBIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private InternalBudgetRepository internalBudgetRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private PasswordEncoder encoder;

    private String financeOfficerToken;
    private String requesterToken;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        departmentRepository.deleteAll();
        internalBudgetRepository.deleteAll();

        User financeOfficer = User.builder()
                .name("Finance Officer")
                .email("finance@veritas.com")
                .passwordHash(encoder.encode("password123"))
                .role(UserRole.FINANCE_OFFICER)
                .isActive(true)
                .build();
        financeOfficer = userRepository.save(financeOfficer);
        financeOfficerToken = jwtService.generateAccessToken(financeOfficer);

        User requester = User.builder()
                .name("Requester")
                .email("requester@veritas.com")
                .passwordHash(encoder.encode("password123"))
                .role(UserRole.REQUESTER)
                .isActive(true)
                .build();
        requester = userRepository.save(requester);
        requesterToken = jwtService.generateAccessToken(requester);
    }

    @Test
    void CreateBudget_NoGlobalBudgetExists_CreatesBudget() throws Exception {
        BudgetDto dto = new BudgetDto(null, 500000.0, null);

        mockMvc.perform(post("/api/v1/budget")
                        .header("Authorization", "Bearer " + financeOfficerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.totalAmount").value(500000.0));

        assertTrue(internalBudgetRepository.existsByBudgetType(BudgetType.GLOBAL));
        InternalBudget globalBudget = internalBudgetRepository.findByBudgetType(BudgetType.GLOBAL).orElseThrow();
        assertEquals(0, BigDecimal.valueOf(500000.0).compareTo(globalBudget.getTotalAmount()));
    }

    @Test
    void CreateBudget_GlobalBudgetExists_ReturnsConflict() throws Exception {

        InternalBudget globalBudget = new InternalBudget();
        globalBudget.setBudgetName("Global Budget");
        globalBudget.setBudgetType(BudgetType.GLOBAL);
        globalBudget.setTotalAmount(BigDecimal.valueOf(100000.0));
        internalBudgetRepository.save(globalBudget);

        BudgetDto dto = new BudgetDto(null, 500000.0, null);

        mockMvc.perform(post("/api/v1/budget")
                        .header("Authorization", "Bearer " + financeOfficerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict());
    }

    @Test
    void CreateBudget_AsRequester_ReturnsForbidden() throws Exception {
        BudgetDto dto = new BudgetDto(null, 500000.0, null);

        mockMvc.perform(post("/api/v1/budget")
                        .header("Authorization", "Bearer " + requesterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    @Test
    void EditBudget_GlobalBudgetExists_UpdatesBudget() throws Exception {

        InternalBudget globalBudget = new InternalBudget();
        globalBudget.setBudgetName("Global Budget");
        globalBudget.setBudgetType(BudgetType.GLOBAL);
        globalBudget.setTotalAmount(BigDecimal.valueOf(100000.0));
        globalBudget.setSafetyBuffer(BigDecimal.ZERO);
        internalBudgetRepository.save(globalBudget);

        BudgetDto dto = new BudgetDto(null, 250000.0, 10.0);

        mockMvc.perform(patch("/api/v1/budget")
                        .header("Authorization", "Bearer " + financeOfficerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAmount").value(250000.0));

        InternalBudget updated = internalBudgetRepository.findByBudgetType(BudgetType.GLOBAL).orElseThrow();
        assertAll(
                ()->assertEquals(0, BigDecimal.valueOf(250000.0).compareTo(updated.getTotalAmount())),
                ()->assertEquals(0, BigDecimal.valueOf(10.0).compareTo(updated.getSafetyBuffer()))
        );

    }

    @Test
    void EditBudget_NoGlobalBudgetExists_ReturnsNotFound() throws Exception {
        BudgetDto dto = new BudgetDto(null, 250000.0, null);

        mockMvc.perform(patch("/api/v1/budget")
                        .header("Authorization", "Bearer " + financeOfficerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound());
    }

    @Test
    void EditBudget_AsRequester_ReturnsForbidden() throws Exception {
        BudgetDto dto = new BudgetDto(null, 250000.0, null);

        mockMvc.perform(patch("/api/v1/budget")
                        .header("Authorization", "Bearer " + requesterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    @Test
    void EditBudget_NewAmountLessThanDepartmentsSum_ReturnsBadRequest() throws Exception {
        InternalBudget globalBudget = new InternalBudget();
        globalBudget.setBudgetName("Global Budget");
        globalBudget.setBudgetType(BudgetType.GLOBAL);
        globalBudget.setTotalAmount(BigDecimal.valueOf(200000.0));
        globalBudget.setSafetyBuffer(BigDecimal.ZERO);
        internalBudgetRepository.save(globalBudget);

        departmentRepository.save(Department.builder()
                .name("Marketing")
                .internalBudget(InternalBudget.builder()
                        .budgetName("Marketing Budget")
                        .budgetType(BudgetType.DEPARTMENT)
                        .totalAmount(BigDecimal.valueOf(150000.0))
                        .build())
                .build());

        BudgetDto dto = new BudgetDto(null, 100000.0, null);

        mockMvc.perform(patch("/api/v1/budget")
                        .header("Authorization", "Bearer " + financeOfficerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("is less than the sum of its department budgets")));
    }

    @Test
    void EditBudget_NewAmountGreaterThanCurrent_SavesSuccessfullyWithoutCheck() throws Exception {
        InternalBudget globalBudget = new InternalBudget();
        globalBudget.setBudgetName("Global Budget");
        globalBudget.setBudgetType(BudgetType.GLOBAL);
        globalBudget.setTotalAmount(BigDecimal.valueOf(100000.0));
        globalBudget.setSafetyBuffer(BigDecimal.ZERO);
        internalBudgetRepository.save(globalBudget);

        BudgetDto dto = new BudgetDto(null, 250000.0, null);

        mockMvc.perform(patch("/api/v1/budget")
                        .header("Authorization", "Bearer " + financeOfficerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAmount").value(250000.0));

        InternalBudget updated = internalBudgetRepository.findByBudgetType(BudgetType.GLOBAL).orElseThrow();
        assertEquals(0, BigDecimal.valueOf(250000.0).compareTo(updated.getTotalAmount()));
    }
}
