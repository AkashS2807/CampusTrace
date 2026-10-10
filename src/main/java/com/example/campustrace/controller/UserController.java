package com.example.campustrace.controller;

import com.example.campustrace.entity.User;
import com.example.campustrace.repository.UserRepository;
import com.example.campustrace.web.RegistrationForm;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class UserController {
    private final UserRepository users;
    private final PasswordEncoder passwords;
    public UserController(UserRepository users, PasswordEncoder passwords) {
        this.users=users; this.passwords=passwords;
    }
    @GetMapping("/role") public String rolePage() { return "role"; }
    @GetMapping("/admin") public String adminPage() { return "adminLogin"; }
    @GetMapping("/userChoice") public String userChoice() { return "userChoice"; }
    @GetMapping("/registeredUser") public String login() { return "login"; }
    @GetMapping("/newUser") public String register(Model model) {
        model.addAttribute("form",new RegistrationForm()); return "register";
    }
    @PostMapping("/saveUser")
    public String saveUser(@Valid @ModelAttribute("form") RegistrationForm form,
            BindingResult errors, Model model, RedirectAttributes flash) {
        String email=form.email == null ? "" : form.email.trim().toLowerCase(Locale.ROOT);
        if (form.password != null && form.password.getBytes(StandardCharsets.UTF_8).length > 72) {
            errors.rejectValue("password", "length", "Password must be at most 72 UTF-8 bytes.");
        }
        if (errors.hasErrors()) {
            model.addAttribute("error","Check required fields, email, phone number, and password (8?72 bytes).");
            return "register";
        }
        if (users.findByEmailIgnoreCase(email) != null) {
            model.addAttribute("error","This email is already registered. Please sign in."); return "register";
        }
        User user=new User(); user.setFullName(form.fullName.trim()); user.setEmail(email);
        user.setPassword(passwords.encode(form.password)); user.setContactNumber(form.contactNumber.trim());
        try { users.saveAndFlush(user); }
        catch (DataIntegrityViolationException duplicate) {
            if (users.findByEmailIgnoreCase(email) == null) { throw duplicate; }
            model.addAttribute("error","This email is already registered. Please sign in."); return "register";
        }
        flash.addFlashAttribute("success","Account created. Please sign in.");
        return "redirect:/registeredUser";
    }
}
