package com.arashivision.sdk.demo.activity;

public class YourDataModel {
    private int project;          // Unique identifier for the project
    private String image;         // URL or resource ID for the image
    private String totalFloors;   // Total number of floors in the project
    private String noOfEmployees; // Number of employees in the project
    private String planDetailsUrl; // URL for the project plan details

    // Constructor
    public YourDataModel(int project, String image, String totalFloors, String noOfEmployees) {
        this.project = project;
        this.image = image;
        this.totalFloors = totalFloors;
        this.noOfEmployees = noOfEmployees;
    }

    // Getter for project
    public int getProject() {
        return project;
    }

    // Getter for image
    public String getImage() {
        return image;
    }

    // Getter for totalFloors
    public String getTotalFloors() {
        return totalFloors;
    }

    // Getter for noOfEmployees
    public String getNoOfEmployees() {
        return noOfEmployees;
    }

    // Getter for planDetailsUrl
    public String getPlanDetailsUrl() {
        return planDetailsUrl;
    }

    // Setter for planDetailsUrl
    public void setPlanDetailsUrl(String planDetailsUrl) {
        this.planDetailsUrl = planDetailsUrl;
    }
}
