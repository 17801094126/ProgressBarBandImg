package com.kz.circleprogressbarbandimg;


import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import static android.graphics.Paint.Style.FILL;
import static android.graphics.Paint.Style.STROKE;
import static java.lang.Math.cos;
import static java.lang.Math.sin;

/**
 * wkz
 */

public class CircleProgressBar extends View {

    private int outsideColor;    //进度的颜色
    private float outsideRadius;    //外圆半径大小
    private int insideColor;    //背景颜色
    private int progressTextColor;   //圆环内文字颜色
    private float progressTextSize;    //圆环内文字大小
    private float progressWidth;    //圆环的宽度
    private int maxProgress;    //最大进度
    private float progress;    //当前进度
    private int direction;    //进度从哪里开始(设置了4个值,上左下右)

    private Paint paint;
    private String progressText;     //圆环内文字
    private Rect rect;


    private ValueAnimator animator;
    //球
    private Bitmap mLititleBitmap;  // 圆点图片
    //圆点画笔
    private Paint mbitmapPaint;
    private Matrix mMatrix;             // 矩阵,用于对图片进行一些操作
    private float[] pos;                // 当前点的实际位置
    private float[] tan;                // 当前点的tangent值,用于计算图片所需旋转的角度
    private String title; //百分比上面的字
    private int titleColor;//百分比上面字的颜色
    private float titleSize;//百分比上面字的大小
    private Rect titleRect;

    enum DirectionEnum {
        LEFT(0, 180.0f),
        TOP(1, 270.0f),
        RIGHT(2, 0.0f),
        BOTTOM(3, 90.0f);

        private final int direction;
        private final float degree;

        DirectionEnum(int direction, float degree) {
            this.direction = direction;
            this.degree = degree;
        }

        public int getDirection() {
            return direction;
        }

        public float getDegree() {
            return degree;
        }

        public boolean equalsDescription(int direction) {
            return this.direction == direction;
        }

        public static DirectionEnum getDirection(int direction) {
            for (DirectionEnum enumObject : values()) {
                if (enumObject.equalsDescription(direction)) {
                    return enumObject;
                }
            }
            return RIGHT;
        }

        public static float getDegree(int direction) {
            DirectionEnum enumObject = getDirection(direction);
            if (enumObject == null) {
                return 0;
            }
            return enumObject.getDegree();
        }
    }

    public CircleProgressBar(Context context) {
        this(context, null);
    }

    public CircleProgressBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CircleProgressBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.CircleProgressBar, defStyleAttr, 0);
        outsideColor = a.getColor(R.styleable.CircleProgressBar_outside_color, ContextCompat.getColor(getContext(), R.color.colorPrimary));
        outsideRadius = a.getDimension(R.styleable.CircleProgressBar_outside_radius, DimenUtil.px2dp(getContext(), 60));
        insideColor = a.getColor(R.styleable.CircleProgressBar_inside_color, ContextCompat.getColor(getContext(), R.color.inside_color));
        progressTextColor = a.getColor(R.styleable.CircleProgressBar_progress_text_color, ContextCompat.getColor(getContext(), R.color.colorPrimary));
        progressTextSize = a.getDimension(R.styleable.CircleProgressBar_progress_text_size, DimenUtil.px2dp(getContext(), 14));
        progressWidth = a.getDimension(R.styleable.CircleProgressBar_progress_width, DimenUtil.px2dp(getContext(), 10));
        progress = a.getFloat(R.styleable.CircleProgressBar_progress, 50.0f);
        maxProgress = a.getInt(R.styleable.CircleProgressBar_max_progress, 100);
        direction = a.getInt(R.styleable.CircleProgressBar_direction, 1);
        title = a.getString(R.styleable.CircleProgressBar_title);
        titleColor = a.getColor(R.styleable.CircleProgressBar_title_color, ContextCompat.getColor(getContext(), R.color.colorPrimary));
        titleSize = a.getDimension(R.styleable.CircleProgressBar_title_size, 10.0f);
        a.recycle();

        paint = new Paint();
        //圆点画笔
        mbitmapPaint = new Paint();
        mbitmapPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP));
        mbitmapPaint.setStyle(FILL);
        mbitmapPaint.setAntiAlias(true);

        //初始化bitmap
        initBitmap();
    }

    private void initBitmap() {
        mMatrix=new Matrix();
        pos = new float[2];
        tan = new float[2];
        mLititleBitmap= ((BitmapDrawable) getResources()
                .getDrawable(R.mipmap.white_round))
                .getBitmap();
    }
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int circlePoint = getWidth() / 2;
        //第一步:画背景(即内层圆)
        paint.setColor(insideColor); //设置圆的颜色
        paint.setStyle(STROKE); //设置空心
        paint.setAlpha(100);
        paint.setStrokeWidth(progressWidth); //设置圆的宽度
        paint.setAntiAlias(true);  //消除锯齿
        canvas.drawCircle(circlePoint, circlePoint, outsideRadius, paint); //画出圆

        //第二步:画进度(圆弧)
        float pro=360 * (progress / maxProgress);
        paint.setColor(outsideColor);  //设置进度的颜色
        paint.setDither(true);//设定是否使用图像抖动处理，会使绘制出来的图片颜色更加平滑和饱满，图像更加清晰
        // 如圆形样式Cap.ROUND,或方形样式Cap.SQUARE
        paint.setStrokeCap(Paint.Cap.ROUND);
        RectF rectF = new RectF(circlePoint - outsideRadius, circlePoint - outsideRadius, circlePoint + outsideRadius, circlePoint + outsideRadius);  //用于定义的圆弧的形状和大小的界限
        canvas.drawArc(rectF, DirectionEnum.getDegree(direction), pro, false, paint);  //根据进度画圆弧

        //绘制白色小星星
        Path orbit = new Path();
        //通过Path类画一个90度（180—270）的内切圆弧路径
        orbit.addArc(rectF, DirectionEnum.getDegree(direction), pro);
        // 创建 PathMeasure
        PathMeasure measure = new PathMeasure(orbit, false);
        measure.getPosTan(measure.getLength() * 1, pos, tan);
        mMatrix.reset();
        mMatrix.postScale(2,2);
        mMatrix.postTranslate(pos[0] - mLititleBitmap.getWidth()  , pos[1] - mLititleBitmap.getHeight()  );   // 将图片绘制中心调整到与当前点重合
        canvas.drawBitmap(mLititleBitmap, mMatrix, mbitmapPaint);//绘制球
        mbitmapPaint.setColor(Color.WHITE);
        //绘制实心小圆圈
        canvas.drawCircle(pos[0], pos[1], 5, mbitmapPaint);

        //绘制百分比上面的数字
        titleRect = new Rect();
        paint.setColor(titleColor);
        paint.setTextSize(titleSize);
        paint.setStrokeWidth(0);
        paint.getTextBounds(title,0,title.length(),titleRect);
        Paint.FontMetricsInt fontMetrics = paint.getFontMetricsInt();
        int titlebaseline = (getMeasuredHeight() - fontMetrics.bottom + fontMetrics.top) / 2 - fontMetrics.top-120;  //获得文字的基准线
        canvas.drawText(title, getMeasuredWidth() / 2 - titleRect.width() / 2, titlebaseline, paint);
        //第三步:画圆环内百分比文字
        rect = new Rect();
        paint.setColor(progressTextColor);
        paint.setTextSize(progressTextSize);
        paint.setStrokeWidth(0);
        progressText = getProgressText();
        paint.getTextBounds(progressText, 0, progressText.length(), rect);
        int baseline = (getMeasuredHeight() - fontMetrics.bottom + fontMetrics.top) / 2 - fontMetrics.top+32;  //获得文字的基准线
        canvas.drawText(progressText, getMeasuredWidth() / 2 - rect.width() / 2, baseline, paint);


        //绘制8个小圆点
        /**
         * 圆点坐标：(1,270)
         * 半径：outside_radius
         * 角度：ao
         * 则圆上任一点为：（x1,y1）
         * //实心Style.FILL
         * x1   =   circlePoint  +   outside_radius   *   cos(ao   *   3.14   /180   )
         * y1   =   circlePoint  +   outside_radius   *   sin(ao   *   3.14   /180   )
         * 第一个坐标    x1=1      y1   =   270
         * 第二个坐标  x1=
         * DirectionEnum.getDegree(direction)
         */
        paint.reset();
        paint.setColor(insideColor); //设置圆的颜色
        paint.setStyle(FILL); //设置空心
        paint.setAntiAlias(true);  //消除锯齿
        canvas.drawCircle((float) getCirclex(getWidth(),36),(float)getCircley(getWidth(),36), 4, paint); //画出圆

    }

    /**
     * 获取小圆点的x坐标
     * @param circlePoint
     * @param ao
     * @return
     */
    private double getCirclex(float circlePoint,int ao){
        return circlePoint  +   outsideRadius   *   cos(ao   *   3.14   /180   );
    }
    /**
     * 获取小圆点的x坐标
     * @param circlePoint
     * @param ao
     * @return
     */
    private double getCircley(float circlePoint,int ao){
        return circlePoint  +   outsideRadius   *   sin(ao   *   3.14   /180   );
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width;
        int height;
        int size = MeasureSpec.getSize(widthMeasureSpec);
        int mode = MeasureSpec.getMode(widthMeasureSpec);

        if (mode == MeasureSpec.EXACTLY) {
            width = size;
        } else {
            width = (int) ((2 * outsideRadius) + progressWidth);
        }
        size = MeasureSpec.getSize(heightMeasureSpec);
        mode = MeasureSpec.getMode(heightMeasureSpec);
        if (mode == MeasureSpec.EXACTLY) {
            height = size;
        } else {
            height = (int) ((2 * outsideRadius) + progressWidth);
        }
        setMeasuredDimension(width, height);
    }

    //中间的进度百分比
    private String getProgressText() {
        return (int) ((progress / maxProgress) * 100) + "%";
    }

    public int getOutsideColor() {
        return outsideColor;
    }

    public void setOutsideColor(int outsideColor) {
        this.outsideColor = outsideColor;
    }

    public float getOutsideRadius() {
        return outsideRadius;
    }

    public void setOutsideRadius(float outsideRadius) {
        this.outsideRadius = outsideRadius;
    }

    public int getInsideColor() {
        return insideColor;
    }

    public void setInsideColor(int insideColor) {
        this.insideColor = insideColor;
    }

    public int getProgressTextColor() {
        return progressTextColor;
    }

    public void setProgressTextColor(int progressTextColor) {
        this.progressTextColor = progressTextColor;
    }

    public float getProgressTextSize() {
        return progressTextSize;
    }

    public void setProgressTextSize(float progressTextSize) {
        this.progressTextSize = progressTextSize;
    }

    public float getProgressWidth() {
        return progressWidth;
    }

    public void setProgressWidth(float progressWidth) {
        this.progressWidth = progressWidth;
    }

    public synchronized int getMaxProgress() {
        return maxProgress;
    }

    public synchronized void setMaxProgress(int maxProgress) {
        if (maxProgress < 0) {
            //此为传递非法参数异常
            throw new IllegalArgumentException("maxProgress should not be less than 0");
        }
        this.maxProgress = maxProgress;
    }

    public synchronized float getProgress() {
        return progress;
    }

    //加锁保证线程安全,能在线程中使用
    public synchronized void setProgress(int progress) {
        if (progress < 0) {
            throw new IllegalArgumentException("progress should not be less than 0");
        }
        if (progress > maxProgress) {
            progress = maxProgress;
        }
        startAnim(progress);
    }

    private void startAnim(float startProgress) {
        animator = ObjectAnimator.ofFloat(0, startProgress);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                CircleProgressBar.this.progress = (float) animation.getAnimatedValue();
                postInvalidate();
            }
        });
        animator.setStartDelay(500);
        animator.setDuration(2000);
        animator.setInterpolator(new LinearInterpolator());
        animator.start();
    }
}
