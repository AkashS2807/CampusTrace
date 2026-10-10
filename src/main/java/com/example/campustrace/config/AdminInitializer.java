package com.example.campustrace.config;

import com.example.campustrace.entity.User;
import com.example.campustrace.repository.UserRepository;
import jakarta.validation.Validator;
import com.example.campustrace.web.RegistrationForm;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Explicit, opt-in creation only: never promotes or rewrites an existing account. */
@Component
public class AdminInitializer implements ApplicationRunner {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final Environment env;
    private final Validator validator;
    public AdminInitializer(UserRepository users, PasswordEncoder encoder, Environment env, Validator validator) {
        this.users=users; this.encoder=encoder; this.env=env; this.validator=validator;
    }
    @Override public void run(ApplicationArguments args) {
        if (!env.getProperty("ADMIN_INIT_ENABLED",Boolean.class,false)) { return; }
        String email=env.getProperty("ADMIN_EMAIL", "").trim().toLowerCase(Locale.ROOT);
        String password=env.getProperty("ADMIN_PASSWORD", "");
        RegistrationForm form=new RegistrationForm(); form.fullName="Portal Administrator";
        form.email=email; form.password=password; form.contactNumber="0000000000";
        if (!validator.validate(form).isEmpty() || password.getBytes(StandardCharsets.UTF_8).length>72) {
            throw new IllegalStateException("Administrator initialization requires a valid email and an 8?72 byte password.");
        }
        User existing=users.findByEmailIgnoreCase(email);
        if (existing != null) {
            if (!"ADMIN".equals(existing.getRole())) {
                throw new IllegalStateException("Administrator initialization cannot promote an existing student account.");
            }
            return;
        }
        User user=new User(); user.setFullName(form.fullName); user.setEmail(email);
        user.setPassword(encoder.encode(password)); user.setContactNumber(form.contactNumber); user.setRole("ADMIN");
        users.saveAndFlush(user);
    }
}
