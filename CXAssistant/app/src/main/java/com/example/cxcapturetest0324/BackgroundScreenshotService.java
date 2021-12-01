package com.example.cxcapturetest0324;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.WindowManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Timer;
import java.util.TimerTask;

import cn.jpush.im.android.api.JMessageClient;
import cn.jpush.im.api.BasicCallback;

//import android.support.v4.os.AsyncTaskCompat;


public class BackgroundScreenshotService extends Service {
    private MediaProjection mMediaProjection;  //MediaProjection用于获取屏幕内容
    private VirtualDisplay mVirtualDisplay;  //

    private static Intent mResultData = null;


    private ImageReader mImageReader;
    private WindowManager mWindowManager;
    private WindowManager.LayoutParams mLayoutParams;
    // private GestureDetector mGestureDetector;

    // private ImageView mFloatView;

    private int mScreenWidth;
    private int mScreenHeight;
    private int mScreenDensity;

    private Timer timer;

    private Handler timerHandler = new Handler(){
        @Override
        public void handleMessage(Message msg){
            if(msg.what==0){
                startScreenShot();
            }
        }
    };



    public BackgroundScreenshotService() {
    }



    @Override
    public void onCreate() {
        super.onCreate();

        System.out.println("BackgroundScreenshotSerivce started");

        initScreen();

        createImageReader();

        initTimer();

        //startScreenShot();

        //timer=new Timer(true);
        //timer.schedule(new timerTask(),5000,10000);

    }




    public static Intent newIntent(Context context, Intent mResultData) {

        Intent intent = new Intent(context, BackgroundScreenshotService.class);

        if (mResultData != null) {
            intent.putExtras(mResultData);
        }
        return intent;
    }



    public static Intent getResultData() {
        return mResultData;
    }

    public static void setResultData(Intent mResultData) {
        BackgroundScreenshotService.mResultData = mResultData;
        System.out.println("mResultData received");
    }

    private void initScreen(){
        mLayoutParams = new WindowManager.LayoutParams();
        mWindowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);

        DisplayMetrics metrics = new DisplayMetrics();
        mWindowManager.getDefaultDisplay().getMetrics(metrics);
        mScreenDensity = metrics.densityDpi;
        mScreenWidth = metrics.widthPixels;
        mScreenHeight = metrics.heightPixels;

        mLayoutParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        mLayoutParams.format = PixelFormat.RGBA_8888;
        // 设置Window flag
        mLayoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        mLayoutParams.gravity = Gravity.LEFT | Gravity.TOP;
        mLayoutParams.x = mScreenWidth;
        mLayoutParams.y = 100;
        mLayoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        mLayoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;

    }

    private void initTimer(){
        timer=new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                Message message = new Message();
                message.what=0;
                timerHandler.sendMessage(message);
            }
        },1000,3000);


    }

    //4：触发startScreenShot
    public void startScreenShot() {

       // mFloatView.setVisibility(View.GONE);
        System.out.println("startScreenShot");
        Handler handler1 = new Handler();
        handler1.postDelayed(new Runnable() {
            public void run() {
                //start virtual
                //5
                startVirtual();
            }
        }, 5);

        handler1.postDelayed(new Runnable() {
            public void run() {
                //capture the screen
                //6
                startCapture();

            }
        }, 30);
    }

    //3：创建ImageReader实例
    private void createImageReader() {

        mImageReader = ImageReader.newInstance(mScreenWidth, mScreenHeight, PixelFormat.RGBA_8888, 1);

    }

    //5.1：通过virtualDisplay获取当前屏幕内容
    public void startVirtual() {
        System.out.println("startVirtual");
        if (mMediaProjection != null) {
            //5.4
            virtualDisplay();
        } else {
            //5.2
            setUpMediaProjection();
            virtualDisplay();
        }
    }

    //5.2：如果还没有获取MediaProjection实例的话，用从MainActivity中授权时传来的mResultData来获取
    public void setUpMediaProjection() {
        if (mResultData == null) {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            startActivity(intent);
        } else {
            //5.3
            mMediaProjection = getMediaProjectionManager().getMediaProjection(Activity.RESULT_OK, mResultData);
            System.out.println("mediaProjection set");
        }
    }

    //5.3：如果MediaProjectionManager还没有的话，在这里实例化一下
    private MediaProjectionManager getMediaProjectionManager() {

        return (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
    }

    //5.4：通过MediaProjection实例获得VirtualDisplay实例，其中参数用到了ImageReader实例来获得surface，即将屏幕的内容传入
    private void virtualDisplay() {
        mVirtualDisplay = mMediaProjection.createVirtualDisplay("screen-mirror",
                mScreenWidth, mScreenHeight, mScreenDensity, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mImageReader.getSurface(), null, null);
        System.out.println("virtualDisplay set");
    }


    //6：从ImageReader中获得上一个屏幕图像
    private void startCapture() {

        Image image = mImageReader.acquireLatestImage();

        if (image == null) {
            //6.1：如果图像为空，回去截图
            startScreenShot();
        } else {
            //6.2：如果图像非空，就实例化一个7：SaveTask来保存图片
            SaveTask mSaveTask = new SaveTask();
            //6.3：传入image，执行SaveTask实例
            AsyncTaskCompat.executeParallel(mSaveTask, image);
            System.out.println("start SaveTask \n \n");
        }
    }




    //7：用来保存图像的任务，将Image实例转化为Bitmap以位图的形式存储
    public class SaveTask extends AsyncTask<Image, Void, Bitmap> {

        @Override
        protected Bitmap doInBackground(Image... params) {

            if (params == null || params.length < 1 || params[0] == null) {

                return null;
            }

            //7.1：从参数中获得image
            Image image = params[0];

            //7.2：将image转化为imagePlane，然后写入buffer
            int width = image.getWidth();
            int height = image.getHeight();
            final Image.Plane[] planes = image.getPlanes();
            final ByteBuffer buffer = planes[0].getBuffer();

            //每个像素的间距
            int pixelStride = planes[0].getPixelStride();
            //总的间距
            int rowStride = planes[0].getRowStride();
            int rowPadding = rowStride - pixelStride * width;

            //7.3：从buffer创建位图
            Bitmap bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888);
            bitmap.copyPixelsFromBuffer(buffer);
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height);


            //7.4：关闭图像实例，不然下次使用会报错
            image.close();


            //7.5：新建图像文件实例并保存
            File fileImage = null;
            if (bitmap != null) {
                try {
                    fileImage = new File(ScreenshotFileUtil.getScreenShotsName(getApplicationContext()));
                    if (!fileImage.exists()) {
                        fileImage.createNewFile();
                    }
                    FileOutputStream out = new FileOutputStream(fileImage);
                    if (out != null) {
                        bitmap.compress(Bitmap.CompressFormat.PNG, 10, out);
                        out.flush();
                        out.close();

                        //下面时用于触发截图完成事件的部分，本程序中用不到
                        //Intent media = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                        //Uri contentUri = Uri.fromFile(fileImage);
                        //media.setData(contentUri);
                        //sendBroadcast(media);
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    fileImage = null;
                } catch (IOException e) {
                    e.printStackTrace();
                    fileImage = null;
                }
            }

            if (fileImage != null) {
                System.out.println("fileImage is not null");
                try {
                    cn.jpush.im.android.api.model.Message msg = JMessageClient.createSingleImageMessage("test",fileImage);

                    msg.setOnSendCompleteCallback(new BasicCallback() {
                        @Override
                        public void gotResult(int responseCode, String responseDesc) {
                            if (responseCode == 0) {
                                //消息发送成功
                                System.out.println("message sent");
                            } else {
                                //消息发送失败
                                System.out.println("send message failed");
                            }
                        }
                    });

                    JMessageClient.sendMessage(msg);

                } catch (FileNotFoundException e) {
                    System.out.println("imageFile not found");
                    e.printStackTrace();
                }

                return bitmap;
            }
            return null;
        }

//        @Override
 //       protected void onPostExecute(Bitmap bitmap) {
//            super.onPostExecute(bitmap);
//            //预览图片
//            if (bitmap != null) {
//
//                ((ScreenCaptureApplication) getApplication()).setmScreenCaptureBitmap(bitmap);
//                Log.e("ryze", "获取图片成功");
//                startActivity(PreviewPictureActivity.newIntent(getApplicationContext()));
//            }
//
//            mFloatView.setVisibility(View.VISIBLE);

 //       }
    }

    public class timerTask extends TimerTask{
        @Override
        public void run(){
            System.out.println("timer task");
            startScreenShot();
        }
    }


    private void tearDownMediaProjection() {
        if (mMediaProjection != null) {
            mMediaProjection.stop();
            mMediaProjection = null;
        }
    }

    private void stopVirtual() {
        if (mVirtualDisplay == null) {
            return;
        }
        mVirtualDisplay.release();
        mVirtualDisplay = null;
    }

    @Override
    public void onDestroy() {
        // to remove mFloatLayout from windowManager
        super.onDestroy();
        //if (mFloatView != null) {
        //    mWindowManager.removeView(mFloatView);
        //}
        stopVirtual();

        tearDownMediaProjection();

        timer.cancel();

        System.out.println("service stopped");
    }





    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
