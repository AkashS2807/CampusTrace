package com.example.campustrace;

import com.example.campustrace.config.AdminInitializer;
import com.example.campustrace.entity.User;
import com.example.campustrace.repository.UserRepository;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminInitializerTests {
    @Test void disabledInitializerDoesNotTouchAccounts() {
        UserRepository users=mock(UserRepository.class);
        run(users,new MockEnvironment()); verifyNoInteractions(users);
    }
    @Test void enabledInitializerCreatesHashedAdmin() {
        UserRepository users=mock(UserRepository.class);
        run(users,enabled());
        var captor=org.mockito.ArgumentCaptor.forClass(User.class);
        verify(users).saveAndFlush(captor.capture());
        User admin=captor.getValue();
        assertThat(admin.getEmail()).isEqualTo("admin@example.test");
        assertThat(admin.getRole()).isEqualTo("ADMIN");
        assertThat(new BCryptPasswordEncoder().matches("DemoAdmin123!",admin.getPassword())).isTrue();
    }
    @Test void initializerRefusesToPromoteExistingStudent() {
        UserRepository users=mock(UserRepository.class); User existing=new User();
        when(users.findByEmailIgnoreCase("admin@example.test")).thenReturn(existing);
        assertThatThrownBy(() -> run(users,enabled())).isInstanceOf(IllegalStateException.class);
        verify(users,never()).saveAndFlush(any()); assertThat(existing.getRole()).isEqualTo("USER");
    }
    @Test void existingAdminIsLeftUnchanged() {
        UserRepository users=mock(UserRepository.class); User existing=new User(); existing.setRole("ADMIN"); existing.setPassword("unchanged");
        when(users.findByEmailIgnoreCase("admin@example.test")).thenReturn(existing);
        run(users,enabled()); verify(users,never()).saveAndFlush(any()); assertThat(existing.getPassword()).isEqualTo("unchanged");
    }
    @Test void invalidConfigurationCannotCreateAdmin() {
        UserRepository users=mock(UserRepository.class);
        assertThatThrownBy(() -> run(users,enabled().withProperty("ADMIN_PASSWORD","short"))).isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(users);
    }
    private MockEnvironment enabled() {
        return new MockEnvironment().withProperty("ADMIN_INIT_ENABLED","true")
            .withProperty("ADMIN_EMAIL"," ADMIN@example.test ").withProperty("ADMIN_PASSWORD","DemoAdmin123!");
    }
    private void run(UserRepository users,MockEnvironment env) {
        try (var factory=Validation.buildDefaultValidatorFactory()) {
            new AdminInitializer(users,new BCryptPasswordEncoder(),env,factory.getValidator()).run(null);
        }
    }
}
