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

import lombok.Getter;
import java.math.BigDecimal;
import java.util.List;

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

        // Selected Quote Resolution
        List<Quote> quotes = request.getQuotes();
        Quote selectedQuote = null;
        for (Quote quote : quotes) {
            if (quote.isSelected()) {
                selectedQuote = quote;
            }
        }

        if (selectedQuote != null) {
            CurrencyConversionResult conversionResult = currencyConversionService.convert(
                selectedQuote.getTotalAmount(),
                selectedQuote.getCurrency()
            );
            this.selectedQuoteTotalAmount = conversionResult.convertedAmount();
        } else {
            this.selectedQuoteTotalAmount = null;
        }

        this.priority = request.getPriority() != null ? request.getPriority().name() : null;
        this.totalQuantity = request.getTotalQuantity();

        User creator = request.getUser();

        // Department resolution (from team assigned directly to request, or creator's team)
        Team resolvedTeam = request.getTeam();
        if (resolvedTeam == null && creator != null) {
            resolvedTeam = creator.getTeam();
        }
        Department dept = (resolvedTeam != null) ? resolvedTeam.getDepartment() : null;
        this.department = (dept != null) ? new BranchingDepartment(
            dept.getDepartmentId(),
            dept.getName(),
            dept.getInternalBudget() != null ? new BranchingBudget(dept.getInternalBudget()) : null
        ) : null;

        // Project resolution
        Project proj = request.getProject();
        this.project = (proj != null) ? new BranchingProject(
            proj.getId(),
            proj.getName(),
            proj.getProjectKey(),
            proj.getInternalBudget() != null ? new BranchingBudget(proj.getInternalBudget()) : null
        ) : null;

        // Requester resolution
        boolean teamLeader = false;
        if (creator != null && creator.getTeam() != null) {
            User leader = creator.getTeam().getLeader();
            if (leader != null && leader.getId().equals(creator.getId())) {
                teamLeader = true;
            }
        }
        this.requester = (creator != null) ? new BranchingUser(creator.getId(), creator.getName(), creator.getEmail(), creator.getRole() != null ? creator.getRole().name() : null, teamLeader) : null;

        // Budget resolution
        InternalBudget b = request.getBudget();
        this.budget = (b != null) ? new BranchingBudget(b) : null;

        // Global budget resolution
        InternalBudget rootBudget = resolveGlobalBudget(b);
        this.globalBudget = (rootBudget != null) ? new BranchingBudget(rootBudget) : null;
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
        if (b == null) {
            return null;
        }
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
