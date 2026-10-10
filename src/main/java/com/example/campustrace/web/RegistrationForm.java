package com.example.campustrace.web;
import jakarta.validation.constraints.*;
public class RegistrationForm {
    @NotBlank @Size(max=100) public String fullName;
    @NotBlank @Email @Size(max=254) public String email;
    @NotBlank @Size(min=8,max=72) public String password;
    @NotBlank @Pattern(regexp="(?=(?:[^0-9]*[0-9]){7})[+0-9() -]{7,25}") public String contactNumber;
    public String getFullName() { return fullName; }
    public void setFullName(String v) { fullName=v == null ? null : v.trim(); }
    public String getEmail() { return email; }
    public void setEmail(String v) { email=v == null ? null : v.trim(); }
    public String getPassword() { return password; }
    public void setPassword(String v) { password=v; }
    public String getContactNumber() { return contactNumber; }
    public void setContactNumber(String v) { contactNumber=v == null ? null : v.trim(); }
}
