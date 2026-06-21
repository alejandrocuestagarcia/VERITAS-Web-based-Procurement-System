package com.veritas.backend.workflow.validation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.veritas.backend.budget.entity.InternalBudget;
import com.veritas.backend.department.entity.Department;
import com.veritas.backend.integrations.currency.dto.CurrencyConversionResult;
import com.veritas.backend.integrations.currency.service.CurrencyConversionService;
import com.veritas.backend.integrations.currency.entity.ExchangeRateSource;
import java.time.LocalDateTime;
import com.veritas.backend.project.entity.Project;
import com.veritas.backend.requisition.entity.Request;
import com.veritas.backend.requisition.entity.Priority;
import com.veritas.backend.team.entity.Team;
import com.veritas.backend.user.entity.User;
import com.veritas.backend.user.entity.UserRole;
import com.veritas.backend.vendor.entity.Quote;
import com.veritas.backend.integrations.currency.entity.Currency;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

class WorkflowBranchingContextUnitTest {

    @Test
    void createDummyContext_returnsValidDummy() {
        WorkflowBranchingContext context = WorkflowBranchingContext.createDummyContext();
        assertAll(
            () -> assertEquals(BigDecimal.ZERO, context.getSelectedQuoteTotalAmount()),
            () -> assertEquals("MEDIUM", context.getPriority()),
            () -> assertEquals(0, context.getTotalQuantity()),
            () -> assertNotNull(context.getDepartment()),
            () -> assertEquals(0L, context.getDepartment().getId()),
            () -> assertEquals("", context.getDepartment().getName()),
            () -> assertNotNull(context.getProject()),
            () -> assertEquals(0L, context.getProject().getId()),
            () -> assertEquals("", context.getProject().getName()),
            () -> assertEquals("", context.getProject().getKey()),
            () -> assertNotNull(context.getRequester()),
            () -> assertEquals(0L, context.getRequester().getId()),
            () -> assertEquals("", context.getRequester().getName()),
            () -> assertEquals("", context.getRequester().getEmail()),
            () -> assertEquals("", context.getRequester().getRole()),
            () -> assertFalse(context.getRequester().getIsTeamLeader()),
            () -> assertNotNull(context.getBudget()),
            () -> assertEquals(0L, context.getBudget().getId()),
            () -> assertEquals("", context.getBudget().getName()),
            () -> assertEquals(BigDecimal.ZERO, context.getBudget().getTotalAmount()),
            () -> assertEquals(BigDecimal.ZERO, context.getBudget().getCommittedSpend()),
            () -> assertEquals(BigDecimal.ZERO, context.getBudget().getActualSpend()),
            () -> assertEquals(BigDecimal.ZERO, context.getBudget().getSafetyBuffer()),
            () -> assertEquals(BigDecimal.ZERO, context.getBudget().getRemainingAmount()),
            () -> assertNotNull(context.getGlobalBudget()),
            () -> assertEquals(0L, context.getGlobalBudget().getId())
        );
    }

    @Test
    void constructor_withFullyPopulatedRequest_mapsCorrectly() {
        CurrencyConversionService conversionService = mock(CurrencyConversionService.class);
        
        Quote quote = new Quote();
        quote.setTotalAmount(BigDecimal.valueOf(150.0));
        quote.setCurrency(Currency.USD);
        
        CurrencyConversionResult conversionResult = new CurrencyConversionResult(BigDecimal.valueOf(140.0), BigDecimal.valueOf(0.93), LocalDateTime.now(), ExchangeRateSource.FRANKFURTER);
        when(conversionService.convert(BigDecimal.valueOf(150.0), Currency.USD)).thenReturn(conversionResult);

        User leader = new User();
        leader.setId(10L);

        User creator = new User();
        creator.setId(10L); // Creator is the leader
        creator.setName("John Doe");
        creator.setEmail("john@example.com");
        creator.setRole(UserRole.REQUESTER);

        Team team = new Team();
        team.setLeader(leader);
        creator.setTeam(team);

        Department dept = new Department();
        dept.setDepartmentId(5L);
        dept.setName("Engineering");
        InternalBudget deptBudget = new InternalBudget();
        deptBudget.setId(100L);
        deptBudget.setBudgetName("Dept Budget");
        deptBudget.setTotalAmount(BigDecimal.valueOf(1000));
        deptBudget.setCommittedSpend(BigDecimal.valueOf(200));
        deptBudget.setActualSpend(BigDecimal.valueOf(100));
        deptBudget.setSafetyBuffer(BigDecimal.valueOf(50));
        dept.setInternalBudget(deptBudget);
        team.setDepartment(dept);

        Project project = new Project();
        project.setId(20L);
        project.setName("Project Alpha");
        project.setProjectKey("ALPHA");
        InternalBudget projBudget = new InternalBudget();
        projBudget.setId(200L);
        projBudget.setBudgetName("Proj Budget");
        project.setInternalBudget(projBudget);

        InternalBudget parentBudget = new InternalBudget();
        parentBudget.setId(300L);
        parentBudget.setBudgetName("Parent Budget");

        InternalBudget requestBudget = new InternalBudget();
        requestBudget.setId(400L);
        requestBudget.setBudgetName("Req Budget");
        requestBudget.setParentBudget(parentBudget);

        Request request = new Request();
        quote.setSelected(true);
        request.getQuotes().add(quote);
        request.setPriority(Priority.HIGH);
        request.setTotalQuantity(5);
        request.setUser(creator);
        request.setTeam(team);
        request.setProject(project);
        request.setBudget(requestBudget);

        WorkflowBranchingContext context = new WorkflowBranchingContext(request, conversionService);

        assertAll(
            () -> assertEquals(BigDecimal.valueOf(140.0), context.getSelectedQuoteTotalAmount()),
            () -> assertEquals("HIGH", context.getPriority()),
            () -> assertEquals(5, context.getTotalQuantity()),
            () -> assertNotNull(context.getDepartment()),
            () -> assertEquals(5L, context.getDepartment().getId()),
            () -> assertEquals("Engineering", context.getDepartment().getName()),
            () -> assertEquals(100L, context.getDepartment().getBudget().getId()),
            () -> assertNotNull(context.getProject()),
            () -> assertEquals(20L, context.getProject().getId()),
            () -> assertEquals("Project Alpha", context.getProject().getName()),
            () -> assertEquals("ALPHA", context.getProject().getKey()),
            () -> assertEquals(200L, context.getProject().getBudget().getId()),
            () -> assertNotNull(context.getRequester()),
            () -> assertEquals(10L, context.getRequester().getId()),
            () -> assertEquals("John Doe", context.getRequester().getName()),
            () -> assertEquals("john@example.com", context.getRequester().getEmail()),
            () -> assertEquals("REQUESTER", context.getRequester().getRole()),
            () -> assertTrue(context.getRequester().getIsTeamLeader()),
            () -> assertNotNull(context.getBudget()),
            () -> assertEquals(400L, context.getBudget().getId()),
            () -> assertNotNull(context.getGlobalBudget()),
            () -> assertEquals(300L, context.getGlobalBudget().getId())
        );
    }

    @Test
    void constructor_withConversionExceptions_mapsCorrectly() {
        CurrencyConversionService conversionService = mock(CurrencyConversionService.class);
        
        Quote quote = new Quote();
        quote.setTotalAmount(BigDecimal.valueOf(150.0));
        quote.setCurrency(Currency.USD);
        
        // Throw exception on conversion to cover catch block
        when(conversionService.convert(any(), any())).thenThrow(new EntityNotFoundException("Not found"));

        Request request = createFullyPopulatedRequest();
        quote.setSelected(true);
        request.getQuotes().add(quote);

        WorkflowBranchingContext context = new WorkflowBranchingContext(request, conversionService);

        assertAll(
            () -> assertNull(context.getSelectedQuoteTotalAmount()),
            () -> assertEquals("HIGH", context.getPriority()),
            () -> assertEquals(2, context.getTotalQuantity()),
            () -> assertEquals(5L, context.getDepartment().getId()),
            () -> assertEquals(20L, context.getProject().getId()),
            () -> assertNotNull(context.getRequester()),
            () -> assertEquals(11L, context.getRequester().getId()),
            () -> assertEquals("REQUESTER", context.getRequester().getRole()),
            () -> assertTrue(context.getRequester().getIsTeamLeader()),
            () -> assertEquals(400L, context.getBudget().getId())
        );
    }

    @Test
    void constructor_withConversionIllegalArgumentException_returnsNullAmount() {
        CurrencyConversionService conversionService = mock(CurrencyConversionService.class);
        Quote quote = new Quote();
        quote.setTotalAmount(BigDecimal.TEN);
        when(conversionService.convert(any(), any())).thenThrow(new IllegalArgumentException("Invalid"));

        Request request = createFullyPopulatedRequest();
        quote.setSelected(true);
        request.getQuotes().add(quote);

        WorkflowBranchingContext context = new WorkflowBranchingContext(request, conversionService);
        assertNull(context.getSelectedQuoteTotalAmount());
    }

    @Test
    void constructor_teamLeaderResolution_whenCreatorIsNotLeader() {
        CurrencyConversionService conversionService = mock(CurrencyConversionService.class);
        
        Request request = createFullyPopulatedRequest();
        User leader = new User();
        leader.setId(22L);
        request.getUser().setId(11L); // Not leader
        request.getUser().getTeam().setLeader(leader);

        WorkflowBranchingContext context = new WorkflowBranchingContext(request, conversionService);
        assertFalse(context.getRequester().getIsTeamLeader());
    }

    @Test
    void constructor_teamLeaderResolution_whenLeaderIsNull() {
        CurrencyConversionService conversionService = mock(CurrencyConversionService.class);
        
        Request request = createFullyPopulatedRequest();
        request.getUser().setId(11L);
        request.getUser().getTeam().setLeader(null); // Null leader

        WorkflowBranchingContext context = new WorkflowBranchingContext(request, conversionService);
        assertFalse(context.getRequester().getIsTeamLeader());
    }

    @Test
    void constructor_globalBudgetResolution_recursionBreaksAfter100() {
        CurrencyConversionService conversionService = mock(CurrencyConversionService.class);
        
        InternalBudget root = new InternalBudget();
        root.setId(0L);
        
        InternalBudget current = root;
        // Construct deep hierarchy to trigger depth check
        for (int i = 1; i <= 105; i++) {
            InternalBudget child = new InternalBudget();
            child.setId((long) i);
            child.setParentBudget(current);
            current = child;
        }

        Request request = createFullyPopulatedRequest();
        request.setBudget(current);

        WorkflowBranchingContext context = new WorkflowBranchingContext(request, conversionService);
        // Because of depth constraint, it should stop and not hit infinite loop, returning the 100th level up
        assertNotNull(context.getGlobalBudget());
    }

    @Test
    void branchingBudget_nullFields_fallbackToZero() {
        InternalBudget budget = new InternalBudget();
        budget.setId(99L);
        budget.setBudgetName("Null fields budget");
        budget.setTotalAmount(null);
        budget.setCommittedSpend(null);
        budget.setActualSpend(null);
        budget.setSafetyBuffer(null);

        WorkflowBranchingContext.BranchingBudget b = new WorkflowBranchingContext.BranchingBudget(budget);
        assertAll(
            () -> assertEquals(99L, b.getId()),
            () -> assertEquals("Null fields budget", b.getName()),
            () -> assertEquals(BigDecimal.ZERO, b.getTotalAmount()),
            () -> assertEquals(BigDecimal.ZERO, b.getCommittedSpend()),
            () -> assertEquals(BigDecimal.ZERO, b.getActualSpend()),
            () -> assertEquals(BigDecimal.ZERO, b.getSafetyBuffer()),
            () -> assertEquals(BigDecimal.ZERO, b.getRemainingAmount())
        );
    }

    private Request createFullyPopulatedRequest() {
        User creator = new User();
        creator.setId(11L);
        creator.setName("Jane Doe");
        creator.setEmail("jane@example.com");
        creator.setRole(UserRole.REQUESTER);

        Team team = new Team();
        team.setLeader(creator);
        creator.setTeam(team);

        Department dept = new Department();
        dept.setDepartmentId(5L);
        dept.setName("Engineering");
        
        InternalBudget deptBudget = new InternalBudget();
        deptBudget.setId(100L);
        deptBudget.setBudgetName("Dept Budget");
        dept.setInternalBudget(deptBudget);
        team.setDepartment(dept);

        Project project = new Project();
        project.setId(20L);
        project.setName("Project Alpha");
        project.setProjectKey("ALPHA");
        
        InternalBudget projBudget = new InternalBudget();
        projBudget.setId(200L);
        projBudget.setBudgetName("Proj Budget");
        project.setInternalBudget(projBudget);

        InternalBudget requestBudget = new InternalBudget();
        requestBudget.setId(400L);
        requestBudget.setBudgetName("Req Budget");

        Request request = new Request();
        request.setPriority(Priority.HIGH);
        request.setTotalQuantity(2);
        request.setUser(creator);
        request.setTeam(team);
        request.setProject(project);
        request.setBudget(requestBudget);
        return request;
    }
}
