package com.example.cxcapturetest0324;

import android.app.Application;

import cn.jpush.im.android.api.JMessageClient;

public class CXAssistant extends Application {

    @Override
    public void onCreate(){
        super.onCreate();

        JMessageClient.init(getApplicationContext());
        System.out.println("JMessage Initialize");
    }
}
