package com.veritas.backend.requisition;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.veritas.backend.project.entity.Project;
import com.veritas.backend.requisition.dto.RequisitionDto;
import com.veritas.backend.requisition.entity.Priority;
import com.veritas.backend.requisition.entity.Request;
import com.veritas.backend.requisition.mapper.RequisitionMapper;
import org.mapstruct.factory.Mappers;
import com.veritas.backend.team.entity.Team;
import com.veritas.backend.user.entity.User;
import com.veritas.backend.workflow.entity.WorkflowStep;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RequisitionMapperUnitTest {

    private RequisitionMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = Mappers.getMapper(RequisitionMapper.class);
    }

    @Test
    void ToDto_WithCurrentStep_MapsStepNameAsCurrentStep() {
        WorkflowStep step = new WorkflowStep();
        step.setName("Manager Approval");

        Request request = buildRequest();
        request.setCurrentStep(step);

        RequisitionDto dto = mapper.toDto(request);
        assertEquals("Manager Approval", dto.status());
    }

    @Test
    void ToDto_DraftCurrentStep_ReturnsDraftStatus() {
        WorkflowStep draftStep = new WorkflowStep();
        draftStep.setName("DRAFT");

        Request request = buildRequest();
        request.setCurrentStep(draftStep);

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
        request.setProject(project);
        request.setTeam(team);
        request.setUser(user);
        request.setCurrentStep(step);
        return request;
    }
}
