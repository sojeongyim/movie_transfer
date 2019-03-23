package pulse9.drawingmovie;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.VideoView;

import java.io.File;
import java.util.Random;

/**
 * Created by pulse on 2019. 2. 20..
 */

public class ExvideoActivity extends AppCompatActivity implements View.OnClickListener{

    private int thum_num = 6;
    private int option_num = 2;
    private ImageView[] thumlist = new ImageView[thum_num];
    private ImageButton[] buttons = new ImageButton[option_num];
    private VideoView videoView;
    private int[] videofile = {R.raw.video1,R.raw.video1,R.raw.video1,R.raw.video1,R.raw.video1,R.raw.video1};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exvideo);

        initButtons();
        setThumbnail(); //영상 몇개없을거같아서 임시로 하나하나 리스트로 넣어줌(동적x)




    }

    @Override
    protected void onStart() {
        super.onStart();
        playSampleVideo(0);
    }

    private void initButtons(){
        GradientDrawable rounded = (GradientDrawable)getApplicationContext().getResources().getDrawable(R.drawable.rounded);

        //for(int i=0;i<6;i++){
        thumlist[0]=(ImageView) findViewById(R.id.list_video1);
        thumlist[1]=(ImageView)findViewById(R.id.list_video2);
        thumlist[2]=(ImageView)findViewById(R.id.list_video3);
        thumlist[3]=(ImageView)findViewById(R.id.list_video4);
        thumlist[4]=(ImageView)findViewById(R.id.list_video5);
        thumlist[5]=(ImageView)findViewById(R.id.list_video6);
        //}

        buttons[0] = (ImageButton)findViewById(R.id.back);
        buttons[1] = (ImageButton)findViewById(R.id.shoot);

        videoView = (VideoView)findViewById(R.id.main_video);

        for(int i=0;i<6;i++){
            thumlist[i].setOnClickListener(this);
            thumlist[i].setBackground(rounded);
            thumlist[i].setClipToOutline(true);
        }

        for(int i=0;i<2;i++){
            buttons[i].setOnClickListener(this);
        }
        playSampleVideo(0);


    }

    @Override
    public void onClick(View v) {
        Animation clickAnimation = AnimationUtils.loadAnimation(ExvideoActivity.this,R.anim.clickanimation);
        v.startAnimation(clickAnimation);

        switch (v.getId()){
            case R.id.list_video1:
                playSampleVideo(0);
                break;
            case R.id.list_video2:
                playSampleVideo(1);
                break;
            case R.id.list_video3:
                playSampleVideo(2);
                break;
            case R.id.list_video4:
                playSampleVideo(3);
                break;
            case R.id.list_video5:
                playSampleVideo(4);
                break;
            case R.id.list_video6:
                playSampleVideo(5);
                break;
            case R.id.back:
                finish();
                break;
            case R.id.shoot:
                MoveToInsta();
//                Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
//                intent.putExtra(MediaStore.EXTRA_DURATION_LIMIT,3);
//                startActivityForResult(intent,0);
                break;
            default:
                break;


        }

    }

    private void MoveToInsta() {
        Uri uri = Uri.parse("https://www.instagram.com/officialpaintly/?hl=ko");
        Intent likeIng = new Intent(Intent.ACTION_VIEW, uri);

        likeIng.setPackage("com.instagram.android");
        try {
            startActivity(likeIng);
        } catch (ActivityNotFoundException e) {
            Log.e("ActivityException",e+"");
        }
    }

    private void setThumbnail(){

        for(int i=0;i<6;i++) {
            Uri videoURI = Uri.parse("android.resource://" + getPackageName() + "/" + videofile[i]);
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(this, videoURI);
            Bitmap thum = retriever.getFrameAtTime(100000, MediaMetadataRetriever.OPTION_PREVIOUS_SYNC);
            thumlist[i].setImageBitmap(thum);
            thumlist[i].setScaleType(ImageView.ScaleType.CENTER_CROP);
        }
    }

    private void playSampleVideo(int index){

        GradientDrawable border = (GradientDrawable)getApplicationContext().getResources().getDrawable(R.drawable.border);

        for(int i=0;i<6;i++){
            thumlist[i].setForeground(null);
        }

        thumlist[index].setForeground(border);


        videoView.setMediaController(new MediaController(this){
        });
        Uri uri = Uri.parse("android.resource://"+getPackageName()+"/"+videofile[index]);
        videoView.setVideoURI(uri);
        //videoView.requestFocus();
        videoView.setMediaController(null);  //하단바 제거
        videoView.seekTo( 1 );     //처음부터 실행
        videoView.start();

        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            public void onCompletion(MediaPlayer mp) {
                videoView.start(); //need to make transition seamless.
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode==RESULT_OK){
            if(requestCode==0){
                Uri uri = data.getData();
                String path = getPath(uri);
                Log.e("sojeong","filterNum: "+requestCode);
                Intent intent = new Intent(ExvideoActivity.this, CheckvideoActivity.class);
                intent.putExtra("Uri", path);
                intent.addFlags(intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }
        }
    }

    // 실제 경로 찾기
    private String getPath(Uri uri)
    {
        String[] projection = { MediaStore.Images.Media.DATA };
        Cursor cursor = managedQuery(uri, projection, null, null, null);
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }




}
