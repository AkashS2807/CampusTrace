package com.example.campustrace.config;

import com.example.campustrace.repository.UserRepository;
import java.util.Locale;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    UserDetailsService userDetailsService(UserRepository users) {
        return email -> {
            var user = users.findByEmailIgnoreCase(email.trim().toLowerCase(Locale.ROOT));
            if (user == null || user.getPassword() == null ||
                    !user.getPassword().matches("^\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}$")) {
                throw new UsernameNotFoundException("Invalid credentials");
            }
            if (!"USER".equals(user.getRole()) && !"ADMIN".equals(user.getRole())) {
                throw new UsernameNotFoundException("Invalid credentials");
            }
            return org.springframework.security.core.userdetails.User.withUsername(user.getEmail())
                    .password(user.getPassword()).roles(user.getRole()).build();
        };
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/role", "/admin", "/userChoice", "/newUser",
                        "/saveUser", "/registeredUser", "/css/**", "/error").permitAll()
                .requestMatchers("/allUsers", "/manageItems", "/deleteItem/**").hasRole("ADMIN")
                .anyRequest().authenticated())
            .formLogin(form -> form.loginPage("/registeredUser").loginProcessingUrl("/userLogin")
                .usernameParameter("email").defaultSuccessUrl("/dashboard", true)
                .failureUrl("/registeredUser?error").permitAll())
            .logout(logout -> logout.logoutUrl("/logout").logoutSuccessUrl("/?logout")
                .invalidateHttpSession(true).deleteCookies("JSESSIONID"));
        return http.build();
    }
}
