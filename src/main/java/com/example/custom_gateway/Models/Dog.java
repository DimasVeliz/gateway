package com.example.custom_gateway.Models;

import java.util.ArrayList;

public class Dog {
    private String name;
    private int age;
    private ArrayList<Person> people;
    

    public ArrayList<Person> getPeople() {
        return people;
    }
    public void setPeople(ArrayList<Person> people) {
        this.people = people;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public int getAge() {
        return age;
    }
    public void setAge(int age) {
        this.age = age;
    }
}
