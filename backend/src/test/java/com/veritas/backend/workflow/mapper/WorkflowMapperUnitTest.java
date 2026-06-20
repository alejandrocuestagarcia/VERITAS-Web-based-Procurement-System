package com.veritas.backend.workflow.mapper;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.veritas.backend.department.entity.Department;
import com.veritas.backend.workflow.dto.WorkflowDto;
import com.veritas.backend.workflow.dto.WorkflowSaveDto;
import com.veritas.backend.workflow.entity.WorkflowDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.veritas.backend.department.mapper.DepartmentMapper;
import org.mapstruct.factory.Mappers;

import java.lang.reflect.Field;

class WorkflowMapperUnitTest {

    private WorkflowMapper mapper;

    @BeforeEach
    void setUp() throws Exception {
        mapper = Mappers.getMapper(WorkflowMapper.class);
        DepartmentMapper deptMapper = Mappers.getMapper(DepartmentMapper.class);
        Field field = mapper.getClass().getDeclaredField("departmentMapper");
        field.setAccessible(true);
        field.set(mapper, deptMapper);
    }

    @Test
    void toWorkflowDefinition_NullDto_ReturnsNull() {
        assertNull(mapper.toWorkflowDefinition(null));
    }

    @Test
    void toWorkflowDefinition_ValidDto_MapsCorrectly() {
        WorkflowSaveDto dto = new WorkflowSaveDto("xmlContent", 12L);
        WorkflowDefinition def = mapper.toWorkflowDefinition(dto);
        assertAll(
            () -> assertEquals("xmlContent", def.getBpmnXml()),
            () -> assertNull(def.getDepartment())
        );
    }

    @Test
    void toWorkflowDto_NullDefinition_ReturnsNull() {
        assertNull(mapper.toWorkflowDto(null));
    }

    @Test
    void toWorkflowDto_ValidDefinition_MapsCorrectly() {
        Department dept = new Department();
        dept.setDepartmentId(15L);
        dept.setName("Finance");

        WorkflowDefinition def = new WorkflowDefinition();
        def.setId(1L);
        def.setName("Standard Workflow");
        def.setBpmnXml("<xml/>");
        def.setDepartment(dept);
        def.setDescription("desc");
        def.setIsActive(true);

        WorkflowDto dto = mapper.toWorkflowDto(def);

        assertAll(
            () -> assertEquals(1L, dto.id()),
            () -> assertEquals("Standard Workflow", dto.name()),
            () -> assertEquals("<xml/>", dto.bpmnXml()),
            () -> assertEquals(15L, dto.department().id()),
            () -> assertEquals("Finance", dto.department().name()),
            () -> assertEquals("desc", dto.description()),
            () -> assertTrue(dto.isActive())
        );
    }

    @Test
    void toWorkflowDto_NullDepartment_MapsNullDepartment() {
        WorkflowDefinition def = new WorkflowDefinition();
        def.setId(1L);
        def.setDepartment(null);

        WorkflowDto dto = mapper.toWorkflowDto(def);
        assertNull(dto.department());
    }
}
