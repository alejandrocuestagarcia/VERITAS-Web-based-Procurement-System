package com.veritas.backend.util;

import com.veritas.backend.team.entity.Team;
import com.veritas.backend.team.repository.TeamRepository;
import com.veritas.backend.user.entity.User;
import com.veritas.backend.user.entity.UserRole;
import com.veritas.backend.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UserFactory {

    @Autowired private UserRepository userRepository;
    @Autowired private TeamRepository teamRepository;

    public Team createTeam(String name) {
        return teamRepository.save(Team.builder()
                .name(name)
                .description(name + " Team")
                .isActive(true)
                .build());
    }

    public User createUser(String email, Team team, UserRole role) {
        if (team == null) {
            team = createTeam("Team for " + email);
        }
        User user = new User();
        user.setEmail(email);
        user.setName("Test User (" + role + ")");
        user.setPasswordHash("hashed_password");
        user.setRole(role);
        user.setIsActive(true);
        user.setTeam(team);
        return userRepository.save(user);
    }
}
