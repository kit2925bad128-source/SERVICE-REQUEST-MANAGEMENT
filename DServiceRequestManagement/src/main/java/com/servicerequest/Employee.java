package com.servicerequest;

public class Employee {
    private int employeeId;
    private String name;
    private String email;
    private String department;

    public Employee() {
    }

    public Employee(int employeeId, String name, String email, String department) {
        this.employeeId = employeeId;
        this.name = name;
        this.email = email;
        this.department = department;
    }

    public int getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(int employeeId) {
        this.employeeId = employeeId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }
}