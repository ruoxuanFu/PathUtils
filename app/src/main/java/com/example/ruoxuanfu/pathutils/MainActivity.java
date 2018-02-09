package com.example.ruoxuanfu.pathutils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private TextView mTvSave;
    private TextView mTvClear;
    private ImageView mImgEraser;
    private ImageView mImgPen;
    private PathDrawView mPathDrawView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initData();
    }

    private void initView() {
        mTvSave = (TextView) findViewById(R.id.tvSave);
        mTvClear = (TextView) findViewById(R.id.tvClear);
        mImgEraser = (ImageView) findViewById(R.id.imgEraser);
        mImgPen = (ImageView) findViewById(R.id.imgPen);
        mPathDrawView = (PathDrawView) findViewById(R.id.pathDrawView);

        mTvSave.setOnClickListener(this);
        mTvClear.setOnClickListener(this);
        mImgEraser.setOnClickListener(this);
        mImgPen.setOnClickListener(this);
    }

    private void initData() {
        mPathDrawView.setPathWidth(2);
        mPathDrawView.setMode(PathDrawView.PATH_UNLOCK);
        mPathDrawView.setCurPathColor(getResources().getColor(R.color.black));
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.background);
        mPathDrawView.setBackground(new BitmapDrawable(null, bitmap));
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tvSave:
                Bitmap bitmap = Bitmap.createBitmap(mPathDrawView.getDrawingCache());
                break;
            case R.id.tvClear:
                mPathDrawView.cleanCanvas();
                break;
            case R.id.imgEraser:
                mPathDrawView.setEraserEnable(true);
                break;
            case R.id.imgPen:
                mPathDrawView.setEraserEnable(false);
                break;
        }
    }
}
