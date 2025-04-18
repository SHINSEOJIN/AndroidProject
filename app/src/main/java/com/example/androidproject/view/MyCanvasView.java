package com.example.androidproject.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import com.example.androidproject.R;

public class MyCanvasView extends View {

    private int score = 0;
    private int customColor = 0xFF3F51B5;
    private Paint backgroundPaint;
    private Paint scorePaint;

    public MyCanvasView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.MyCanvasView);
        customColor = a.getColor(R.styleable.MyCanvasView_customColor, 0xFF3F51B5);
        a.recycle();

        backgroundPaint = new Paint();
        backgroundPaint.setColor(0xFFE0E0E0); // 회색 배경
        backgroundPaint.setStyle(Paint.Style.STROKE);
        backgroundPaint.setStrokeWidth(30);
        backgroundPaint.setAntiAlias(true);

        scorePaint = new Paint();
        scorePaint.setColor(customColor);
        scorePaint.setStyle(Paint.Style.STROKE);
        scorePaint.setStrokeWidth(30);
        scorePaint.setAntiAlias(true);
    }

    public void setScore(int score) {
        this.score = score;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int centerX = getWidth() / 2;
        int centerY = getHeight() / 2;
        int radius = Math.min(centerX, centerY) - 20;

        canvas.drawCircle(centerX, centerY, radius, backgroundPaint);

        float sweepAngle = (score / 100f) * 360f;
        canvas.drawArc(centerX - radius, centerY - radius, centerX + radius, centerY + radius,
                -90, sweepAngle, false, scorePaint);
    }
}
