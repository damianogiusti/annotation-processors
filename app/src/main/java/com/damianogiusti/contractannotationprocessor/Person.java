package com.damianogiusti.contractannotationprocessor;

import com.damianogiusti.contractannotation.Contract;
import com.damianogiusti.mappable.Mappable;

import java.util.Date;

/**
 * Created by Damiano Giusti on 20/07/17.
 */
@Contract
@Mappable
public class Person {
    private long id;
    private String name;
    private String surname;
    private int age;
    private Date birthDate;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public Date getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(Date birthDate) {
        this.birthDate = birthDate;
    }
}
