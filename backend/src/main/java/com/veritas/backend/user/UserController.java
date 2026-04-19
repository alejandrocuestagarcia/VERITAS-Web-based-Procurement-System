package com.veritas.backend.user;

import com.veritas.backend.config.annotations.IsFinanceOfficer;
import com.veritas.backend.user.dto.UserDto;
import com.veritas.backend.user.dto.UserEditDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
@Tag(name = "User Module", description = "Management of VERITAS users")
public class UserController {
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
        return new UserDto();
    }

    @Operation(summary = "Edit user", description = "Edits a users basic info.")
    @IsFinanceOfficer
    @PatchMapping("/{id}")
    public UserDto editUser(@PathVariable Long id, @RequestBody UserEditDto edits) {
        return new UserDto();
    }
}
