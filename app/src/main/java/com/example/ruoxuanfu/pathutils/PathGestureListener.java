package com.example.ruoxuanfu.pathutils;

import android.view.GestureDetector;
import android.view.MotionEvent;

/**
 * Created by ruoxuan.fu on 2018/1/4.
 * <p>
 * Code is far away from bug with WOW protecting.
 */

public class PathGestureListener extends GestureDetector.SimpleOnGestureListener {
    private PathDrawInterface pathDrawInterface;

    public PathGestureListener(PathDrawInterface pathDrawInterface) {
        super();
        this.pathDrawInterface = pathDrawInterface;
    }

    @Override
    public boolean onDoubleTap(MotionEvent e) {
        if (pathDrawInterface != null) {
            return pathDrawInterface.onDoubleTap(e);
        }
        return super.onDoubleTap(e);
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        if (pathDrawInterface != null) {
            return pathDrawInterface.onSingleTapConfirmed(e);
        }
        return super.onSingleTapConfirmed(e);
    }

    @Override
    public void onLongPress(MotionEvent e) {
        if (pathDrawInterface != null) {
            pathDrawInterface.onLongPress(e);
        }
    }
}
