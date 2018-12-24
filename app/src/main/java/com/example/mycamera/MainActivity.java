package com.example.mycamera;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.net.wifi.aware.DiscoverySession;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.StrictMode;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static android.hardware.camera2.CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE;
import android.view.OrientationEventListener;

public class MainActivity extends AppCompatActivity{
    private static final String TAG = "MainActivity";
    private TextureView tv;
    private Button btnpz;
    private Button btnlp;
    private Button btnipvi;
    private Button btnipqd;
    private Button btnipqx;
    private RelativeLayout ip_vi;
    private EditText ip_edit;
    private boolean LP_FLAG=true;
    private boolean IPVI_FLAG=true;
    private boolean CONNET_FLAG=false;
    private String mCameraId = "0";//摄像头id（通常0代表后置摄像头，1代表前置摄像头）
    private final int RESULT_CODE_CAMERA=1;//判断是否有拍照权限的标识码
    private CameraDevice cameraDevice;
    private CameraCaptureSession mPreviewSession;
    private CaptureRequest.Builder mCaptureRequestBuilder,captureRequestBuilder;
    private CaptureRequest mCaptureRequest;
    private ImageReader imageReader;
    private int height=0,width=0;
    private Size previewSize;
    private ImageView iv;
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    private String mydirpath;
    private Socket socket;
    private String ip = "10.246.19.223";//电脑ip地址，在本地连接的属性下查找本机地址
    private int port = 4652;//端口号
    File file;
    static
    {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //保持屏幕常量
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        tv = (TextureView) findViewById(R.id.tv);
        btnpz = (Button) findViewById(R.id.btn);
        btnlp=findViewById(R.id.btn2);
        btnipvi=findViewById(R.id.btn3);
        btnipqd=findViewById(R.id.btn_ip_qd);
        btnipqx=findViewById(R.id.btn_ip_qx);
        iv= (ImageView) findViewById(R.id.iv);
        ip_vi=(RelativeLayout)findViewById(R.id.ip_vi);
        ip_edit=findViewById(R.id.ip_edit_text);


        //隐藏连接设置界面
        ip_vi.setVisibility(View.GONE);
        //拍照
        btnpz.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePicture();
                new Thread(new CustomThread1()).start();
            }
        });
        //连拍
        btnlp.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                if(LP_FLAG)
                {
                    handler.postDelayed(task,0);
                    LP_FLAG=false;
                    btnlp.setText("正在连拍");
                }
                else {
                    handler.removeCallbacks(task);
                    LP_FLAG=true;
                    btnlp.setText("连拍");
                }

            }
        });

        //设置界面显示
        btnipvi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(IPVI_FLAG)
                {
                    btnipvi.setText("上传设置");
                    ip_vi.setVisibility(View.VISIBLE);
                    btnlp.setEnabled(false);
                    btnpz.setEnabled(false);
                    btnlp.setText("连拍");
                    handler.removeCallbacks(task);
                    IPVI_FLAG=false;
                }
                else {
                    btnipvi.setText("上传设置");
                    ip_vi.setVisibility(View.GONE);
                    handler.removeCallbacks(task);
                    btnlp.setEnabled(true);
                    btnpz.setEnabled(true);
                    IPVI_FLAG=true;
                }
            }
        });
        //确定更改IP地址
        btnipqd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!ip_edit.getText().toString().equals(""))
                {
                    btnipqx.setText("保存");
                    btnipqx.setEnabled(true);
                    ip=ip_edit.getText().toString();
                    ip_edit.setHint("输入服务端IP，当前"+ip);
                    ip_edit.setText("");
                    btnipvi.performClick();
                }
            }
        });
        //初始化ip地址
        ipCreat();
        ip_edit.setHint("输入服务端IP，当前"+ip);
        btnipqx.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               // ip=ip_edit.getText().toString();
                ip=ip_edit.getText().toString();
                btnipqx.setText("已保存");
                btnipqx.setEnabled(false);
                File file = null;
                String textPath;
                textPath=Environment.getExternalStorageDirectory().getPath() + "/myCameraPhoto";
                try {
                    file = new File(textPath + '/'+"ip.txt");
                    if (file.exists()) {
                        file.delete();
                    }
                        //写入ip
                    try {
                            FileWriter fw = new FileWriter(textPath + '/' + "ip.txt");
                            File f = new File(textPath + '/' + "ip.txt");
                            fw.write(ip);
                            Log.i(TAG, "写入了某个话！");
                            FileOutputStream os = new FileOutputStream(f);
                            DataOutputStream out = new DataOutputStream(os);
                            out.writeShort(2);
                            out.writeUTF("");
                            System.out.println(out);
                            fw.flush();
                            fw.close();
                            System.out.println(fw);
                        } catch (Exception e) {
                        }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        //设置TextureView监听
        tv.setSurfaceTextureListener(surfaceTextureListener);

      //  handler.postDelayed(task,1000); //延时1s调用


    }

    private void ipCreat(){
        File file = null;
        String textPath;
        textPath=Environment.getExternalStorageDirectory().getPath() + "/myCameraPhoto";
        try {
            file = new File(textPath + '/'+"ip.txt");
            if (!file.exists()) {
                //写入ip
                try {
                    FileWriter fw = new FileWriter(textPath + '/' + "ip.txt");
                    File f = new File(textPath + '/' + "ip.txt");
                    fw.write("10.246.19.223");
                    Log.i(TAG, "写入了某个话！");
                    FileOutputStream os = new FileOutputStream(f);
                    DataOutputStream out = new DataOutputStream(os);
                    out.writeShort(2);
                    out.writeUTF("");
                    System.out.println(out);
                    fw.flush();
                    fw.close();
                    System.out.println(fw);
                } catch (Exception e) {
                }
            }
            else {
                ip=readIp();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String readIp() {
        String textPath;
        textPath=Environment.getExternalStorageDirectory().getPath() + "/myCameraPhoto";
        StringBuffer sb = new StringBuffer();
        File file = new File(textPath + '/'+"ip.txt");
        try {
            FileInputStream fis = new FileInputStream(file);
            int c;
            while ((c = fis.read()) != -1) {
                sb.append((char) c);
            }
            fis.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    private Handler handler = new Handler();
    private Runnable task =new Runnable() {
        public void run() {
            handler.postDelayed(this,1000);//设置延迟时间
                takePicture();
        }
    };


    @Override
    protected void onPause() {
        super.onPause();
        if(cameraDevice!=null) {
            stopCamera();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        startCamera();
    }

    /**TextureView的监听*/
    private TextureView.SurfaceTextureListener surfaceTextureListener= new TextureView.SurfaceTextureListener() {

        //可用
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            MainActivity.this.width=width;
            MainActivity.this.height=height;
            openCamera();
        }


        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

        }

        //释放
        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            stopCamera();
            return true;
        }

        //更新
        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };


    /**打开摄像头*/
    private void openCamera() {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        //设置摄像头特性
        setCameraCharacteristics(manager);
        try {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                //提示用户开户权限
            //    String[] perms = {"android.permission.CAMERA"};
                ActivityCompat.requestPermissions( this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.CAMERA,Manifest.permission.INTERNET}, RESULT_CODE_CAMERA);

            }else {
                manager.openCamera(mCameraId, stateCallback, null);
            }

        } catch (CameraAccessException e){
            e.printStackTrace();
        }
    }


    /**设置摄像头的参数*/
    private void setCameraCharacteristics(CameraManager manager)
    {
        try
        {
            // 获取指定摄像头的特性
            CameraCharacteristics characteristics
                    = manager.getCameraCharacteristics(mCameraId);
            // 获取摄像头支持的配置属性
            StreamConfigurationMap map = characteristics.get(
                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            // 获取摄像头支持的最大尺寸
            Size largest = Collections.max(
                    Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)),new CompareSizesByArea());
            // 创建一个ImageReader对象，用于获取摄像头的图像数据
            imageReader = ImageReader.newInstance(1920, 1080, ImageFormat.JPEG, 2);
//           imageReader = ImageReader.newInstance(largest.getWidth(), largest.getHeight(),
//                    ImageFormat.JPEG, 2);
            //设置获取图片的监听
            imageReader.setOnImageAvailableListener(imageAvailableListener,null);

            // 获取最佳的预览尺寸
            previewSize = chooseOptimalSize(map.getOutputSizes(
                    SurfaceTexture.class), width, height, largest);
        }
        catch (CameraAccessException e)
        {
            e.printStackTrace();
        }
        catch (NullPointerException e)
        {
        }
    }
    private Size chooseOptimalSize(Size[] choices
            , int width, int height, Size aspectRatio)
    {
        // 收集摄像头支持的大过预览Surface的分辨率
        List<Size> bigEnough = new ArrayList<>();
        for (Size option : choices)
        {
            if (option.getHeight() == option.getWidth() * width / height )
            {
                bigEnough.add(option);
                Log.i(TAG, "啊符合要求的:option.H="+option.getHeight()+" option.W="+ option.getWidth());
            }
        }
        // 如果找到多个预览尺寸，获取其中面积最大的
        if (bigEnough.size() > 0)
        {
            return Collections.max(bigEnough, new CompareSizesByArea());
        }
        else
        {
            //没有合适的预览尺寸
            return choices[0];
        }
    }


    // 为Size定义一个比较器Comparator
    static class CompareSizesByArea implements Comparator<Size>
    {
        @Override
        public int compare(Size lhs, Size rhs)
        {
            // 强转为long保证不会发生溢出
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }
    }



    /**摄像头状态的监听*/
    private CameraDevice.StateCallback stateCallback = new CameraDevice. StateCallback()
    {
        // 摄像头被打开时触发该方法
        @Override
        public void onOpened(CameraDevice cameraDevice){
            MainActivity.this.cameraDevice = cameraDevice;
            // 开始预览
            takePreview();
        }

        // 摄像头断开连接时触发该方法
        @Override
        public void onDisconnected(CameraDevice cameraDevice)
        {
            MainActivity.this.cameraDevice.close();
            MainActivity.this.cameraDevice = null;

        }
        // 打开摄像头出现错误时触发该方法
        @Override
        public void onError(CameraDevice cameraDevice, int error)
        {
            cameraDevice.close();
        }
    };

    /**
     * 开始预览
     */
    private void takePreview() {
        SurfaceTexture mSurfaceTexture = tv.getSurfaceTexture();
        //设置TextureView的缓冲区大小
        mSurfaceTexture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
        //获取Surface显示预览数据
        Surface mSurface = new Surface(mSurfaceTexture);
        try {
            //创建预览请求
            mCaptureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            // 设置自动对焦模式
            mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            //设置Surface作为预览数据的显示界面
            mCaptureRequestBuilder.addTarget(mSurface);
            //创建相机捕获会话，第一个参数是捕获数据的输出Surface列表，第二个参数是CameraCaptureSession的状态回调接口，当它创建好后会回调onConfigured方法，第三个参数用来确定Callback在哪个线程执行，为null的话就在当前线程执行
            cameraDevice.createCaptureSession(Arrays.asList(mSurface,imageReader.getSurface()),new CameraCaptureSession.StateCallback(){
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    try {
                        //开始预览
                        mCaptureRequest = mCaptureRequestBuilder.build();
                        mPreviewSession = session;
                        //设置反复捕获数据的请求，这样预览界面就会一直有数据显示
                        mPreviewSession.setRepeatingRequest(mCaptureRequest, null, null);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {

                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }

    /**拍照*/
    private void takePicture()
    {
        if(CONNET_FLAG){
            btnipvi.setText("发送成功");
        }
        else {
            btnipvi.setText("连接断开");
        }

        try
        {
            if (cameraDevice == null)
            {
                return;
            }
            // 创建拍照请求
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            // 设置自动对焦模式
            captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            //
            // 将imageReader的surface设为目标
            captureRequestBuilder.addTarget(imageReader.getSurface());
            // 获取设备方向
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            // 根据设备方向计算设置照片的方向
            Log.i(TAG, "方向为"+rotation);
            captureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION
                    , ORIENTATIONS.get(rotation));
            // 停止连续取景
            mPreviewSession.stopRepeating();
            //拍照
            CaptureRequest captureRequest = captureRequestBuilder.build();
            //设置拍照监听
           mPreviewSession.capture(captureRequest,captureCallback, null);
        }
        catch (CameraAccessException e)
        {
            e.printStackTrace();
        }
    }

    /**监听拍照结果*/
    private CameraCaptureSession.CaptureCallback captureCallback= new CameraCaptureSession.CaptureCallback()
    {
        // 拍照成功
        @Override
        public void onCaptureCompleted(CameraCaptureSession session,CaptureRequest request,TotalCaptureResult result)
        {
            // 重设自动对焦模式
            captureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
            try {
                //重新进行预览
                mPreviewSession.setRepeatingRequest(mCaptureRequest, null, null);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }

        }

        @Override
        public void onCaptureFailed(CameraCaptureSession session, CaptureRequest request, CaptureFailure failure) {
            super.onCaptureFailed(session, request, failure);
        }
    };

    /**监听拍照的图片*/
    private ImageReader.OnImageAvailableListener imageAvailableListener= new ImageReader.OnImageAvailableListener()
    {
        // 当照片数据可用时激发该方法
        @Override
        public void onImageAvailable(ImageReader reader) {

            //先验证手机是否有sdcard
            String status = Environment.getExternalStorageState();
            if (!status.equals(Environment.MEDIA_MOUNTED)) {
                Toast.makeText(getApplicationContext(), "你的sd卡不可用。", Toast.LENGTH_SHORT).show();
                return;
            }
            // 获取捕获的照片数据
            Image image = reader.acquireNextImage();
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] data = new byte[buffer.remaining()];
            buffer.get(data);

            //保存图片到文件夹
            String filePath = Environment.getExternalStorageDirectory().getPath() + "/myCameraPhoto";
            //检查文件夹是否存在，不存在则创建
            try {
                File file;
                file = new File(filePath);
                if (!file.exists()) {
                    file.mkdir();
                }
            } catch (Exception e) {
                Log.i("error:", e+"");
            }
            //保存图片
            String picturePath = System.currentTimeMillis() + ".jpg";
            File file = new File(filePath, picturePath);
            try {
                //存到本地相册
                FileOutputStream fileOutputStream = new FileOutputStream(file);
                fileOutputStream.write(data);
                fileOutputStream.close();

                //显示图片
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 2;
                Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length, options);
                iv.setImageBitmap(bitmap);
                mydirpath=filePath+'/'+picturePath;
                new Thread(new CustomThread()).start();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                image.close();
            }
        }


    };

    @Override
    public void onRequestPermissionsResult(int permsRequestCode, String[] permissions, int[] grantResults){
        switch(permsRequestCode){
            case RESULT_CODE_CAMERA:
                boolean cameraAccepted = grantResults[0]==PackageManager.PERMISSION_GRANTED;
                if(cameraAccepted){
                    //授权成功之后，调用系统相机进行拍照操作等
                    openCamera();
                }else{
                    //用户授权拒绝之后，友情提示一下就可以了
                    Toast.makeText(MainActivity.this,"请开启应用拍照权限",Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    public class CustomThread implements Runnable{
        @Override
        public void run() {
            // 处理耗时逻辑
            DataOutputStream dos;
            FileInputStream fis;
            try {
                file = new File(mydirpath);
                if (file.length() == 0) {
                    Log.i(TAG, "啊，文件长度为0！");
                    return;
                } else {
                    CONNET_FLAG=false;
                    Log.i(TAG, "啊，开始创建套接字");
                    socket = new Socket(ip, port);
                    CONNET_FLAG=true;
                    Log.i(TAG, "啊，创建套接字成功");
                    dos = new DataOutputStream(socket.getOutputStream());
                    fis = new FileInputStream(file);
                    dos.writeUTF(mydirpath.substring(mydirpath.lastIndexOf("/")+1));//截取图片名称
                    dos.flush();
                    byte[] sendBytes = new byte[64 * 1024 * 8];
                    int length;
                    while ((length = fis.read(sendBytes, 0, sendBytes.length)) > 0) {
                        dos.write(sendBytes, 0, length);
                        dos.flush();// 发送给服务器
                    }
                    dos.close();//在发送消息完之后一定关闭，否则服务端无法继续接收信息后处理，手机卡机
                    fis.close();
                    socket.close();
                    file.delete();
                    Log.i(TAG, "啊：发送给服务器完毕.删除图片");
                }
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }catch (SocketTimeoutException e) {
                e.printStackTrace();
            }catch (IOException e) {
                e.printStackTrace();
            }

            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(CONNET_FLAG){
                        btnipvi.setText("发送成功");
                    }
                    else {
                        btnipvi.setText("连接断开");
                    }
                }
            });
        }
    }





    public class CustomThread1 implements Runnable{
        @Override
        public void run() {
            // 处理耗时逻辑
            DataOutputStream dos;
            FileInputStream fis;
            try {
                file = new File("/storage/emulated/0/myCameraPhoto/1234.mp4");
                Log.i(TAG, "哈哈哈"+mydirpath);
                if (file.length() == 0) {
                    Log.i(TAG, "啊，文件长度为0！");
                    return;
                } else {
                    CONNET_FLAG=false;
                    Log.i(TAG, "啊，开始创建套接字");
                    socket = new Socket(ip, port);
                    CONNET_FLAG=true;
                    Log.i(TAG, "啊，创建套接字成功");
                    dos = new DataOutputStream(socket.getOutputStream());
                    fis = new FileInputStream(file);
                    dos.writeUTF(mydirpath.substring(mydirpath.lastIndexOf("/")+1));//截取图片名称
                    dos.flush();
                    byte[] sendBytes = new byte[8 * 1024 * 8];
                    int length;
                    while ((length = fis.read(sendBytes, 0, sendBytes.length)) > 0) {
                        dos.write(sendBytes, 0, length);
                        dos.flush();// 发送给服务器
                    }
                    dos.close();//在发送消息完之后一定关闭，否则服务端无法继续接收信息后处理，手机卡机
                    fis.close();
                    socket.close();
                    file.delete();
                    Log.i(TAG, "啊：发送给服务器完毕.删除图片");
                }
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }catch (SocketTimeoutException e) {
                e.printStackTrace();
            }catch (IOException e) {
                e.printStackTrace();
            }

            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(CONNET_FLAG){
                        btnipvi.setText("发送成功");
                    }
                    else {
                        btnipvi.setText("连接断开");
                    }
                }
            });
        }
    }


    /**启动拍照*/
    private void startCamera(){
        if (tv.isAvailable()) {
            if(cameraDevice==null) {
                openCamera();
            }
        } else {
            tv.setSurfaceTextureListener(surfaceTextureListener);
        }
    }

    /**
     * 停止拍照释放资源*/
    private void stopCamera(){
        if(cameraDevice!=null){
            cameraDevice.close();
            cameraDevice=null;
        }

    }
}