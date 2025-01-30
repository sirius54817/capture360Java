package com.arashivision.sdk.demo.model;

public class YourDataModel1 {
    private int id; // Assuming this is an int
    private String name; // Assuming this is a String
    private String data; // Assuming this is a String
    private boolean someBoolean; // Assuming this is a boolean

    // Constructor
    public YourDataModel1(int id, String name, String data, boolean someBoolean) {
        this.id = id;
        this.name = name;
        this.data = data;
        this.someBoolean = someBoolean;
    }

    // Getters
    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getData() {
        return data;
    }

    public boolean isSomeBoolean() {
        return someBoolean;
    }

    // Setters
    public void setId(int id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setData(String data) {
        this.data = data;
    }

    public void setSomeBoolean(boolean someBoolean) {
        this.someBoolean = someBoolean;
    }

    // Override toString() method for better debugging and logging
    @Override
    public String toString() {
        return "YourDataModel1{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", data='" + data + '\'' +
                ", someBoolean=" + someBoolean +
                '}';
    }
}
