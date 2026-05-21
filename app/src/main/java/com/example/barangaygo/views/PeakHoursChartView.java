package com.example.barangaygo.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple bar chart showing booking volume per hour (7 AM – 5 PM).
 * Peak hours (≥ 75th percentile) are highlighted in orange.
 */
public class PeakHoursChartView extends View {

    private static final int START_H = 7;
    private static final int END_H   = 17; // 5 PM

    private static final int COLOR_BAR      = 0xFF5a1102; // maroon_800
    private static final int COLOR_PEAK     = 0xFFc4850a; // cream_600
    private static final int COLOR_LABEL    = 0xFF94A3B8; // gray_400
    private static final int COLOR_COUNT    = 0xFF1E293B; // gray_800
    private static final int COLOR_AXIS     = 0xFFE2E8F0; // gray_200

    private final Paint barPaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint countPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint axisPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);

    private Map<Integer, Integer> hourData = new HashMap<>();

    public PeakHoursChartView(Context context) {
        super(context);
        init();
    }

    public PeakHoursChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PeakHoursChartView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        float density = getResources().getDisplayMetrics().density;

        barPaint.setStyle(Paint.Style.FILL);

        labelPaint.setColor(COLOR_LABEL);
        labelPaint.setTextSize(9 * density);
        labelPaint.setTextAlign(Paint.Align.CENTER);

        countPaint.setColor(COLOR_COUNT);
        countPaint.setTextSize(8 * density);
        countPaint.setTextAlign(Paint.Align.CENTER);

        axisPaint.setColor(COLOR_AXIS);
        axisPaint.setStrokeWidth(1 * density);
        axisPaint.setStyle(Paint.Style.STROKE);
    }

    /** Pass hour-to-count data (keys 0–23). Non-working hours are ignored. */
    public void setData(Map<Integer, Integer> data) {
        this.hourData = data != null ? data : new HashMap<>();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float density = getResources().getDisplayMetrics().density;
        int w = getWidth();
        int h = getHeight();

        float topPad    = 20 * density;
        float bottomPad = 28 * density;
        float leftPad   = 8  * density;
        float rightPad  = 8  * density;

        float chartW = w - leftPad - rightPad;
        float chartH = h - topPad - bottomPad;

        // Draw baseline
        canvas.drawLine(leftPad, topPad + chartH, w - rightPad, topPad + chartH, axisPaint);

        int numBars = END_H - START_H + 1;
        float slotW = chartW / numBars;
        float gap   = slotW * 0.3f;
        float barW  = slotW - gap;

        // Determine max and 75th-percentile threshold for peak colouring
        int maxVal = 1;
        for (int hr = START_H; hr <= END_H; hr++) {
            int v = hourData.containsKey(hr) ? hourData.get(hr) : 0;
            if (v > maxVal) maxVal = v;
        }

        // 75th percentile
        int[] vals = new int[numBars];
        for (int i = 0; i < numBars; i++) {
            vals[i] = hourData.containsKey(START_H + i) ? hourData.get(START_H + i) : 0;
        }
        int[] sorted = vals.clone();
        java.util.Arrays.sort(sorted);
        int p75 = sorted[(int) Math.floor(numBars * 0.75)];

        for (int i = 0; i < numBars; i++) {
            int hr = START_H + i;
            int count = hourData.containsKey(hr) ? hourData.get(hr) : 0;

            float left   = leftPad + i * slotW + gap / 2;
            float right  = left + barW;
            float barH   = count > 0 ? (float) count / maxVal * chartH : 0;
            float top    = topPad + chartH - barH;
            float bottom = topPad + chartH;

            // Bar colour
            boolean isPeak = count > 0 && count >= p75 && p75 > 0;
            barPaint.setColor(isPeak ? COLOR_PEAK : COLOR_BAR);

            if (barH > 0) {
                float cornerR = 3 * density;
                canvas.drawRoundRect(new RectF(left, top, right, bottom),
                    cornerR, cornerR, barPaint);

                // Count label above bar (only if bar is tall enough)
                if (barH > 14 * density) {
                    canvas.drawText(String.valueOf(count),
                        left + barW / 2, top - 3 * density, countPaint);
                }
            }

            // Hour label
            String label = formatHour(hr);
            canvas.drawText(label, left + barW / 2, h - 6 * density, labelPaint);
        }
    }

    private String formatHour(int h) {
        if (h == 12) return "12p";
        if (h < 12)  return h + "a";
        return (h - 12) + "p";
    }
}