package com.example.ruoxuanfu.pathutils;

import android.view.MotionEvent;

/**
 * Created by ruoxuan.fu on 2018/1/4.
 * <p>
 * Code is far away from bug with WOW protecting.
 */

public interface PathDrawInterface {
    boolean onSingleTapConfirmed(MotionEvent e);

    void onLongPress(MotionEvent e);

    boolean onDoubleTap(MotionEvent e);
}
