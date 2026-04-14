package com.veritas.backend;

import com.veritas.backend.common.model.Department;
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
	ApplicationRunner seedData(UserRepository userRepo, PasswordEncoder encoder) {
		return args -> {
			if (userRepo.findByEmail("dev@veritas.com").isEmpty()) {
				User admin = User.builder()
						.name("Veritas Dev")
						.email("dev@veritas.com")
						.passwordHash(encoder.encode("password123"))
						.role(UserRole.ADMINISTRATOR)
						.department(Department.IT)
						.isActive(true)
						.build();
				userRepo.save(admin);

			}
		};
	}

}
