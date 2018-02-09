package com.example.ruoxuanfu.pathutils;

/**
 * Created by ruoxuan.fu on 2018/1/4.
 * <p>
 * Code is far away from bug with WOW protecting.
 */

public interface OnImageSaveListener {
    void onImageSaveStart();

    void onImageSaveOnEnd(String imagePath);
}
