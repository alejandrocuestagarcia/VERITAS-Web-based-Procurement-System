package com.veritas.backend.user;

import com.veritas.backend.user.dto.UserCreationRequestDto;
import com.veritas.backend.config.annotations.IsFinanceOfficer;
import com.veritas.backend.user.dto.UserDto;
import com.veritas.backend.user.dto.UserEditDto;
import com.veritas.backend.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "User Module", description = "Management of VERITAS users")
public class UserController {

    private final UserService userService;

    @Operation(summary = "List users", description = "Retrieves all users.")
    @IsFinanceOfficer
    @GetMapping
    public List<UserDto> getAllUsers() {
        return List.of();
    }

    @Operation(summary = "Get user", description = "Retrieves a user.")
    @IsFinanceOfficer
    @GetMapping("/{id}")
    public UserDto getUser(@PathVariable Long id) {
        return null;
    }

    @Operation(summary = "Edit user", description = "Edits a users basic info.")
    @IsFinanceOfficer
    @PatchMapping("/{id}")
    public ResponseEntity<UserDto> editUser(@PathVariable Long id, @RequestBody UserEditDto edits) {
        return null;
    }

    @Operation(summary = "Create user", description = "Creates a new user.")
    @IsFinanceOfficer
    @PostMapping(consumes = "application/json", produces = "application/json")
    public ResponseEntity<UserDto> createUser(@Valid @RequestBody UserCreationRequestDto user) {
        UserDto userDto = userService.createUser(user);

        URI userUri = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(userDto.id())
                .toUri();
        return ResponseEntity.created(userUri).body(userDto);
    }

}
