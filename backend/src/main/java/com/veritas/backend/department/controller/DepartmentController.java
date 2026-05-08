package com.veritas.backend.department.controller;

import com.veritas.backend.config.annotations.IsFinanceOfficer;
import com.veritas.backend.config.annotations.IsRequester;
import com.veritas.backend.department.dto.DepartmentCreateDto;
import com.veritas.backend.department.dto.DepartmentDto;
import com.veritas.backend.department.service.DepartmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/departments")
@RequiredArgsConstructor
@Tag(name = "Departments Module", description = "Management of company departments")
public class DepartmentController {

    private final DepartmentService departmentService;

    @Operation(summary = "List departments", description = "Retrieves all departments.")
    @IsRequester
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<DepartmentDto>> getAllDepartments() {
        log.info("GET /departments");
        return ResponseEntity.ok(departmentService.getAllDepartments());
    }

    @Operation(summary = "Get department", description = "Retrieves a department.")
    @IsFinanceOfficer
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DepartmentDto> getDepartment(@PathVariable Long id) {
        log.info("GET /departments/{}", id);
        return ResponseEntity.ok(departmentService.getDepartmentById(id));
    }

    @Operation(summary = "Create department", description = "Creates a new department.")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @IsFinanceOfficer
    public ResponseEntity<DepartmentDto> createDepartment(@Valid @RequestBody DepartmentCreateDto request) {
        log.info("POST /departments – creating department with name: {}", request.name());
        DepartmentDto departmentDto = departmentService.createDepartment(request);
        log.info("Department created successfully – id: {}", departmentDto.id());
        URI departmentURI = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(departmentDto.id()).toUri();
        return ResponseEntity.created(departmentURI).body(departmentDto);
    }

    @Operation(summary = "Update department", description = "Updates an existing department.")
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @IsFinanceOfficer
    public ResponseEntity<DepartmentDto> updateDepartment(@PathVariable Long id, @Valid @RequestBody DepartmentCreateDto request) {
        log.info("PUT /departments/{} – updating department with name: {}", id, request.name());
        return ResponseEntity.ok(departmentService.updateDepartment(id, request));
    }

    @Operation(summary = "Delete department", description = "Deletes an existing department.")
    @DeleteMapping(value = "/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @IsFinanceOfficer
    public ResponseEntity<Void> deleteDepartment(@PathVariable Long id) {
        log.info("DELETE /departments/{}", id);
        departmentService.deleteDepartment(id);
        return ResponseEntity.noContent().build();
    }
}
