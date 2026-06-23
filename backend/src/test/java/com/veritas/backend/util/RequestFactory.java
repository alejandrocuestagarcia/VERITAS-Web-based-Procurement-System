package com.veritas.backend.util;

import com.veritas.backend.budget.entity.BudgetType;
import com.veritas.backend.budget.entity.InternalBudget;
import com.veritas.backend.budget.repository.InternalBudgetRepository;
import com.veritas.backend.project.entity.Project;
import com.veritas.backend.project.repository.ProjectRepository;
import com.veritas.backend.requisition.entity.Priority;
import com.veritas.backend.requisition.entity.Request;
import com.veritas.backend.requisition.repository.RequestRepository;
import com.veritas.backend.team.entity.Team;
import com.veritas.backend.workflow.entity.WorkflowComponent;
import com.veritas.backend.workflow.entity.WorkflowDefinition;
import com.veritas.backend.workflow.entity.WorkflowStep;
import com.veritas.backend.workflow.repository.WorkflowDefinitionRepository;
import com.veritas.backend.workflow.repository.WorkflowStepRepository;
import com.veritas.backend.user.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
public class RequestFactory {

    @Autowired private RequestRepository requestRepository;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private InternalBudgetRepository internalBudgetRepository;
    @Autowired private WorkflowDefinitionRepository workflowDefinitionRepository;
    @Autowired private WorkflowStepRepository workflowStepRepository;
    @Autowired private UserFactory userFactory;

    public InternalBudget createBudget(String name) {
        return internalBudgetRepository.save(InternalBudget.builder()
                .budgetName(name)
                .totalAmount(BigDecimal.valueOf(1000000.00))
                .committedSpend(BigDecimal.ZERO)
                .actualSpend(BigDecimal.ZERO)
                .safetyBuffer(BigDecimal.ZERO)
                .budgetType(BudgetType.REQUEST)
                .build());
    }

    public Project createProject(String name, String key, Team team) {
        InternalBudget budget = InternalBudget.builder()
                .budgetName(name + " Budget")
                .totalAmount(BigDecimal.valueOf(1000000.00))
                .committedSpend(BigDecimal.ZERO)
                .actualSpend(BigDecimal.ZERO)
                .safetyBuffer(BigDecimal.ZERO)
                .budgetType(BudgetType.PROJECT)
                .build();
        String uniqueKey = key + "-" + java.util.UUID.randomUUID().toString().substring(0, 8);
        return projectRepository.save(Project.builder()
                .name(name)
                .projectKey(uniqueKey)
                .team(team)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusYears(1))
                .internalBudget(budget)
                .build());
    }

    public WorkflowStep createWorkflowWithStartStep(String name) {
        WorkflowDefinition workflow = new WorkflowDefinition();
        workflow.setName(name);
        workflow.setVersion(1);
        workflow.setIsActive(true);
        workflow.setBpmnXml("<bpmn/>");
        workflow = workflowDefinitionRepository.save(workflow);

        WorkflowStep startStep = new WorkflowStep();
        startStep.setWorkflowDefinition(workflow);
        startStep.setWorkflowComponent(WorkflowComponent.START_EVENT);
        startStep.setName("Start");
        return workflowStepRepository.save(startStep);
    }

    /**
     * Spawns a request with default valid mocked entities for all database non-null constraints.
     * Always creates a fresh project, budget, and workflow to guarantee test isolation.
     */
    public Request createValidRequest(String requestName, User requester) {
        Team team = requester.getTeam();
        if (team == null) {
            team = userFactory.createTeam("Team for " + requester.getEmail());
        }

        Project project = createProject(requestName + " Project", "PRJ", team);
        InternalBudget budget = createBudget(requestName + " Budget");
        WorkflowStep startStep = createWorkflowWithStartStep(requestName + " Workflow");

        project.setRequestCounter(project.getRequestCounter() + 1);
        project = projectRepository.save(project);

        Request request = new Request();
        request.setRequestName(requestName);
        request.setUser(requester);
        request.setTeam(team);
        request.setProject(project);
        request.setBudget(budget);
        request.setWorkflowDefinition(startStep.getWorkflowDefinition());
        request.setCurrentStep(startStep);
        request.setPriority(Priority.LOW);
        request.setRequestKey(project.getProjectKey() + "-" + project.getRequestCounter());
        return requestRepository.save(request);
    }
}
