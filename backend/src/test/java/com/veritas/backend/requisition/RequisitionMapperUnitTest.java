package com.veritas.backend.requisition;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.veritas.backend.project.entity.Project;
import com.veritas.backend.requisition.dto.RequisitionDto;
import com.veritas.backend.requisition.entity.Priority;
import com.veritas.backend.requisition.entity.Request;
import com.veritas.backend.requisition.mapper.RequisitionMapper;
import com.veritas.backend.requisition.mapper.RequisitionMapperImpl;
import com.veritas.backend.team.entity.Team;
import com.veritas.backend.user.entity.User;
import com.veritas.backend.workflow.entity.WorkflowStep;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RequisitionMapperUnitTest {

    private RequisitionMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new RequisitionMapperImpl();
    }

    @Test
    void ToDto_WithCurrentStep_MapsStepNameAsCurrentStep() {
        WorkflowStep step = new WorkflowStep();
        step.setName("Manager Approval");

        Request request = buildRequest();
        request.setCurrentStepID(step);

        RequisitionDto dto = mapper.toDto(request);
        assertEquals("Manager Approval", dto.currentStep());
    }

    @Test
    void ToDto_NullCurrentStep_ReturnsDraftStatus() {
        Request request = buildRequest();
        request.setCurrentStepID(null);

        RequisitionDto dto = mapper.toDto(request);
        assertEquals("DRAFT", dto.status());
    }

    private Request buildRequest() {
        Project project = new Project();
        project.setName("Test Project");

        Team team = new Team();
        team.setName("Engineering");

        User user = new User();
        user.setName("Max Mustermann");

        WorkflowStep step = new WorkflowStep();
        step.setName("Start");

        Request request = new Request();
        request.setRequestID(42L);
        request.setRequestName("Test Request");
        request.setRequestKey("PRJ-1");
        request.setPriority(Priority.HIGH);
        request.setProjectID(project);
        request.setTeamID(team);
        request.setUserID(user);
        request.setCurrentStepID(step);
        return request;
    }
}
