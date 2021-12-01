package com.example.cxcapturetest0324;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import cn.jpush.im.android.api.JMessageClient;
import cn.jpush.im.api.BasicCallback;

//特别注意！！！程序中没有问系统要存储权限，如果安装好程序之后不手动去系统设置里面设置一下会不能保存截图，抛出no such file or directory
public class MainActivity extends AppCompatActivity {

    public static final int REQUEST_MEDIA_PROJECTION = 18;

    Button btn_screenshot;
    Button btn_stop;
    Button btn_login;
    Button btn_location;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        System.out.println("MainActivity created");

        init_view();

        //JMessageClient.init(getApplicationContext());
    }

    //1：通过button触发申请截图权限
    public void requestCapturePermission() {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            //5.0 之后才允许使用屏幕截图

            return;
        }

        //创建一个MediaProjectionManager，通过startActivityForResult来申请获得mediaProjection
        MediaProjectionManager mediaProjectionManager = (MediaProjectionManager)
                getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        startActivityForResult(
                mediaProjectionManager.createScreenCaptureIntent(),
                REQUEST_MEDIA_PROJECTION);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //2：当得到同意的结果时，将BackgroundScreenshotService的mResultData设置为返回来的Intent，然后启动BackgroundScreenshotService
        switch (requestCode) {
            case REQUEST_MEDIA_PROJECTION:

                if (resultCode == RESULT_OK && data != null) {
                    BackgroundScreenshotService.setResultData(data);
                    startService(new Intent(getApplicationContext(), BackgroundScreenshotService.class));
                    System.out.println("allowed");
                }
                break;

        }


    }

    public void login(){
        JMessageClient.login("wangshuo", "123456", new BasicCallback() {
            @Override
            public void gotResult(int i, String s) {
                if(i==0) {
                    System.out.println("login as test");
                } else {
                    System.out.println("login failed");
                }
            }
        });
    }



    public void location(){

    }


    public void init_view(){
        System.out.println("init view");
        btn_screenshot=(Button)findViewById(R.id.btn_screenshot);
        btn_stop=(Button)findViewById(R.id.btn_stop);
        btn_login=(Button)findViewById(R.id.btn_login);
        btn_location=(Button)findViewById(R.id.btn_location);


        btn_login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                System.out.println("btn_login pressed");
                login();

            }
        });

        btn_screenshot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                requestCapturePermission();
                System.out.println("btn_screenshot pressed");
            }
        });

        btn_stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopService(new Intent(getApplicationContext(), BackgroundScreenshotService.class));
                System.out.println("btn_stop pressed");
            }
        });

        btn_location.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                System.out.println("btn_location pressed");
                location();
            }
        });
    }
}