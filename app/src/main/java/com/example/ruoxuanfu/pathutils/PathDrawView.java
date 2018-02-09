package com.example.ruoxuanfu.pathutils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

/**
 * Created by ruoxuan.fu on 2018/1/4.
 * <p>
 * Code is far away from bug with WOW protecting.
 */

public class PathDrawView extends android.support.v7.widget.AppCompatImageView {
    //Default
    private static final int DEFAULT_BG_COLOR = Color.TRANSPARENT;//默认背景颜色
    private static final int DEFAULT_PATH_COLOR = Color.RED;//默认笔触颜色
    private static final int DEFAULT_PATH_WIDTH = 1;//默认笔触宽度

    public static final int PATH_LOCK = 1;//锁定绘图
    public static final int PATH_UNLOCK = 2;//可绘图
    public static final int PATH_PAN_ONLY = 3;//只认笔绘图
    private static final int ERASER_RADIUS = 30;//橡皮擦半径

    //Util
    private Context mContext;
    private GestureDetector mGestureDetector;

    //Flag
    private int viewWidth = -1;
    private int viewHeight = -1;

    private int curMode = PATH_UNLOCK;
    private boolean eraserEnable = false;

    private float preX;
    private float preY;

    private boolean customSetBit = true;//内部调用setBitmap不走update方法
    private boolean outOfBound;//出边界再进入重走down
    private boolean refreshBitmap;//在首次进入和手抬起时刷新bitmap。过多调用setBitmap会内存变大

    //笔迹绘制
    private Paint cachePaint = new Paint(Paint.DITHER_FLAG);
    private Path cachePath = new Path();//实际path
    private Matrix cacheMatrix = new Matrix();
    private Path cacheDraw = new Path();//转换过的path
    private WeakReference<Bitmap> cacheBitmap = null;
    private Canvas cacheCanvas = null;
    private RectF cacheRectF = new RectF();

    //笔迹删除
    private Paint deletePaint = new Paint(Paint.DITHER_FLAG);
    private Path deletePath = new Path();
    private Bitmap deleteBitmap;
    private Paint deleteBitmapPaint = new Paint();

    //笔迹保存
    private String imagePath;
    private Subject<String> imagePathSubject;
    private Disposable disposable;
    private OnImageSaveListener listener;

    public PathDrawView(Context context, AttributeSet set) {
        super(context, set);
        mContext = context;

        cachePaint.setAntiAlias(true);
        cachePaint.setDither(true);
        cachePaint.setColor(DEFAULT_PATH_COLOR);
        cachePaint.setStyle(Paint.Style.STROKE);
        cachePaint.setStrokeJoin(Paint.Join.ROUND);
        cachePaint.setStrokeCap(Paint.Cap.ROUND);
        cachePaint.setStrokeWidth(DEFAULT_PATH_WIDTH);

        deleteBitmapPaint.setAntiAlias(true);
        deleteBitmapPaint.setDither(true);
        deleteBitmapPaint.setColor(Color.BLACK);
        deleteBitmapPaint.setStyle(Paint.Style.STROKE);
        deleteBitmapPaint.setStrokeJoin(Paint.Join.ROUND);
        deleteBitmapPaint.setStrokeCap(Paint.Cap.ROUND);
        deleteBitmapPaint.setStrokeWidth(1);

        deletePaint.setAntiAlias(true);
        deletePaint.setDither(true);
        deletePaint.setColor(Color.RED);//设置画笔颜色
        deletePaint.setStrokeWidth(ERASER_RADIUS);//设置描边宽度
        //BlurMaskFilter bmf = new BlurMaskFilter(5, BlurMaskFilter.Blur.NORMAL);//指定了一个模糊的样式和半径来处理Paint的边缘。
        //deletePaint.setMaskFilter(bmf);//为Paint分配边缘效果。
        deletePaint.setStyle(Paint.Style.STROKE);//让画出的图形是空心的
        //它的作用是用此画笔后，画笔划过的痕迹就变成透明色了。画笔设置好了后，就可以调用该画笔进行橡皮痕迹的绘制了
        deletePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));
        deletePaint.setStrokeJoin(Paint.Join.ROUND);//设置结合处的样子 Miter:结合处为锐角， Round:结合处为圆弧：BEVEL：结合处为直线。
        deletePaint.setStrokeCap(Paint.Cap.ROUND);//画笔笔刷类型   圆形形状

        cacheCanvas = new Canvas();
        cacheCanvas.setDrawFilter(new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG));
    }

    //***********************public method*******************//

    /**
     * 设置长按、双击、点击监听
     *
     * @param pathListener
     */
    public void setPathListener(PathDrawInterface pathListener) {
        if (pathListener != null) {
            PathGestureListener gestureListener = new PathGestureListener(pathListener);
            mGestureDetector = new GestureDetector(mContext, gestureListener);
            mGestureDetector.setOnDoubleTapListener(gestureListener);
        } else if (mGestureDetector != null) {
            mGestureDetector.setOnDoubleTapListener(null);
            mGestureDetector = null;
        }
    }

    /**
     * 设置当前笔触模式：可绘图、不可绘图、只可笔触绘图
     *
     * @param mode
     */
    public void setMode(int mode) {
        if (mode != PATH_LOCK && mode != PATH_UNLOCK && mode != PATH_PAN_ONLY) {
            throw new IllegalArgumentException("mode not exist");
        }
        curMode = mode;
    }

    /**
     * 获取当前模式
     *
     * @return
     */
    public int getMode() {
        return curMode;
    }

    /**
     * 设置是否是橡皮
     *
     * @param enable
     */
    public void setEraserEnable(boolean enable) {
        eraserEnable = enable;
    }

    /**
     * 获取是否是橡皮擦模式
     *
     * @return
     */
    public boolean getEraserEnable() {
        return eraserEnable;
    }

    /**
     * 清空绘图区域
     */
    public void cleanCanvas() {
        clear();
        updateCanvasBean();
        saveImage();
    }

    /**
     * 设置笔触颜色
     *
     * @param color
     */
    public void setCurPathColor(@ColorInt int color) {
        cachePaint.setColor(color);
    }

    /**
     * 设置笔触宽度
     *
     * @param width
     */
    public void setPathWidth(int width) {
        if (width < 0) {
            throw new IllegalArgumentException("width can't less then 0");
        }
        cachePaint.setStrokeWidth(width);
    }

    /**
     * 设置画布宽度
     *
     * @param width
     */
    public void setWidth(int width) {
        if (width < 0) {
            throw new IllegalArgumentException("width can't less then 0");
        }
        viewWidth = width;
        if (getWidth() != 0 || getHeight() != 0) {
            requestLayout();
        }
    }

    /**
     * 设置画布高度
     *
     * @param height
     */
    public void setHeight(int height) {
        if (height < 0) {
            throw new IllegalArgumentException("height can't less then 0");
        }
        viewHeight = height;
        if (getWidth() != 0 || getHeight() != 0) {
            requestLayout();
        }
    }

    /**
     * 同时设置宽高
     *
     * @param width
     * @param height
     */
    public void setMeasured(int width, int height) {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("width & height can't less then 0");
        }
        viewWidth = width;
        viewHeight = height;
        if (getWidth() != 0 || getHeight() != 0) {
            requestLayout();
        }
    }

    /**
     * 设置保存路径
     * 笔迹会边写边保存成文件
     *
     * @param imagePath
     * @param listener
     */
    public void setImagePath(String imagePath, OnImageSaveListener listener) {
        if (imagePath == null) {
            throw new IllegalArgumentException("imagePath can not null");
        }
        this.imagePath = imagePath;
        this.listener = listener;
        if (imagePathSubject == null) {
            imagePathSubject = PublishSubject.create();
            imagePathSubject
                    .throttleLast(500, TimeUnit.MILLISECONDS, Schedulers.io())
                    .subscribe(new Observer<String>() {
                        @Override
                        public void onSubscribe(Disposable d) {
                            disposable = d;
                        }

                        @Override
                        public void onNext(String value) {
                            try {
                                if (PathDrawView.this.listener != null) {
                                    PathDrawView.this.listener.onImageSaveStart();
                                }
                                OutputStream outputStream = new FileOutputStream(value);
                                cacheBitmap.get().compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                                outputStream.close();
                                if (PathDrawView.this.listener != null) {
                                    PathDrawView.this.listener.onImageSaveOnEnd(value);
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onError(Throwable e) {

                        }

                        @Override
                        public void onComplete() {

                        }
                    });
        }
    }

    //***********************public method*******************//

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (viewWidth != -1 && viewHeight != -1) {
            setMeasuredDimension(viewWidth, viewHeight);
        } else if (viewWidth != -1) {
            setMeasuredDimension(viewWidth, heightMeasureSpec);
        } else if (viewHeight != -1) {
            setMeasuredDimension(widthMeasureSpec, viewHeight);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (changed || customSetBit) {
            if (getDrawable() != null) {
                Rect g = getDrawable().getBounds();
                cacheRectF.set(g.left, g.top, g.right,
                        g.bottom);
                getImageMatrix().mapRect(cacheRectF);
            } else {
                cacheRectF.set(0, 0, getWidth(),
                        getHeight());
            }
            updateCanvasBean();
        }
        customSetBit = true;
    }

    private void clear() {
        customSetBit = false;
        super.setImageBitmap(null);
        if (cacheBitmap != null && cacheBitmap.get() != null && !cacheBitmap.get().isRecycled()) {
            cacheBitmap.get().recycle();
            cacheBitmap.clear();
            cacheBitmap = null;
        }
    }

    private void updateCanvasBean() {
        if (getWidth() != 0 && getHeight() != 0) {
            try {
                WeakReference<Bitmap> newBitmap = PathUtil.drawableToBitmap(getDrawable());
                clear();
                cacheBitmap = newBitmap;
                if (cacheBitmap == null || cacheBitmap.get() == null || cacheBitmap.get().isRecycled()) {
                    Bitmap bitmap = Bitmap.createBitmap(getWidth(), getHeight(),
                            Bitmap.Config.ARGB_4444);
                    cacheBitmap = new WeakReference<>(bitmap);
                }
                cacheCanvas.setBitmap(cacheBitmap.get());
            } catch (Exception e) {
                e.printStackTrace();
            }
            cacheCanvas.drawColor(DEFAULT_BG_COLOR);
            refreshBitmap = true;
            invalidate();
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        deleteBitmap = PathUtil.readBitMapById(mContext, R.drawable.pathdraw_eraser, 1);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        clear();
        if (deleteBitmap != null && !deleteBitmap.isRecycled()) {
            deleteBitmap.recycle();
            deleteBitmap = null;
        }
        if (disposable != null && !disposable.isDisposed()) {
            disposable.dispose();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mGestureDetector != null) {
            mGestureDetector.onTouchEvent(event);
        }
        if (curMode == PATH_LOCK) {
            return super.onTouchEvent(event);
        } else if (curMode == PATH_PAN_ONLY
                && event.getToolType(0) != MotionEvent.TOOL_TYPE_STYLUS) {
            return super.onTouchEvent(event);
        }

        getParent().requestDisallowInterceptTouchEvent(true);

        float x = event.getX();
        float y = event.getY();

        if (getEraserEnable()) {
            x = x - deleteBitmap.getWidth() / 2;
            y = y - deleteBitmap.getHeight() / 2;
        }

        int action = event.getAction();

        if (x < cacheRectF.left || x > cacheRectF.right
                || y < cacheRectF.top || y > cacheRectF.bottom) {
            if (!outOfBound && action == MotionEvent.ACTION_MOVE) {
                outOfBound = true;
                x = preX;
                y = preY;
                action = MotionEvent.ACTION_UP;
            } else {
                return super.onTouchEvent(event);
            }
        } else {
            if (outOfBound && action == MotionEvent.ACTION_MOVE) {
                action = MotionEvent.ACTION_DOWN;
            }
            outOfBound = false;
        }

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                touchDown(x, y);
                break;
            case MotionEvent.ACTION_MOVE:
                touchMove(x, y);
                break;
            case MotionEvent.ACTION_UP:
                touchUp(x, y);
                break;
        }

        if (getEraserEnable()) {
            eraserTouch(x, y, event);
        }

        switch (action) {
            case MotionEvent.ACTION_UP:
                saveImage();
                break;
        }
        invalidate();
        return true;
    }

    private void touchDown(float x, float y) {
        cachePath.reset();
        cachePath.moveTo(x, y);
        preX = x;
        preY = y;
    }

    private void touchMove(float x, float y) {
        if (Math.abs(x - preX) > 0 || Math.abs(y - preY) > 0) {
            cachePath.quadTo(preX, preY, (x + preX) / 2, (y + preY) / 2);
            preX = x;
            preY = y;
        }
    }

    private void touchUp(float x, float y) {
        pathMatrixInvert();
        if (!getEraserEnable()) {
            cacheCanvas.drawPath(cacheDraw, cachePaint);
        }
        cachePath.reset();
        refreshBitmap = true;
    }

    private void eraserTouch(float x, float y, MotionEvent event) {
        deletePath.reset();
        if (event.getAction() != MotionEvent.ACTION_UP) {
            deletePath.addCircle(x, y, ERASER_RADIUS / 2, Path.Direction.CW);
        }
    }

    private void saveImage() {
        if (imagePath != null && cacheBitmap.get() != null) {
            imagePathSubject.onNext(imagePath);
        }
    }

    private void pathMatrixInvert() {
        cacheMatrix.reset();
        cacheDraw.reset();
        getImageMatrix().invert(cacheMatrix);
        cachePath.transform(cacheMatrix, cacheDraw);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!getEraserEnable()) {
            canvas.drawPath(cachePath, cachePaint);
        } else if (!deletePath.isEmpty()) {
            pathMatrixInvert();
            cacheCanvas.drawPath(cacheDraw, deletePaint);
            //canvas.drawPath(deletePath, deleteBitmapPaint);
            if (deleteBitmap != null && !deleteBitmap.isRecycled()) {
                canvas.drawBitmap(deleteBitmap, preX - deleteBitmap.getWidth() / 2
                        , preY - deleteBitmap.getHeight() / 2, deleteBitmapPaint);
            }
        }

        if (refreshBitmap && cacheBitmap != null && cacheBitmap.get() != null && !cacheBitmap.get().isRecycled()) {
            customSetBit = false;
            setImageBitmap(cacheBitmap.get());
            refreshBitmap = false;
        }
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        Log.d("onSizeChanged", "width:" + width);
        Log.d("onSizeChanged", "height:" + height);
        Log.d("onSizeChanged", "oldWidth:" + oldWidth);
        Log.d("onSizeChanged", "oldHeight:" + oldHeight);
        Log.d("onSizeChanged", "-----------------");
    }

    @Override
    public void setBackground(Drawable background) {
        super.setBackground(background);
        int width = getMeasuredWidth();
        int height = getMeasuredWidth();

        int dgWidth = background.getIntrinsicWidth();
        int dgHeight = background.getIntrinsicHeight();
        Log.d("setBackground", "viewWidth:" + viewWidth);
        Log.d("setBackground", "viewHeight:" + viewHeight);
        Log.d("setBackground", "width:" + width);
        Log.d("setBackground", "height:" + height);
        Log.d("setBackground", "dWidth:" + dgWidth);
        Log.d("setBackground", "dHeight:" + dgHeight);
        Log.d("setBackground", "-----------------");
    }
}