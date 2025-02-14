package com.arashivision.sdk.demo.activity;

public class IntroModel {
    private int id;
    private String projectName;
    private String companyName;
    private String location;
    private Integer user; // Add user field to store user_id

    // Updated constructor to include user field
    public IntroModel(int id, String projectName, String companyName, String location, Integer user) {
        this.id = id;
        this.projectName = projectName;
        this.companyName = companyName;
        this.location = location;
        this.user = user; // Set the user_id
    }

    public int getId() {
        return id;
    }

    public String getProjectName() {
        return projectName;
    }

    public String getCompanyName() {
        return companyName;
    }

    public String getLocation() {
        return location;
    }

    public Integer getUser() {
        return user; // Getter for user_id
    }
}
