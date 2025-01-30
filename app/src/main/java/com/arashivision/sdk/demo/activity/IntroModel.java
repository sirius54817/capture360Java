package com.arashivision.sdk.demo.activity;

public class IntroModel {
    private int id;
    private String projectName;
    private String companyName;
    private String location;

    public IntroModel(int id, String projectName, String companyName, String location) {
        this.id = id;
        this.projectName = projectName;
        this.companyName = companyName;
        this.location = location;
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
}
