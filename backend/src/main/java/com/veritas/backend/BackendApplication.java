package com.veritas.backend;

import com.veritas.backend.common.model.Department;
import com.veritas.backend.team.entity.Team;
import com.veritas.backend.team.repository.TeamRepository;
import com.veritas.backend.user.entity.User;
import com.veritas.backend.user.entity.UserRole;
import com.veritas.backend.user.repository.UserRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
public class BackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
	}

	@Bean
	ApplicationRunner seedData(UserRepository userRepo, PasswordEncoder encoder, TeamRepository teamRepo) {
		return args -> {

			teamRepo.findById(1L).orElseGet(() -> {
				Team team = new Team();
				team.setName("Procurement Alpha");
				team.setDepartment(Department.IT);
				return teamRepo.save(team);
			});

			if (userRepo.findByEmail("admin@veritas.com").isEmpty()) {
				User admin = User.builder()
						.name("Veritas Admin")
						.email("admin@veritas.com")
						.passwordHash(encoder.encode("password123"))
						.role(UserRole.ADMINISTRATOR)
						.department(Department.IT)
						.isActive(true)
						.build();
				userRepo.save(admin);
			}
			if (userRepo.findByEmail("finance@veritas.com").isEmpty()) {
				User finance = User.builder()
						.name("Veritas Finance")
						.email("finance@veritas.com")
						.passwordHash(encoder.encode("password123"))
						.role(UserRole.FINANCE_OFFICER)
						.department(Department.IT)
						.isActive(true)
						.build();
				userRepo.save(finance);
			}
			if (userRepo.findByEmail("procurement@veritas.com").isEmpty()) {
				User procurement = User.builder()
						.name("Veritas Procurement")
						.email("procurement@veritas.com")
						.passwordHash(encoder.encode("password123"))
						.role(UserRole.PROCUREMENT_OFFICER)
						.department(Department.IT)
						.isActive(true)
						.build();
				userRepo.save(procurement);
			}
			if (userRepo.findByEmail("requester@veritas.com").isEmpty()) {
				User requester = User.builder()
						.name("Veritas Requester")
						.email("requester@veritas.com")
						.passwordHash(encoder.encode("password123"))
						.role(UserRole.REQUESTER)
						.department(Department.IT)
						.isActive(true)
						.build();
				userRepo.save(requester);
			}
		};
	}

}
