package com.example.campustrace.model;

import jakarta.persistence.*;

@Entity
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String itemName;
    @Column(nullable = false, length = 255)
    private String description;
    @Column(nullable = false, length = 10)
    @jakarta.validation.constraints.NotNull
    @jakarta.validation.constraints.Pattern(regexp = "LOST|FOUND")
    private String type;
    @Column(nullable = false, length = 254)
    private String postedBy;
    @Column(nullable = false, length = 100)
    private String postedByName;
    @Column(nullable = false, length = 25)
    private String contactNumber;
    @Column(nullable = true, length = 150)
    private String location;
    @Column(nullable = false, length = 10)
    @jakarta.validation.constraints.NotNull
    @jakarta.validation.constraints.Pattern(regexp = "OPEN|RESOLVED")
    private String status = "OPEN";

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPostedBy() {
        return postedBy;
    }

    public void setPostedBy(String postedBy) {
        this.postedBy = postedBy;
    }

    public String getPostedByName() {
        return postedByName;
    }

    public void setPostedByName(String postedByName) {
        this.postedByName = postedByName;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
