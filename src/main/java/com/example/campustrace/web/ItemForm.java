package com.example.campustrace.web;
import jakarta.validation.constraints.*;
public class ItemForm {
    @NotBlank @Size(max=100) public String itemName;
    @NotBlank @Size(max=255) public String description;
    @Size(max=150) public String location;
    @NotBlank @Pattern(regexp="(?=(?:[^0-9]*[0-9]){7})[+0-9() -]{7,25}") public String contactNumber;
    public String getItemName() { return itemName; }
    public void setItemName(String v) { itemName=v == null ? null : v.trim(); }
    public String getDescription() { return description; }
    public void setDescription(String v) { description=v == null ? null : v.trim(); }
    public String getLocation() { return location; }
    public void setLocation(String v) { location=v == null ? null : v.trim(); }
    public String getContactNumber() { return contactNumber; }
    public void setContactNumber(String v) { contactNumber=v == null ? null : v.trim(); }
}
