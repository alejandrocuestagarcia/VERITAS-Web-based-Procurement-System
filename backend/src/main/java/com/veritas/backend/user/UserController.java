package com.veritas.backend.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
@Tag(name = "User Module", description = "Management of VERITAS users")
public class UserController {
    @Operation(summary = "List users", description = "Retrieves all users.")
    @GetMapping
    public List<Object> getAllUsers() {
        return List.of();
    }

    @Operation(summary = "Get user", description = "Retrieves a user.")
    @GetMapping("/{id}")
    public List<Object> getUser(@PathVariable Long id) {
        return List.of();
    }

    @Operation(summary = "Edit user", description = "Edits a users basic info.")
    @PatchMapping("/{id}")
    public String editUser(@PathVariable Long id) {
        return "User " + id + " edited";
    }

}
