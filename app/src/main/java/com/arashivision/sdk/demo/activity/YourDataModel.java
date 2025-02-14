package com.arashivision.sdk.demo.activity;

public class YourDataModel {
    private Integer id;              // Unique identifier for the building (buildingId)
    private Integer project;         // Unique identifier for the project
    private String image;            // URL or resource ID for the image
    private String totalFloors;      // Total number of floors in the project
    private String noOfEmployees;    // Number of employees in the project
    private String planDetailsUrl;   // URL for the project plan details

    // Constructor
    public YourDataModel(Integer id, Integer project, String image, String totalFloors, String noOfEmployees) {
        this.id = id;
        this.project = project;
        this.image = image;
        this.totalFloors = totalFloors;
        this.noOfEmployees = noOfEmployees;
    }

    // Getter for id (buildingId)
    public Integer getId() {
        return id;
    }

    // Getter for project
    public Integer getProject() {
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
