package com.example.ruoxuanfu.pathutils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;

/**
 * Created by ruoxuan.fu on 2018/1/4.
 * <p>
 * Code is far away from bug with WOW protecting.
 */

public class PathUtil {
    protected static WeakReference<Bitmap> zoomBitmap(Bitmap bitmap, int width, int height) {
        if (bitmap == null) {
            return null;
        }
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        Matrix matrix = new Matrix();
        float scaleWidth = ((float) width / w);
        float scaleHeight = ((float) height / h);
        matrix.postScale(scaleWidth, scaleHeight);
        Bitmap newBitmap = null;
        try {
            newBitmap = Bitmap.createBitmap(bitmap, 0, 0, w, h, matrix, true);
        } catch (OutOfMemoryError | Exception e) {
            e.printStackTrace();
        }
        return new WeakReference<>(newBitmap);
    }

    protected static WeakReference<Bitmap> drawableToBitmap(Drawable drawable) {
        if (drawable == null) {
            return null;
        }

        int w = drawable.getIntrinsicWidth();
        int h = drawable.getIntrinsicHeight();

        if (w <= 0 || h <= 0) {
            return null;
        }

        Bitmap.Config config = drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888
                : Bitmap.Config.RGB_565;
        Bitmap bitmap = Bitmap.createBitmap(w, h, config);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, w, h);
        drawable.draw(canvas);
        return new WeakReference<>(bitmap);
    }

    protected static Bitmap readBitMapById(Context context, int resId, int shrinkScale) {
        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inPreferredConfig = Bitmap.Config.RGB_565;
        opt.inPurgeable = true;
        opt.inInputShareable = true;
        opt.inSampleSize = shrinkScale;
        InputStream is = context.getResources().openRawResource(resId);
        Bitmap bitmap;
        try {
            bitmap = BitmapFactory.decodeStream(is, null, opt);
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return bitmap;
    }
}
