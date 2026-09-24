package com.yalantis.ucrop.view.widget;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.yalantis.ucrop.R;

import androidx.annotation.ColorInt;
import androidx.core.content.ContextCompat;

public class HorizontalProgressWheelView extends View {

    private final Rect mCanvasClipBounds = new Rect();

    private ScrollingListener mScrollingListener;
    private float mLastTouchedPosition;

    private Paint mProgressLinePaint;
    private Paint mProgressMiddleLinePaint;
    private int mProgressLineWidth, mProgressLineHeight;
    private int mProgressLineMargin;

    private Paint trianglePaint;

    private boolean mScrollStarted;
    private float mTotalScrollDistance;

    private int mMarkerColor;

    public HorizontalProgressWheelView(Context context) {
        this(context, null);
    }

    public HorizontalProgressWheelView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HorizontalProgressWheelView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public HorizontalProgressWheelView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public void setScrollingListener(ScrollingListener scrollingListener) {
        mScrollingListener = scrollingListener;
    }

    public void setMarkerColor(@ColorInt int middleLineColor) {
        mMarkerColor = middleLineColor;
        mProgressMiddleLinePaint.setColor(mMarkerColor);
        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mLastTouchedPosition = event.getX();
                break;
            case MotionEvent.ACTION_UP:
                if (mScrollingListener != null) {
                    mScrollStarted = false;
                    mScrollingListener.onScrollEnd();
                }
                break;
            case MotionEvent.ACTION_MOVE:
                float distance = event.getX() - mLastTouchedPosition;
                if (distance != 0) {
                    if (!mScrollStarted) {
                        mScrollStarted = true;
                        if (mScrollingListener != null) {
                            mScrollingListener.onScrollStart();
                        }
                    }
                    onScrollEvent(event, distance);
                }

                break;
        }
        return true;
    }
    Path trianglePath = new Path();
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.getClipBounds(mCanvasClipBounds);
        float triangleSize = mProgressLineHeight * 0.7f;
        float triangleX = mCanvasClipBounds.centerX();

        Paint trianglePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        trianglePaint.setColor(Color.parseColor("#E84749"));
        trianglePaint.setStyle(Paint.Style.FILL);

        float topVTriangle = mCanvasClipBounds.centerY() + mProgressLineHeight * 0.5f + triangleSize * 0.3f;
        trianglePath.reset();
        trianglePath.moveTo(triangleX, topVTriangle);
        trianglePath.lineTo(triangleX - triangleSize/2, topVTriangle + triangleSize);
        trianglePath.lineTo(triangleX + triangleSize/2, topVTriangle + triangleSize);
        trianglePath.close();

        canvas.drawPath(trianglePath, trianglePaint);

        drawMarkings(canvas);
    }


    private void drawMarkings(Canvas canvas) {
        int linesCount = mCanvasClipBounds.width() / (mProgressLineWidth + mProgressLineMargin) + 4;
        float topY = mCanvasClipBounds.centerY() - mProgressLineHeight * 0.5f;

        float deltaX = mTotalScrollDistance % (mProgressLineWidth + mProgressLineMargin);

        for (int i = -linesCount/2; i < linesCount/2; i++) {
            float lineX = mCanvasClipBounds.centerX() + i * (mProgressLineWidth + mProgressLineMargin) - deltaX;

            int absolutePosition = Math.round((mTotalScrollDistance + i * (mProgressLineWidth + mProgressLineMargin)) / (mProgressLineWidth + mProgressLineMargin));

            float distanceFromCenter = Math.abs(lineX - mCanvasClipBounds.centerX());
            float alphaFactor = 1 - Math.min(distanceFromCenter / (mCanvasClipBounds.width() * 0.4f), 1.0f);
            mProgressLinePaint.setAlpha((int) (255 * alphaFactor));

            float lineHeight = mProgressLineHeight * 0.5f;
            if (absolutePosition % 5 == 0) {
                lineHeight = mProgressLineHeight;
            }
            canvas.drawLine(
                    lineX,
                    topY,
                    lineX,
                    topY + lineHeight,
                    mProgressLinePaint);
        }
    }

    private void onScrollEvent(MotionEvent event, float distance) {
        mTotalScrollDistance -= distance;
        postInvalidate();
        mLastTouchedPosition = event.getX();
        if (mScrollingListener != null) {
            mScrollingListener.onScroll(-distance, mTotalScrollDistance);
        }
    }

    private void init() {
        mMarkerColor = ContextCompat.getColor(getContext(), R.color.azcrop_color_widget_rotate_mid_line);

        mProgressLineWidth = getContext().getResources().getDimensionPixelSize(R.dimen.azcrop_width_horizontal_wheel_progress_line);
        mProgressLineHeight = getContext().getResources().getDimensionPixelSize(R.dimen.azcrop_height_horizontal_wheel_progress_line);
        mProgressLineMargin = getContext().getResources().getDimensionPixelSize(R.dimen.azcrop_margin_horizontal_wheel_progress_line);

        mProgressLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mProgressLinePaint.setStyle(Paint.Style.STROKE);
        mProgressLinePaint.setStrokeWidth(mProgressLineWidth);
        mProgressLinePaint.setColor(getResources().getColor(R.color.azcrop_color_progress_wheel_line));

        mProgressMiddleLinePaint = new Paint(mProgressLinePaint);
        mProgressMiddleLinePaint.setColor(mMarkerColor);
        mProgressMiddleLinePaint.setStrokeCap(Paint.Cap.ROUND);
        mProgressMiddleLinePaint.setStrokeWidth(getContext().getResources().getDimensionPixelSize(R.dimen.azcrop_width_middle_wheel_progress_line));

        trianglePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        trianglePaint.setColor(mMarkerColor);
        trianglePaint.setStyle(Paint.Style.FILL);
        mTotalScrollDistance = 0;

    }

    public void reset() {
        mTotalScrollDistance = 0;
        mLastTouchedPosition = 0;
        invalidate();
    }


    public interface ScrollingListener {

        void onScrollStart();

        void onScroll(float delta, float totalDistance);

        void onScrollEnd();
    }

}
