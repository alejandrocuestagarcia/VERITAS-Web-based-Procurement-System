package com.veritas.backend;

import com.veritas.backend.common.model.Department;
import com.veritas.backend.project.entity.Project;
import com.veritas.backend.project.repository.ProjectRepository;
import com.veritas.backend.team.entity.Team;
import com.veritas.backend.team.repository.TeamRepository;
import com.veritas.backend.user.entity.User;
import com.veritas.backend.user.entity.UserRole;
import com.veritas.backend.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

@Slf4j
@SpringBootApplication
public class BackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
	}

	@Bean
	ApplicationRunner seedData(UserRepository userRepo, PasswordEncoder encoder, TeamRepository teamRepo, ProjectRepository projectRepo) {
		return args -> {
			log.info("Starting seed data initialization...");

			Team teamOne = teamRepo.findById(1L).orElseGet(() -> {
				Team team = new Team();
				team.setName("Procurement Alpha");
				team.setDepartment(Department.IT);
				team.setDescription("Handles procurement for the alpha team");
				log.info("Seeded team: {}", team.getName());
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
				log.info("Seeded user: {} (role={})", admin.getEmail(), admin.getRole());
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
				log.info("Seeded user: {} (role={})", finance.getEmail(), finance.getRole());
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
				log.info("Seeded user: {} (role={})", procurement.getEmail(), procurement.getRole());
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
				log.info("Seeded user: {} (role={})", requester.getEmail(), requester.getRole());

			}

			if (projectRepo.count() == 0) {
				Project p1 = Project.builder()
						.name("Apollo Architecture Audit")
						.budget(new BigDecimal("150000"))
						.projectKey("apollo-architecture-audit")
						.startDate(LocalDate.of(2026, 1, 1))
						.endDate(LocalDate.of(2026, 12, 31))
						.team(teamOne)
						.build();

				Project p2 = Project.builder()
						.name("Enterprise Lifecycle Management")
						.projectKey("Second")
						.budget(new BigDecimal("275000"))
						.startDate(LocalDate.of(2026, 3, 15))
						.endDate(LocalDate.of(2027, 6, 1))
						.team(teamOne)
						.build();

				projectRepo.save(p1);
				projectRepo.save(p2);
				log.info("Seeded {} projects", 2);
			}

			log.info("Seed data initialization complete.");
		};
	}

}

