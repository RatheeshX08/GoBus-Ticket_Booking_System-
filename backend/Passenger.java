package com.busbooking.model;

/**
 * Passenger entity storing personal contact details and travel demographics.
 */
public class Passenger {
    private int passengerId;
    private String fullName;
    private int age;
    private String gender;      // Male, Female, Other
    private String phone;
    private String email;

    public Passenger() {
    }

    public Passenger(String fullName, int age, String gender, String phone, String email) {
        this.fullName = fullName;
        this.age = age;
        this.gender = gender;
        this.phone = phone;
        this.email = email;
    }

    public int getPassengerId() {
        return passengerId;
    }

    public void setPassengerId(int passengerId) {
        this.passengerId = passengerId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public String toString() {
        return fullName + " (" + age + "y, " + gender + ") - " + phone;
    }
}
