package pulse9.drawingmovie;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;

/**
 * Created by ssoww on 2018-11-19.
 */

public class NoticeActivity extends AppCompatActivity implements View.OnClickListener {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notice);

        initView();

    }

    private void initView(){
        ImageButton mypage = (ImageButton)findViewById(R.id.home);
        ImageButton reshoot = (ImageButton)findViewById(R.id.reshoot);
        mypage.setOnClickListener(this);
        reshoot.setOnClickListener(this);

    }

    @Override
    public void onClick(View view) {
        Animation clickAnimation = AnimationUtils.loadAnimation(NoticeActivity.this,R.anim.clickanimation);
        view.startAnimation(clickAnimation);
        Intent intent;
        switch (view.getId()){
            case R.id.home:
                intent = new Intent(NoticeActivity.this,MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                break;
            case R.id.reshoot:
                intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
//                intent.putExtra(android.provider.MediaStore.EXTRA_VIDEO_QUALITY,0.8);
                intent.putExtra(MediaStore.EXTRA_DURATION_LIMIT,3);
                startActivityForResult(intent,0);
                break;
            default:
                break;

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode==RESULT_OK){
            if(requestCode==0){
                Uri uri = data.getData();
                String path = getPath(uri);
                Log.e("sojeong","filterNum: "+requestCode);
                Intent intent = new Intent(NoticeActivity.this, CheckvideoActivity.class);
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
