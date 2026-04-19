package com.SocializerAI;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.CommandLineRunner;
import com.SocializerAI.repository.UserRepository;
import com.SocializerAI.model.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Optional;

@SpringBootApplication
public class SocializerAiApplication {
	public static void main(String[] args) {
		SpringApplication.run(SocializerAiApplication.class, args);
	}

	@Bean
	public CommandLineRunner createAdmin(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		return args -> {
			String adminEmail = System.getenv().getOrDefault("ADMIN_EMAIL", "adminhaikal@gmail.com");
			String adminPassword = System.getenv().getOrDefault("ADMIN_PASSWORD", "admin2003");
			Optional<User> existing = userRepository.findByEmail(adminEmail);
			if (existing.isPresent()) {
				System.out.println("Admin user already exists: " + adminEmail);
				return;
			}

			User admin = new User();
			admin.setUsername("admin");
			admin.setEmail(adminEmail);
			admin.setFullName("Administrator");
			admin.setPasswordHash(passwordEncoder.encode(adminPassword));
			admin.setIsActive(true);
			admin.setIsVerified(true);
			admin.setRoles("ADMIN,USER");

			userRepository.save(admin);
			System.out.println("Created admin user: " + adminEmail);
		};
	}
}
