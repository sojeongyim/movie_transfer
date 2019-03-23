package pulse9.drawingmovie;

import android.Manifest;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.File;
import java.util.ArrayList;
import java.util.Random;

/**
 * Created by pulse on 2019. 2. 27..
 */

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private int ButtonNum=4;
    private ImageButton[] buttons = new ImageButton[ButtonNum];
    private static VideoView videoView;
    private static FrameLayout mylayout;
    private String FcmToken = null;
    private String UID = null;
    private boolean IsRinging=false;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
    };
    private ArrayList<String> fileNameToDown = new ArrayList<>();
    private final int GALLERY_PICK=1;
    final String url = "http://183.102.74.3:8871/pvivid_app/result/";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActivityCompat.requestPermissions(MainActivity.this, PERMISSIONS_STORAGE, 1);
        if (Build.VERSION.SDK_INT >= 21) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }

        initButtons();
        saveToken();
        playSampleVideo();
    }

    private  void startRinging()
    {
        buttons[1].setImageResource(R.drawable.mainbell_pink);
        Animation animation;
        animation = AnimationUtils.loadAnimation(getApplicationContext(),
                R.anim.alarming);
        buttons[1].startAnimation(animation);
        IsRinging=true;
    }
    private  void stopRinging()
    {
        buttons[1].setImageResource(R.drawable.mainbell);
        buttons[1].clearAnimation();
        IsRinging=false;
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == 0) {
                Uri uri = data.getData();
                String path = getPath(uri);
                Log.e("sojeong", "filterNum: " + requestCode);
                Intent intent = new Intent(MainActivity.this, CheckvideoActivity.class);
                intent.putExtra("Uri", path);
                intent.addFlags(intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            } else if (requestCode == GALLERY_PICK) {
                Uri uri = Uri.parse(data.getDataString());
                try {
                    MediaMetadataRetriever m = new MediaMetadataRetriever();
                    m.setDataSource("/storage/emulated/0/Movies/DrawingMovie/190302150651__0__ceEr7soUXK__Android.mp4");
                    if (Build.VERSION.SDK_INT >= 17) {
                        String s = m.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
                        Log.e("*******Rotation", s);
                    }
                }catch (Exception e){
                    Log.e("Exception",e+"");
                    Log.e("data",uri.getPath());
                }

                Log.e("sojeong", "gallery_pick intent: " + data.getData());
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.parse(data.getDataString()), "video/*");
                startActivity(intent);
            }
        }
    }

//     실제 경로 찾기
    private String getPath(Uri uri) {
        String[] projection = { MediaStore.Images.Media.DATA };
        Cursor cursor = managedQuery(uri, projection, null, null, null);
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }




    private void downloadResult(final String filename, final int final_file)    {
//        progressBar.setVisibility(View.VISIBLE);

        //String filename = url.split("/")[url.split("/").length-1];
        final String finalurl=url+filename;

        File direct = new File(Environment
                .getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
                .getAbsolutePath() + "/");

        if (!direct.exists()) {
            direct.mkdir();
            Log.d("sojeong", "dir created for first time");
        }
        DownloadManager dm = (DownloadManager)getApplicationContext().getSystemService(Context.DOWNLOAD_SERVICE);
        Uri downloadUri = Uri.parse(finalurl);
        DownloadManager.Request request = new DownloadManager.Request(downloadUri);
        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE)
                .setAllowedOverRoaming(false)
                .setTitle(filename)
                .setMimeType("video/mp4")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_MOVIES,
                        File.separator + "DrawingMovie" + File.separator + filename);
        dm.enqueue(request);

        if(final_file==fileNameToDown.size()){
//            progressBar.setVisibility(View.INVISIBLE);
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setMessage("Download Complete!")
                    .setNeutralButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int whichButton) {

                            //Uri fileUri = FileProvider.getUriForFile(DownloadActivity.this, "pulse9.drawingmovie.provider", localFile);
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setDataAndType(Uri.parse(finalurl), "video/*");
                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);  //DO NOT FORGET THIS EVER
                            startActivityForResult(intent,1);

                        }

                    });
            AlertDialog dialog = builder.create();
            dialog.show();
        }


//                    progressBar.setVisibility(View.GONE);
        deleteFirebase(filename);
    }




    private void deleteFirebase(String filename){
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference downloaded = database.getReference("VlinkAndroid");
        downloaded.child(UID).child("result").removeValue();


    }

    private void playSampleVideo(){
//        Random random = new Random();
//        int num = random.nextInt(3);
//        int[] videofile = {R.raw.video1,R.raw.video2,R.raw.video3};
//        Log.e("selected video num",""+videofile[num]);



        videoView.setMediaController(new MediaController(this){
        });
//        Uri uri = Uri.parse("android.resource://"+getPackageName()+"/"+videofile[num]);
        Uri uri = Uri.parse("android.resource://"+getPackageName()+"/"+R.raw.mainsample);
        videoView.setVideoURI(uri);
        //videoView.setVideoPath("file:///android_asset/video1.mp4");
        videoView.requestFocus();
        videoView.setMediaController(null);
        videoView.seekTo( 1 );
        videoView.start();

        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            public void onCompletion(MediaPlayer mp) {
                videoView.start(); //need to make transition seamless.
            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
        playSampleVideo();
    }

    private void saveToken(){
        MyFirebaseInstanceIDService myFirebaseInstanceIDService = new MyFirebaseInstanceIDService();
        myFirebaseInstanceIDService.onTokenRefresh();

        FcmToken=myFirebaseInstanceIDService.getToken();
//        FcmToken = FirebaseInstanceId.getInstance().getToken();
        if(FcmToken==null){
            Random rnd = new Random();
            StringBuffer temp = new StringBuffer();
            for(int i=0;i<21;i++) {
                temp.append(String.valueOf((char) ((int) (rnd.nextInt(26)) + 65)));
            }
            FcmToken="RANDOM_"+temp.toString();
            Log.e("sojeong","Fcm임의생성: "+FcmToken);

            Toast.makeText(MainActivity.this,"RANDOM FCM TOKEN is generated ",Toast.LENGTH_SHORT).show();
        }

        UID = FcmToken.substring(0, 10);
        SharedPreferences sharedPreferences = getSharedPreferences("Token", Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("FcmToken",FcmToken);
        editor.putString("UID",UID);
        editor.commit();

        Log.e("sojeong", "[I'm MainActivity] preference save FcmToken: " + FcmToken);

    }

    private void initButtons(){
        buttons[0]=(ImageButton)findViewById(R.id.playex);
        buttons[1]=(ImageButton)findViewById(R.id.bell);
        buttons[2]=(ImageButton)findViewById(R.id.plus);
        buttons[3]=(ImageButton)findViewById(R.id.maincam);

        videoView = (MyVideoView)findViewById(R.id.main_video);
        mylayout = (FrameLayout) findViewById(R.id.mylayout);


        for(int i=0;i<ButtonNum;i++){
            buttons[i].setOnClickListener(this);

        }

        Animation animation;
        animation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.scale);
        buttons[2].startAnimation(animation);

    }

    @Override
    public void onClick(View view) {
        Animation fadein = AnimationUtils.loadAnimation(MainActivity.this,R.anim.fade_in);
        Animation fadeout = AnimationUtils.loadAnimation(MainActivity.this,R.anim.fade_out);
        Animation clickAnimation = AnimationUtils.loadAnimation(MainActivity.this,R.anim.clickanimation);
        //view.startAnimation(clickAnimation);
        Intent intent;

        switch (view.getId()){
            case R.id.playex:
                buttons[0].startAnimation(clickAnimation);
                intent= new Intent(MainActivity.this,ExvideoActivity.class);
                intent.addFlags(intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);



//                intent = new Intent(Intent.ACTION_PICK);
//                intent.setType("video/*");
//                startActivityForResult(intent,GALLERY_PICK);
                break;

            case R.id.bell:
                buttons[1].startAnimation(clickAnimation);
                if(IsRinging==true){
                    mylayout.setForeground(getDrawable(R.drawable.downloading));

                    for(int i =0;i<fileNameToDown.size();i++) {
                        Log.e("sojeong", "Intent filenamelist"+i+":" + fileNameToDown.get(i));
                        downloadResult(fileNameToDown.get(i),i+1);
                    }
                    mylayout.setForeground(null);

                }else {

                    intent = new Intent(Intent.ACTION_PICK);
                    intent.setType("video/*");
                    startActivityForResult(intent,GALLERY_PICK);

                }
                break;
            case R.id.plus:

                FirebaseDatabase database = FirebaseDatabase.getInstance();
                DatabaseReference downloadInfo = database.getReference("VlinkAndroid");

                for(int i=0;i<ButtonNum;i++){
                    buttons[i].startAnimation(fadein);
                    buttons[i].setVisibility(View.VISIBLE);
                }
                buttons[2].startAnimation(fadeout);
                buttons[2].setVisibility(View.GONE);

                downloadInfo.child(UID).child("result").addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                        Log.e("sojeong","child add!");
                        startRinging();
                        String filename=dataSnapshot.getValue().toString();
                        fileNameToDown.add(filename);
                    }
                    @Override
                    public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

                    }
                    @Override
                    public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
                        if(dataSnapshot.getChildrenCount()==0)
                        {
                            stopRinging();
                        }
                    }
                    @Override
                    public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

                break;
            case R.id.maincam:
                buttons[3].startAnimation(clickAnimation);
                intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
//                intent.putExtra(android.provider.MediaStore.EXTRA_VIDEO_QUALITY,0.8);
                intent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 3);
                startActivityForResult(intent, 0);

                break;
            default:
                break;
        }
    }
}
