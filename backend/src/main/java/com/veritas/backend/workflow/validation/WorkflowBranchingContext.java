package com.veritas.backend.workflow.validation;

import com.veritas.backend.requisition.entity.Request;
import com.veritas.backend.user.entity.User;
import com.veritas.backend.team.entity.Team;
import com.veritas.backend.project.entity.Project;
import com.veritas.backend.budget.entity.InternalBudget;
import com.veritas.backend.department.entity.Department;
import com.veritas.backend.integrations.currency.dto.CurrencyConversionResult;
import com.veritas.backend.integrations.currency.service.CurrencyConversionService;
import com.veritas.backend.vendor.entity.Quote;

import jakarta.persistence.EntityNotFoundException;
import lombok.Getter;
import java.math.BigDecimal;
import java.util.List;

/**
 * Read-only evaluation context passed to SpEL expressions in workflow transition conditions and rules.
 * Exposes a flattened, serialization-friendly view of a {@link Request}
 * — including the selected quote total (converted to EUR), priority, quantity, requester, department,
 * project, and budget hierarchy, so BPMN condition expressions can branch on request data without
 * direct entity access. Use {@link #createDummyContext()} for validation-time expression dry-runs.
 */
@Getter
public class WorkflowBranchingContext {
    private final BigDecimal selectedQuoteTotalAmount;
    private final String priority;
    private final Integer totalQuantity;
    private final BranchingDepartment department;
    private final BranchingProject project;
    private final BranchingUser requester;
    private final BranchingBudget budget;
    private final BranchingBudget globalBudget;

    public WorkflowBranchingContext(Request request, CurrencyConversionService currencyConversionService) {

        Quote selectedQuote = request.getSelectedQuote();

        BigDecimal quoteAmount = null;
        if (selectedQuote != null) {
            try {
                CurrencyConversionResult conversionResult = currencyConversionService.convert(selectedQuote.getTotalAmount(), selectedQuote.getCurrency());
                quoteAmount = conversionResult.convertedAmount();
            } catch (IllegalArgumentException | EntityNotFoundException exception) {
                quoteAmount = null;
            }
        }
        this.selectedQuoteTotalAmount = quoteAmount;

        this.priority = request.getPriority().name();
        this.totalQuantity = request.getTotalQuantity();

        User creator = request.getUser();

        // Department resolution
        Team resolvedTeam = request.getTeam();
        Department dept = resolvedTeam.getDepartment();
        this.department = new BranchingDepartment(
            dept.getDepartmentId(),
            dept.getName(),
            new BranchingBudget(dept.getInternalBudget())
        );

        // Project resolution
        Project proj = request.getProject();
        this.project = new BranchingProject(
            proj.getId(),
            proj.getName(),
            proj.getProjectKey(),
            new BranchingBudget(proj.getInternalBudget())
        );

        // Requester resolution
        boolean teamLeader = false;
        User leader = creator.getTeam().getLeader();
        if (leader != null && leader.getId().equals(creator.getId())) {
            teamLeader = true;
        }
        this.requester = new BranchingUser(creator.getId(), creator.getName(), creator.getEmail(), creator.getRole().name(), teamLeader);

        // Budget resolution
        InternalBudget b = request.getBudget();
        this.budget = new BranchingBudget(b);

        // Global budget resolution
        InternalBudget rootBudget = resolveGlobalBudget(b);
        this.globalBudget = new BranchingBudget(rootBudget);
    }

    public static WorkflowBranchingContext createDummyContext() {
        return new WorkflowBranchingContext();
    }

    private WorkflowBranchingContext() {
        this.selectedQuoteTotalAmount = BigDecimal.ZERO;
        this.priority = "MEDIUM";
        this.totalQuantity = 0;
        this.department = new BranchingDepartment();
        this.project = new BranchingProject();
        this.requester = new BranchingUser();
        this.budget = new BranchingBudget();
        this.globalBudget = new BranchingBudget();
    }

    private InternalBudget resolveGlobalBudget(InternalBudget b) {
        InternalBudget current = b;
        int depth = 0;
        while (current.getParentBudget() != null && depth < 100) {
            current = current.getParentBudget();
            depth++;
        }
        return current;
    }

    @Getter
    public static class BranchingDepartment {
        private final Long id;
        private final String name;
        private final BranchingBudget budget;

        public BranchingDepartment(Long id, String name, BranchingBudget budget) {
            this.id = id;
            this.name = name;
            this.budget = budget;
        }

        private BranchingDepartment() {
            this.id = 0L;
            this.name = "";
            this.budget = new BranchingBudget();
        }
    }

    @Getter
    public static class BranchingProject {
        private final Long id;
        private final String name;
        private final String key;
        private final BranchingBudget budget;

        public BranchingProject(Long id, String name, String key, BranchingBudget budget) {
            this.id = id;
            this.name = name;
            this.key = key;
            this.budget = budget;
        }

        private BranchingProject() {
            this.id = 0L;
            this.name = "";
            this.key = "";
            this.budget = new BranchingBudget();
        }
    }

    @Getter
    public static class BranchingUser {
        private final Long id;
        private final String name;
        private final String email;
        private final String role;
        private final Boolean isTeamLeader;

        public BranchingUser(Long id, String name, String email, String role, Boolean isTeamLeader) {
            this.id = id;
            this.name = name;
            this.email = email;
            this.role = role;
            this.isTeamLeader = isTeamLeader;
        }

        private BranchingUser() {
            this.id = 0L;
            this.name = "";
            this.email = "";
            this.role = "";
            this.isTeamLeader = false;
        }
    }

    @Getter
    public static class BranchingBudget {
        private final Long id;
        private final String name;
        private final BigDecimal totalAmount;
        private final BigDecimal committedSpend;
        private final BigDecimal actualSpend;
        private final BigDecimal safetyBuffer;
        private final BigDecimal remainingAmount;

        public BranchingBudget(InternalBudget b) {
            this.id = b.getId();
            this.name = b.getBudgetName();
            this.totalAmount = b.getTotalAmount() != null ? b.getTotalAmount() : BigDecimal.ZERO;
            this.committedSpend = b.getCommittedSpend() != null ? b.getCommittedSpend() : BigDecimal.ZERO;
            this.actualSpend = b.getActualSpend() != null ? b.getActualSpend() : BigDecimal.ZERO;
            this.safetyBuffer = b.getSafetyBuffer() != null ? b.getSafetyBuffer() : BigDecimal.ZERO;
            this.remainingAmount = this.totalAmount.subtract(this.committedSpend).subtract(this.actualSpend);
        }

        private BranchingBudget() {
            this.id = 0L;
            this.name = "";
            this.totalAmount = BigDecimal.ZERO;
            this.committedSpend = BigDecimal.ZERO;
            this.actualSpend = BigDecimal.ZERO;
            this.safetyBuffer = BigDecimal.ZERO;
            this.remainingAmount = BigDecimal.ZERO;
        }
    }
}
