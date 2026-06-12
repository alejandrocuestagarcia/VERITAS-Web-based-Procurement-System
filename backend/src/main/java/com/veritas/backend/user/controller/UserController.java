package com.veritas.backend.user.controller;

import com.veritas.backend.config.annotations.IsAdministrator;
import com.veritas.backend.config.annotations.IsFinanceOfficer;
import com.veritas.backend.requisition.dto.RequisitionDto;
import com.veritas.backend.user.dto.UserCreationRequestDto;
import com.veritas.backend.user.dto.UserDto;
import com.veritas.backend.user.dto.UserEditDto;
import com.veritas.backend.user.dto.UserStatsDto;
import com.veritas.backend.user.entity.User;
import com.veritas.backend.user.entity.UserRole;
import com.veritas.backend.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Slf4j
@RestController
@RequestMapping(path = "/users", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "User Module", description = "Management of VERITAS users")
public class UserController {

    private final UserService userService;

    @Operation(summary = "List users", description = "Retrieves all users.")
    @IsFinanceOfficer
    @GetMapping
    public ResponseEntity<Page<UserDto>> getAllUsers(Pageable pageable, @RequestParam(required = false) String search,
            @RequestParam(required = false) UserRole userRole) {
        log.info("GET /users – page: {}, size: {}, search: '{}', role: {}", pageable.getPageNumber(),
                pageable.getPageSize(), search, userRole);
        return ResponseEntity.ok(userService.getAllUsersFiltered(pageable, search, userRole));
    }

    @Operation(summary = "Get user stats", description = "Retrieves stats about the userbase of Veritas")
    @IsFinanceOfficer
    @GetMapping("/stats")
    public ResponseEntity<UserStatsDto> getUserStats() {
        log.info("GET /users/stats");
        return ResponseEntity.ok(userService.getUserStats());
    }

    @Operation(summary = "Get user", description = "Retrieves a user.")
    @IsFinanceOfficer
    @GetMapping(path = "/{id}", produces = "application/json")
    public ResponseEntity<UserEditDto> getUserByIdForEdit(@PathVariable Long id) {
        log.info("GET /users/{}", id);
        return ResponseEntity.ok(userService.getUserByIdForEdit(id));
    }

    @Operation(summary = "Edit user", description = "Edits a users basic info.")
    @IsFinanceOfficer
    @PatchMapping(path = "/{id}", consumes = "application/json", produces = "application/json")
    public ResponseEntity<UserDto> editUser(@PathVariable Long id, @RequestBody UserEditDto edits) {
        UserDto updated = userService.editUser(id, edits);
        log.info("PATCH /users/{}", id);
        return ResponseEntity.ok(updated);
    }

    @Operation(summary = "Create user", description = "Creates a new user.")
    @IsFinanceOfficer
    @PostMapping(consumes = "application/json", produces = "application/json")
    public ResponseEntity<UserDto> createUser(@Valid @RequestBody UserCreationRequestDto user) {
        log.info("POST /users – creating user with email: {}", user.email());
        UserDto userDto = userService.createUser(user);

        URI userUri = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(userDto.id())
                .toUri();
        log.info("User created successfully – id: {}, email: {}", userDto.id(), userDto.email());
        return ResponseEntity.created(userUri).body(userDto);
    }

    @Operation(summary = "Get pending requests for user", description = "Returns a list of pending requests assigned to the user.")
    @GetMapping(path = "/{id}/pending-requests", produces = MediaType.APPLICATION_JSON_VALUE)
    @IsAdministrator
    public ResponseEntity<List<RequisitionDto>> getPendingRequisitions(@PathVariable Long id) {
        log.info("GET /users/{}/pending-requests", id);
        return ResponseEntity.ok(userService.getPendingRequisitionsForUser(id));
    }

    @Operation(summary = "Delete user", description = "Deletes a user.")
    @IsAdministrator
    @DeleteMapping(path = "/{id}")
    public ResponseEntity<Object> deleteByUserId(@PathVariable Long id,
            @RequestParam(required = false) Long fallbackUserId, @AuthenticationPrincipal User currentUser) {
        log.info("DELETE /users/{} with fallbackUserId: {}", id, fallbackUserId);
        userService.deleteUser(id, fallbackUserId, currentUser);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "List requesters", description = "Retrieves all users with the role REQUESTER.")
    @IsFinanceOfficer
    @GetMapping("/requesters")
    public ResponseEntity<Page<UserDto>> getAllRequesters(Pageable pageable) {
        log.info("GET /users/requesters - page: {}, size: {}", pageable.getPageNumber(), pageable.getPageSize());
        return ResponseEntity.ok(userService.getAllRequesters(pageable));
    }

}
