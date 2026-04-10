package com.example.expensemanagement.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import java.util.List;

/**
 * Custom View vẽ mini chart đơn giản giống Apple Stocks.
 */
public class MiniChartView extends View {

    private Paint linePaint;
    private Paint fillPaint;
    private List<Float> points;
    private boolean isPositive = true;

    public MiniChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MiniChartView(Context context) {
        super(context);
        init();
    }

    private void init() {
        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(2f);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);

        fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fillPaint.setStyle(Paint.Style.FILL);
    }

    public void setData(List<Float> points, boolean isPositive) {
        this.points = points;
        this.isPositive = isPositive;

        int color = isPositive ? 0xFF30D158 : 0xFFFF453A; // xanh/đỏ iOS style
        linePaint.setColor(color);
        fillPaint.setColor(isPositive ? 0x2230D158 : 0x22FF453A);

        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (points == null || points.size() < 2) return;

        int w = getWidth(), h = getHeight();
        float min = Float.MAX_VALUE, max = Float.MIN_VALUE;
        for (float p : points) { min = Math.min(min, p); max = Math.max(max, p); }
        if (max == min) max = min + 1;

        float stepX = (float) w / (points.size() - 1);
        float padding = 4f;

        Path linePath = new Path();
        Path fillPath = new Path();

        for (int i = 0; i < points.size(); i++) {
            float x = i * stepX;
            float y = padding + (1f - (points.get(i) - min) / (max - min)) * (h - padding * 2);
            if (i == 0) { linePath.moveTo(x, y); fillPath.moveTo(x, y); }
            else        { linePath.lineTo(x, y); fillPath.lineTo(x, y); }
        }

        // Close fill path
        fillPath.lineTo((points.size() - 1) * stepX, h);
        fillPath.lineTo(0, h);
        fillPath.close();

        canvas.drawPath(fillPath, fillPaint);
        canvas.drawPath(linePath, linePaint);
    }
}