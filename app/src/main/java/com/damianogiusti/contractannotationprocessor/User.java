package com.damianogiusti.contractannotationprocessor;

import com.damianogiusti.contractannotation.Contract;
import com.damianogiusti.mappable.Mappable;

import java.util.Date;
import java.util.List;

/**
 * Created by Damiano Giusti on 19/07/17.
 */
@Contract
@Mappable
public class User {

    private String userId;
    private String userName;
    private Date registrationDate;
    private List<Person> persons;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Date getRegistrationDate() {
        return registrationDate;
    }

    public void setRegistrationDate(Date registrationDate) {
        this.registrationDate = registrationDate;
    }

    public List<Person> getPersons() {
        return persons;
    }

    public void setPersons(List<Person> persons) {
        this.persons = persons;
    }
}
