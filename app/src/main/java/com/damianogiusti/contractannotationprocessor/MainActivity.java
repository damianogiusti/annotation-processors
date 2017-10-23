package com.damianogiusti.contractannotationprocessor;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import com.damianogiusti.mapper.ObjectMapFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        User user = new User();
        user.setUserId(UUID.randomUUID().toString());
        user.setRegistrationDate(new Date());
        user.setPersons(new ArrayList<Person>());
        user.setUserName("Pippo");

        Map<String, Object> userMap = ObjectMapFactory.userToMap(user);
        Log.d(TAG, String.valueOf(userMap.get(UserContract.PERSONS)));
        User pippo = ObjectMapFactory.userFromMap(userMap);
        Log.d(TAG, String.valueOf(pippo.getPersons()));
    }
}
