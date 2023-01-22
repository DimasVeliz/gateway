package com.example.custom_gateway.Models;

public class Person {
    private String dni;
    private String address;

    public void setAddress(String address) {
        this.address = address;
    }


    public String getDni() {
        return dni;
    }


    public void setDni(String dni) {
        this.dni = dni;
    }

    public String getAddress() {
        return address;
    }
}
