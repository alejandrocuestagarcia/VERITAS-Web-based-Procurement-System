package com.veritas.backend.user.controller;

import com.veritas.backend.config.annotations.IsFinanceOfficer;
import com.veritas.backend.user.dto.UserCreationRequestDto;
import com.veritas.backend.user.dto.UserDto;
import com.veritas.backend.user.dto.UserEditDto;
import com.veritas.backend.user.dto.UserStatsDto;
import com.veritas.backend.user.entity.UserRole;
import com.veritas.backend.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping(path = "/api/v1/users", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "User Module", description = "Management of VERITAS users")
public class UserController {

    private final UserService userService;


    @Operation(summary = "List users", description = "Retrieves all users.")
    @IsFinanceOfficer
    @GetMapping
    public ResponseEntity<Page<UserDto>> getAllUsers(Pageable pageable, @RequestParam(required = false) String search, @RequestParam(required = false)
                                                     UserRole userRole) {
        return ResponseEntity.ok(userService.getAllUsersFiltered(pageable, search, userRole));
    }

    @Operation(summary = "Get user stats", description = "Retrieves stats about the userbase of Veritas")
    @IsFinanceOfficer
    @GetMapping("/stats")
    public ResponseEntity<UserStatsDto> getUserStats() {
        return ResponseEntity.ok(userService.getUserStats());
    }

    @Operation(summary = "Get user", description = "Retrieves a user.")
    @IsFinanceOfficer
    @GetMapping(path = "/{id}", produces = "application/json")
    public ResponseEntity<UserEditDto> getUserByIdForEdit(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserByIdForEdit(id));
    }

    @Operation(summary = "Edit user", description = "Edits a users basic info.")
    @IsFinanceOfficer
    @PatchMapping(path = "/{id}", consumes = "application/json", produces = "application/json")
    public ResponseEntity<UserDto> editUser(@PathVariable Long id, @RequestBody UserEditDto edits) {
        UserDto updated = userService.editUser(id, edits);
        return ResponseEntity.ok(updated);
    }

    @Operation(summary = "Create user", description = "Creates a new user.")
    @IsFinanceOfficer
    @PostMapping(consumes = "application/json", produces = "application/json")
    public ResponseEntity<UserDto> createUser(@Valid @RequestBody UserCreationRequestDto user) {
        UserDto userDto = userService.createUser(user);

        URI userUri = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(userDto.id()).toUri();
        return ResponseEntity.created(userUri).body(userDto);
    }

}
