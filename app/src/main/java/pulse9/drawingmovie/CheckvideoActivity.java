package pulse9.drawingmovie;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.support.v4.widget.ImageViewCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.VideoView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by ssoww on 2018-11-19.
 */


/*
    Number - Style name
    0 - 1-OIL-B
    1 - 1-LINE-B
    2 - 1-NEW-C
    3 - 1-LINE-A
    4 - 1-ACRYLIC-C
    5 - 1-ACRYLIC-A
    6 - 1-NEW-A
    7 - 1-NEW-B
 */

public class CheckvideoActivity extends AppCompatActivity implements Runnable,View.OnClickListener {

    private static final int NUM_STYLES = 9;     //필터 갯수
    private static final int NUM_OPTION = 4;
    private static final String MODEL_FILE = "file:///android_asset/frozen2.pb";
    private static final String INPUT_NODE = "input";
    private static final String INPUT_NODE2 = "style_num";
    private static final String OUTPUT_NODE = "transformer/expand/conv3/conv/Sigmoid";
    final private String upLoadServerUri = "http://183.102.74.3:8871/upload_app";
    private int serverResponseCode = 0;

    private static int WANTED_WIDTH = 400;  //700
    private static int WANTED_HEIGHT = 600;  //1000

    static final float[] styleVals = new float[NUM_STYLES];
    String path;
    String FcmToken;
    String UID;
    private static ImageButton[] buttons = new ImageButton[NUM_OPTION];
    private static ImageButton[] thums = new ImageButton[NUM_STYLES];
    ProgressBar progressBar;
    private static VideoView videoView;
    private static ImageView imageView;
    private static ImageView mtransImageView;
    private static FrameLayout myframe;
    private static Bitmap moriginBitmap;
    private static ArrayList<Bitmap> mTransferredBitmap=new ArrayList<>();
    private static Queue<Integer> threefilter = new LinkedList<Integer>();
    private static TensorFlowInferenceInterface mInferenceInterface;
    private int transFilter=1;
    private String uploadFilename="";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkvideo);

        Intent intent=getIntent();
        path=intent.getStringExtra("Uri");

        loadToken();
        initButtons();
        loadThumbnail();

    }

    private void loadToken(){
        SharedPreferences pref = getSharedPreferences("Token", Activity.MODE_PRIVATE);
        FcmToken = pref.getString("FcmToken","NOTOKEN");
        UID=FcmToken.substring(0,10);
    }

    private void initButtons(){
        buttons[0] = (ImageButton)findViewById(R.id.back);
        buttons[1] = (ImageButton)findViewById(R.id.upload);
        buttons[2] = (ImageButton)findViewById(R.id.play);
        buttons[3] = (ImageButton)findViewById(R.id.origin_butt);
        thums[0] = findViewById(R.id.button0);
        thums[1] = findViewById(R.id.button1);
        thums[2] = findViewById(R.id.button2);
        thums[3] = findViewById(R.id.button3);
        thums[4] = findViewById(R.id.button4);
        thums[5] = findViewById(R.id.button5);
        thums[6] = findViewById(R.id.button6);
        thums[7] = findViewById(R.id.button7);
        thums[8] = findViewById(R.id.button8);


        videoView = (MyVideoView)findViewById(R.id.videoView);
        imageView = (ImageView)findViewById(R.id.origin_imageview);
        mtransImageView = findViewById(R.id.overlapimageview);
        myframe = (FrameLayout)findViewById(R.id.myframe);


        progressBar = (ProgressBar)findViewById(R.id.upload_loading);


        for (int i = 0; i < NUM_STYLES; ++i) {
            styleVals[i] = 0.0f / NUM_STYLES;       //init Style Vales
        }

        for (int i = 0; i < NUM_OPTION; i++) {
            buttons[i].setOnClickListener(this);
        }
        for (int i = 0; i < NUM_STYLES; i++) {
            thums[i].setOnClickListener(this);
        }

        //final MediaController mediaController = new MediaController(this);
        videoView.setMediaController(new MediaController(this){
        });
        videoView.setMediaController(null);
        videoView.setVideoPath(path);
        videoView.requestFocus();
        videoView.seekTo( 1 );


        for(int i=0;i<NUM_STYLES;i++){
            mTransferredBitmap.add(i,null);
        }
        mtransImageView.setImageBitmap(null);
    }

    private void loadThumbnail(){
        Log.e("sojeong","path: "+path);
        moriginBitmap = ThumbnailUtils.createVideoThumbnail(path,
                MediaStore.Images.Thumbnails.MINI_KIND);


        long width =moriginBitmap.getWidth();
        long height = moriginBitmap.getHeight();

        while (width >1000.0 && height > 1000.0){
            width = (long) (width * 0.8);
            height = (long) (height * 0.8);
            moriginBitmap = Bitmap.createScaledBitmap(moriginBitmap, (int) width, (int) height, true);
        }
        imageView.setImageBitmap(moriginBitmap);
    }

    private int uploadFile(String sourceFileUri) {
        String fileName = sourceFileUri;
        HttpURLConnection conn = null;
        DataOutputStream dos = null;
        String lineEnd = "\r\n";
        String twoHyphens = "--";
        String boundary = "*****";
        Message msg = new Message();

        int bytesRead, bytesAvailable, bufferSize;
        byte[] buffer;
        int maxBufferSize = 1 * 1024 * 1024;

        File sourceFile = new File(sourceFileUri);
        if (!sourceFile.isFile()) {
            Log.e("uploadFile", "Source File not exist");
            return 0;
        } else {
            try {
                // open a URL connection to the Servlet
                FileInputStream fileInputStream = new FileInputStream(sourceFile);
                URL url = new URL(upLoadServerUri);

                long unixTime = System.currentTimeMillis() / 1000L;

                // Open a HTTP  connection to  the URL
                conn = (HttpURLConnection) url.openConnection();
                Log.d("uploadFileJ", conn.toString());
                conn.setDoInput(true); // Allow Inputs
                conn.setDoOutput(true); // Allow Outputs
                conn.setUseCaches(false); // Don't use a Cached Copy
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Connection", "Keep-Alive");
                conn.setRequestProperty("ENCTYPE", "multipart/form-data");
                conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
                conn.setRequestProperty("uploaded_file", fileName);
                dos = new DataOutputStream(conn.getOutputStream());
                dos.writeBytes(twoHyphens + boundary + lineEnd);
                Log.d("uploadFileJ", dos.toString());

                String timeStamp = new SimpleDateFormat("yyMMddHHmmss").format(new Date());
                uploadFilename = timeStamp + "__"+transFilter+"__"+UID+"__Android.mp4";

                dos.writeBytes("Content-Disposition: form-data; name=\"uploaded_file\";filename=\"" + uploadFilename + "\"" + lineEnd);
                dos.writeBytes(lineEnd);
                // create a buffer of  maximum size
                bytesAvailable = fileInputStream.available();
                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                buffer = new byte[bufferSize];

                // read file and write it into form...
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                while (bytesRead > 0) {
                    dos.write(buffer, 0, bufferSize);
                    bytesAvailable = fileInputStream.available();
                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                }
                // send multipart form data necesssary after file data...
                dos.writeBytes(lineEnd);
                dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
                // Responses from the server (code and message)
                serverResponseCode = conn.getResponseCode();
                String serverResponseMessage = conn.getResponseMessage();
                Log.i("uploadFile", "HTTP Response is : " + serverResponseMessage + ": " + serverResponseCode);
                if (serverResponseCode == 200) {
                    Log.d("uploadFile", "File Upload Complete.");
                    msg.obj = "success";

                }
                fileInputStream.close();
                dos.flush();
                dos.close();
            } catch (MalformedURLException ex) {
                Log.e("uploadFile", "error: " + ex.getMessage(), ex);
                msg.obj = ex.getMessage();
            } catch (Exception e) {
                Log.e("uploadFile", "Exception : " + e.getMessage(), e);
                msg.obj = e.getMessage();
            }

            mHandler2.sendMessage(msg);
            return serverResponseCode;

        } // End else block
    }

    Handler mHandler2 = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            String text = (String) msg.obj;
            if(text=="success"){
                Toast.makeText(CheckvideoActivity.this,"Upload Complete",Toast.LENGTH_LONG).show();
                addFirebase(uploadFilename);
            }else{
                Toast.makeText(CheckvideoActivity.this,"please upload again(:error)",Toast.LENGTH_LONG).show();

                progressBar.setVisibility(View.GONE);
                for (int i = 0; i < NUM_STYLES; i++) {
                    thums[i].setEnabled(true);
                    thums[i].setAlpha(255);
                }
                for (int i = 1; i < NUM_OPTION; i++) {
                    buttons[i].setEnabled(true);
                    buttons[i].setAlpha(255);
                }

            }
        }
    };

    private void addFirebase(String fireBaseFileName){

        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
        DatabaseReference videoRef = firebaseDatabase.getReference("VlinkAndroid");

        String Fcm28 = FcmToken.substring(0,28);
//        videoRef.child(Fcm28).setValue("1");
        videoRef.child(UID).child("token").setValue(FcmToken);
//        videoRef.child(UID).child("id").setValue(UID+"@");
        videoRef.child(UID).child("image").push().setValue(fireBaseFileName);
//        progressDialog.dismiss();
        progressBar.setVisibility(View.GONE);

        saveUploadCount();
        Intent intent = new Intent(CheckvideoActivity.this,NoticeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);


    }

    private boolean checkUploadOk(){
        String date = new SimpleDateFormat("yyMMdd").format(new Date());
        SharedPreferences pref = getSharedPreferences(date, Activity.MODE_PRIVATE);
        if(pref.contains("Count")){
            int count= pref.getInt("Count",0);
            if(count>=3){
                return false;
            }else{
                return true;
            }
        }else{
            return true;
        }
    }

    private void saveUploadCount(){
        String date = new SimpleDateFormat("yyMMdd").format(new Date());
        SharedPreferences pref = getSharedPreferences(date, Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor=pref.edit();
        if(!pref.contains("Count")){
            editor.putInt("Count",1);
            editor.commit();
        }else{
            int count= pref.getInt("Count",0);
            count++;
            editor.putInt("Count",count);
            editor.commit();
        }


    }



    @Override
    public void onClick(View view) {
        Animation clickAnimation = AnimationUtils.loadAnimation(CheckvideoActivity.this,R.anim.clickanimation);
        view.startAnimation(clickAnimation);
        int j = -1;
        switch (view.getId()){
            case R.id.play:
                buttons[2].setVisibility(View.INVISIBLE);
                videoView.setVisibility(View.VISIBLE);
                imageView.setVisibility(View.INVISIBLE);
                mtransImageView.setVisibility(View.INVISIBLE);
                videoView.start();

                videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

                    @Override
                    public void onCompletion(MediaPlayer vmp) {
                        videoView.setVisibility(View.INVISIBLE);
                        imageView.setVisibility(View.VISIBLE);
                        mtransImageView.setVisibility(View.VISIBLE);
                        buttons[2].setVisibility(View.VISIBLE);
                    }
                });
                break;
            case R.id.back:
                finish();
                break;
            case R.id.upload:
                if(checkUploadOk()){
                    progressBar.setVisibility(View.VISIBLE);
                    for (int i = 0; i < NUM_STYLES; i++) {
                        thums[i].setEnabled(false);
                        thums[i].setAlpha(100);
                    }
                    thums[transFilter].setAlpha(255);
                    for (int i = 1; i < NUM_OPTION; i++) {
                        buttons[i].setEnabled(false);
                        buttons[i].setAlpha(100);
                    }

                    Log.e("sojeong","video path: "+path);
                    new Thread(new Runnable() {
                        public void run() {
                            uploadFile(path);
                        }
                    }).start();

                }else{
                    AlertDialog.Builder builder = new AlertDialog.Builder(CheckvideoActivity.this);
                    builder.setTitle("Upload Count Excess")
                            .setMessage("Only 3 videos can upload per day.")
                            .setNeutralButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    finish();
                                }

                            });
                    AlertDialog dialog = builder.create();
                    dialog.show();
                }



                break;
            case R.id.origin_butt:
                buttons[2].setVisibility(View.VISIBLE);
                myframe.setForeground(getDrawable(R.drawable.videolayout));
                myframe.setBackgroundColor(getResources().getColor(R.color.black));
                mtransImageView.setAlpha(0);
                j = -1;
                break;
            case R.id.button0:
                j = 0;
                break;
            case R.id.button1:
                j = 1;
                break;
            case R.id.button2:
                j = 2;
                break;
            case R.id.button3:
                j = 3;
                break;
            case R.id.button4:
                j = 4;
                break;
            case R.id.button5:
                j = 5;
                break;
            case R.id.button6:
                j = 6;
                break;
            case R.id.button7:
                j = 7;
                break;
            case R.id.button8:
                j = 8;
                break;
            default:
                break;
        }
        if (j != -1) {
            buttons[2].setVisibility(View.INVISIBLE);
            myframe.setForeground(null);
            myframe.setBackgroundColor(getResources().getColor(R.color.opacity));
            startThread(j);          //j값에 따라 필터를 달리하여 thread로 transfer진행
        }
    }

    private void startThread(int k) {
        for(int i=0;i<mTransferredBitmap.size();i++) {
            Log.e("sojeong", "arrayList["+i+"] : "+mTransferredBitmap.get(i));
        }
        if (k != -1) {
            transFilter = k;
            if(mTransferredBitmap.get(transFilter)==null) {
                progressBar.setVisibility(View.VISIBLE);
                styleVals[k] = 1f;                      //변형하려는 필터의 배열위치에 1저장

                Thread thread = new Thread(CheckvideoActivity.this);
                thread.start();               //thread시작

//            ImgTransfer imgTransfer=new ImgTransfer();
//            imgTransfer.execute();

                for (int i = 0; i < NUM_STYLES; i++) {
                    thums[i].setEnabled(false);
                    thums[i].setAlpha(100);
                }
                thums[transFilter].setAlpha(255);
                for (int i = 1; i < NUM_OPTION; i++) {
                    buttons[i].setEnabled(false);
                    buttons[i].setAlpha(100);
                }
                //seekBar.setVisibility(View.INVISIBLE);            //변형되는 동안 다른버튼 비활성화, 투명하게 조정
            }else{
                mtransImageView.setImageBitmap(mTransferredBitmap.get(transFilter));
                mtransImageView.setAlpha(255);
                //mtransImageView.setAlpha(Transparency);
                //seekBar.setVisibility(View.VISIBLE);
            }
        }
    }

    public boolean isLowMemory(long width,long height) {
        long maxMemory = 0L;
        long reqMemory = 4 * width * height;
        long allocMemory = Debug.getNativeHeapAllocatedSize();
        long extraMemory = 0;
        Log.e("sojeong","isLowMemory");

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            ActivityManager am = (ActivityManager)getSystemService(ACTIVITY_SERVICE);
            maxMemory = am.getMemoryClass();
        } else {
            maxMemory = Runtime.getRuntime().totalMemory();
            allocMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        }

        if ((reqMemory + allocMemory + extraMemory) >= maxMemory) {
            return true;
        }
        return false;
    }

    public void run() {         //image transfer

        Message msg = new Message();
        boolean IsOOM = false;
        Log.e("sojeong","run thread");
        long width =moriginBitmap.getWidth();
        long height = moriginBitmap.getHeight();
        while(isLowMemory(width, height)== true){
            if(width >200.0 && height > 200.0) {
                width = (long) (width * 0.8);
                height = (long) (height * 0.8);
                Log.e("sojeong", "width: " + width + "height: " + height);
//            Toast.makeText(MainActivity.this, "Image size is reduced \nbecause the device capacity is too low.", Toast.LENGTH_SHORT).show();

                moriginBitmap = Bitmap.createScaledBitmap(moriginBitmap, (int) width, (int) height, true);
            }else{
                IsOOM =true;
                break;
            }
        }

        if(!IsOOM){
            int[] intValues = new int[WANTED_WIDTH * WANTED_HEIGHT];
            float[] floatValues = new float[WANTED_WIDTH * WANTED_HEIGHT * 3];
            float[] outputValues = new float[WANTED_WIDTH * WANTED_HEIGHT * 3];
            try {
                Bitmap scaledBitmap = Bitmap.createScaledBitmap(moriginBitmap, WANTED_WIDTH, WANTED_HEIGHT, true);
                scaledBitmap.getPixels(intValues, 0, scaledBitmap.getWidth(), 0, 0, scaledBitmap.getWidth(), scaledBitmap.getHeight());
                for (int i = 0; i < intValues.length; i++) {
                    final int val = intValues[i];
                    floatValues[i * 3] = ((val >> 16) & 0xFF);
                    floatValues[i * 3 + 1] = ((val >> 8) & 0xFF);
                    floatValues[i * 3 + 2] = (val & 0xFF);
                }
                AssetManager assetManager = getAssets();
                mInferenceInterface = new TensorFlowInferenceInterface(assetManager, MODEL_FILE);

                mInferenceInterface.feed(INPUT_NODE, floatValues, 1, WANTED_HEIGHT, WANTED_WIDTH, 3);
                mInferenceInterface.feed(INPUT_NODE2, styleVals, NUM_STYLES);
                mInferenceInterface.run(new String[]{OUTPUT_NODE}, false);
                mInferenceInterface.fetch(OUTPUT_NODE, outputValues);

                for (int i = 0; i < intValues.length; ++i) {
                    intValues[i] = 0xFF000000
                            | (((int) (outputValues[i * 3] * 255)) << 16)
                            | (((int) (outputValues[i * 3 + 1] * 255)) << 8)
                            | ((int) (outputValues[i * 3 + 2] * 255));
                }

                Bitmap outputBitmap = scaledBitmap.copy(scaledBitmap.getConfig(), true);
                outputBitmap.setPixels(intValues, 0, outputBitmap.getWidth(), 0, 0, outputBitmap.getWidth(), outputBitmap.getHeight());
                Bitmap transferredBitmap = Bitmap.createScaledBitmap(outputBitmap, moriginBitmap.getWidth(), moriginBitmap.getHeight(), true);

                if(threefilter.size()>=3){
                    int removefilter=threefilter.poll();
                    mTransferredBitmap.set(removefilter,null);
                }
                threefilter.offer(transFilter);
                mTransferredBitmap.set(transFilter, transferredBitmap);

                msg.obj = "Transferred";
                mHandler.sendMessage(msg);
            } catch (OutOfMemoryError e) {
                msg.obj = "OutOfMemory";
                mHandler.sendMessage(msg);
            }
//        bitmap.recycle();


        }else{
            msg.obj = "OutOfMemory";
            mHandler.sendMessage(msg);
        }
    }

    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {          //image transfer된 후 호출
            Log.e("sojeong","run Handler");
            String text = (String) msg.obj;
            Toast.makeText(CheckvideoActivity.this,"This is an example of the first frame!",Toast.LENGTH_LONG).show();
//            Toast.makeText(CheckvideoActivity.this, text,Toast.LENGTH_SHORT).show();
            if(text.equals("OutOfMemory")) {
                android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(CheckvideoActivity.this);
                builder.setMessage("Your device memory is too low, so can not transfer this image!")
                        .setNeutralButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int whichButton) {

                            }

                        });
                android.app.AlertDialog dialog = builder.create();
                dialog.show();
            }else {

                mtransImageView.setImageBitmap(mTransferredBitmap.get(transFilter));
                //Transparency = 255;
                //seekBar.setProgress(Transparency);    //seekbar의 위치도 최대로 옮김
                //mtransImageView.setAlpha(Transparency);         //변환된 사진을 완전 불투명하게하여 origin 이미지위에 올림
                mtransImageView.setAlpha(255);


                for (int i = 0; i < thums.length; i++) {         //비활성화시킨 버튼들을 다시 활성화 시킴
                    thums[i].setEnabled(true);
                    thums[i].setAlpha(255);
                }
                for (int i = 1; i < NUM_OPTION; i++) {
                    buttons[i].setEnabled(true);
                    buttons[i].setAlpha(255);
                }
                for (int i = 0; i < NUM_STYLES; ++i) {
                    styleVals[i] = 0.0f / NUM_STYLES;
                }
                progressBar.setVisibility(View.GONE);
                //seekBar.setVisibility(View.VISIBLE);
            }
        }
    };


}
