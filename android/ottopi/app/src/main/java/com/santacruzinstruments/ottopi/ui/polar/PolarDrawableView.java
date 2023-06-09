package com.santacruzinstruments.ottopi.ui.polar;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.NonNull;

import com.santacruzinstruments.ottopi.R;
import com.santacruzinstruments.ottopi.navengine.NavComputerOutput;
import com.santacruzinstruments.ottopi.navengine.Targets;
import com.santacruzinstruments.ottopi.navengine.geo.Angle;
import com.santacruzinstruments.ottopi.navengine.geo.Speed;
import com.santacruzinstruments.ottopi.navengine.polars.PolarTable;

import java.util.LinkedList;
import java.util.Locale;

import timber.log.Timber;

public class PolarDrawableView extends View {

    private final float historyEntryRadius;
    private final float currentPointRadius;
    private final float markWidth;
    private final int axisColor;
    private final int axisTargetColor;

    private static class HistoryEntry {
        final double speed;
        final double angle;
        final float x;
        final float y;

        private HistoryEntry(double speed, double angle) {
            this.speed = speed;
            this.angle = angle;
            this.x = PolarDrawableView.polarX( - angle, speed);
            this.y = PolarDrawableView.polarY(angle, speed);
        }
    }

    private int viewHeight;
    private int viewWidth;
    private float x0=0, y0=0;

    private final Paint axisMajorPaint = new Paint(0);
    private final Paint axisMinorPaint = new Paint(0);
    private final Paint axisMajorLabelPaint = new Paint(0);
    private final Paint axisLabelPaint = new Paint(0);
    private final Paint polarCurvePaint = new Paint(0);
    private final Paint targetAnglePaint = new Paint(0);
    private final Paint vmgPaint = new Paint(0);
    private final Paint historyPaint = new Paint(0);
    private final Paint twaPaint = new Paint(0);
    private final Paint currentPointPaint = new Paint(0);
    private final Paint markPaint = new Paint(0);
    private final Paint portTwaStatsPaint = new Paint(0);
    private final Paint stbdTwaStatsPaint = new Paint(0);

    private final float[] polarCurvePts;
    private final float[] upwindTargetAnglePts = new float[4 * 2];
    private final float[] downWindTargetAnglePts = new float[4 * 2];
    private final float[] upwindVmgPts = new float[2 * 2];
    private final float[] downWindVmgPts = new float[2 * 2];

    private final PointF markPoint = new PointF();

    private float markAngle = 0;
    private boolean isMarkValid = false;

    private final double startAngle = 10;
    private final double angleStep = 1;

    private Angle medianPortTwa = Angle.INVALID;
    private Angle portIqr = Angle.INVALID;
    private Angle medianStbdTwa = Angle.INVALID;
    private Angle stbdIqr = Angle.INVALID;
    private final RectF twaStatsArgBounds = new RectF();

    private double maxSpeed = 10;
    private float pixInKtsScale = 1;
    private float targetUpwindSpeed = 0;
    private float targetDownwindSpeed = 0;

    private ZoomLevel zoomLevel = ZoomLevel.FULL;

    private static final int HISTORY_LEN = 10;
    private final LinkedList<HistoryEntry> history = new LinkedList<>();
    private boolean hasValidInput = false;

    public PolarDrawableView(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.getTheme().obtainStyledAttributes(attrs,
                R.styleable.PolarDrawableView, 0, 0);

        final DashPathEffect dashPathEffect = new DashPathEffect(new float[]{20, 20}, 0);

        axisColor = a.getColor(R.styleable.PolarDrawableView_axis_color, Color.GRAY);
        axisTargetColor = a.getColor(R.styleable.PolarDrawableView_axis_target_color, Color.WHITE);

        axisMajorPaint.setStyle(Paint.Style.STROKE);
        axisMajorPaint.setColor(axisColor);
        axisMajorPaint.setStrokeWidth(a.getFloat(R.styleable.PolarDrawableView_axis_width, 6.f));

        axisMinorPaint.setStyle(Paint.Style.STROKE);
        axisMinorPaint.setColor(axisColor);
        axisMinorPaint.setStrokeWidth(a.getFloat(R.styleable.PolarDrawableView_axis_minor_width, 3.f));
        axisMinorPaint.setStrokeJoin(Paint.Join.BEVEL);
        axisMinorPaint.setPathEffect(dashPathEffect);

        axisLabelPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        axisLabelPaint.setColor(a.getColor(R.styleable.PolarDrawableView_axis_color, Color.GRAY));
        axisLabelPaint.setTypeface(Typeface.DEFAULT);
        axisLabelPaint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 24, getResources().getDisplayMetrics()));

        axisMajorLabelPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        axisMajorLabelPaint.setColor(axisTargetColor);
        axisMajorLabelPaint.setTypeface(Typeface.DEFAULT);
        axisMajorLabelPaint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 32, getResources().getDisplayMetrics()));

        polarCurvePaint.setColor(a.getColor(R.styleable.PolarDrawableView_polar_curve_color, Color.GRAY));
        polarCurvePaint.setStrokeWidth(a.getFloat(R.styleable.PolarDrawableView_polar_curve_width, 6.f));
        polarCurvePaint.setStyle(Paint.Style.STROKE);
        polarCurvePaint.setStrokeJoin(Paint.Join.BEVEL);

        targetAnglePaint.setColor(a.getColor(R.styleable.PolarDrawableView_target_angle_color, Color.GRAY));
        targetAnglePaint.setStrokeWidth(a.getFloat(R.styleable.PolarDrawableView_target_angle_width, 6.f));
        targetAnglePaint.setStyle(Paint.Style.STROKE);
        targetAnglePaint.setStrokeJoin(Paint.Join.BEVEL);
        targetAnglePaint.setPathEffect(dashPathEffect);

        vmgPaint.setColor(a.getColor(R.styleable.PolarDrawableView_vmg_line_color, Color.GRAY));
        vmgPaint.setStrokeWidth(a.getFloat(R.styleable.PolarDrawableView_vmg_line_width, 6.f));
        vmgPaint.setStyle(Paint.Style.STROKE);
        vmgPaint.setStrokeJoin(Paint.Join.BEVEL);
        vmgPaint.setPathEffect(dashPathEffect);

        twaPaint.setColor(a.getColor(R.styleable.PolarDrawableView_twa_line_color, Color.GRAY));
        twaPaint.setStrokeWidth(a.getFloat(R.styleable.PolarDrawableView_twa_line_width, 6.f));
        twaPaint.setStyle(Paint.Style.STROKE);
        twaPaint.setStrokeJoin(Paint.Join.BEVEL);

        historyEntryRadius = (a.getFloat(R.styleable.PolarDrawableView_history_radius, 20.f));
        currentPointRadius = (a.getFloat(R.styleable.PolarDrawableView_current_point_radius, 20.f));

        historyPaint.setColor(a.getColor(R.styleable.PolarDrawableView_history_color, Color.GRAY));
        historyPaint.setStyle(Paint.Style.FILL_AND_STROKE);

        currentPointPaint.setColor(a.getColor(R.styleable.PolarDrawableView_current_point_color, Color.WHITE));
        currentPointPaint.setStyle(Paint.Style.FILL_AND_STROKE);

        markWidth = (a.getFloat(R.styleable.PolarDrawableView_mark_base_width, 80.f));
        markPaint.setStrokeWidth(a.getFloat(R.styleable.PolarDrawableView_mark_width, 6.f));
        markPaint.setColor(a.getColor(R.styleable.PolarDrawableView_mark_color, Color.RED));

        stbdTwaStatsPaint.setStrokeWidth(a.getFloat(R.styleable.PolarDrawableView_twa_stats_width, 16.f));
        stbdTwaStatsPaint.setColor(a.getColor(R.styleable.PolarDrawableView_twa_stats_color, Color.GREEN));
        stbdTwaStatsPaint.setStyle(Paint.Style.STROKE);

        portTwaStatsPaint.setStrokeWidth(a.getFloat(R.styleable.PolarDrawableView_twa_stats_width, 16.f));
        portTwaStatsPaint.setColor(a.getColor(R.styleable.PolarDrawableView_twa_stats_color, Color.RED));
        portTwaStatsPaint.setStyle(Paint.Style.STROKE);

        a.recycle();

        // Create the polar curve
        double stopAngle = 180;
        int n = (int) Math.round((stopAngle - startAngle)/angleStep) + 1;
        n *= 2;
        polarCurvePts = new float[n * 4];

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        viewHeight = MeasureSpec.getSize(heightMeasureSpec);
        //noinspection SuspiciousNameCombination
        viewWidth = viewHeight;  // Make view square

        x0 = viewWidth / 2.f;
        y0 = viewHeight / 2.f;

        setMeasuredDimension(viewWidth, viewHeight);
    }

    private enum ZoomLevel{
        FULL,
        UPWIND_STARBOARD,
        UPWIND_PORT,
        DOWNWIND_STARBOARD,
        DOWNWIND_PORT,
        REACH_STARBOARD,
        REACH_PORT,
    }

    @Override
    protected void onDraw(Canvas canvas) {

        // Starboard tack shown with negative X
        // Port tack shown with positive X

        float extraScale = 1.f;
        Paint.Align align = Paint.Align.LEFT;

        int targetSpeed;
        int textDirectionY = -1;
        int textDirectionX = 0;

        switch (zoomLevel){
            case UPWIND_PORT:
                x0 = 0;
                y0 = viewHeight;
                targetSpeed = Math.round(targetUpwindSpeed);
                break;
            case UPWIND_STARBOARD:
                x0 = viewWidth;
                align = Paint.Align.RIGHT;
                y0 = viewHeight;
                targetSpeed = Math.round(targetUpwindSpeed);
                break;
            case DOWNWIND_PORT:
                x0 = 0;
                y0 = 0;
                targetSpeed = Math.round(targetDownwindSpeed);
                textDirectionY = 1;
                break;
            case DOWNWIND_STARBOARD:
                x0 = viewWidth;
                align = Paint.Align.RIGHT;
                y0 = 0;
                targetSpeed = Math.round(targetDownwindSpeed);
                textDirectionY = 1;
                break;
            case REACH_PORT:
                x0 = 0;
                y0 = viewHeight / 2.f;
                targetSpeed = Math.round(targetDownwindSpeed);
                textDirectionX = 1;
                textDirectionY = 0;
                break;
            case REACH_STARBOARD:
                x0 = viewWidth;
                align = Paint.Align.RIGHT;
                y0 = viewHeight / 2.f;
                targetSpeed = Math.round(targetDownwindSpeed);
                textDirectionX = -1;
                textDirectionY = 0;
                break;
            default:
                x0 = viewWidth / 2.f;
                y0 = viewHeight / 2.f;
                targetSpeed = Math.round(targetDownwindSpeed);
                extraScale = 0.5f;
                break;
        }

        pixInKtsScale = (float) ( viewHeight / maxSpeed * extraScale );

        int speedScaleStep = 1;
        axisLabelPaint.setTextAlign(align);
        axisMajorLabelPaint.setTextAlign(align);


        for (int speed = (int) maxSpeed; speed > 3; speed -= speedScaleStep) {
            final boolean isTargetSpeed = speed == targetSpeed;
            final boolean isMinorAxis = (speed % 2) != 0;

            int color = isTargetSpeed ? axisTargetColor : axisColor;
            Paint axisPaint = isMinorAxis ? axisMinorPaint : axisMajorPaint;
            axisPaint.setColor(color);
            canvas.drawCircle(x0, y0, pixInKtsScale * speed, axisPaint);
            String text = String.format(Locale.getDefault(),"%d", speed);

            Paint textPaint = isTargetSpeed ?  axisMajorLabelPaint : axisLabelPaint;
            final float textX = x0 + textDirectionX *  (pixInKtsScale * speed );
            final float textY = y0 + textDirectionY *  (pixInKtsScale * speed ) + textPaint.getTextSize();
            canvas.drawText (text, textX, textY,  textPaint);
        }

        plotPoints(canvas, polarCurvePts, polarCurvePaint);

        if( !hasValidInput )
            return;

        // Draw history
        int alphaStep = 255 / HISTORY_LEN ;
        int alpha = 255 - alphaStep * (history.size() - 1);
        for ( int idx =0; idx < history.size()-1; idx++ ){
            HistoryEntry h = history.get(idx);
            historyPaint.setAlpha(alpha);
            alpha += alphaStep;
            canvas.drawCircle(toScreenX(h.x), toScreenY(h.y), historyEntryRadius, historyPaint);
        }

        // Target lines
        plotPointsAsPath(canvas, upwindTargetAnglePts, targetAnglePaint);
        plotPointsAsPath(canvas, downWindTargetAnglePts, targetAnglePaint);
        plotPointsAsPath(canvas, upwindVmgPts, vmgPaint);
        plotPointsAsPath(canvas, downWindVmgPts, vmgPaint);

        // Draw current point
        if ( !history.isEmpty() ) {
            HistoryEntry h = history.getLast();
            canvas.drawLine( toScreenX(0), toScreenY(0),
                    toScreenX(h.x), toScreenY(h.y),
                    twaPaint);
            canvas.drawCircle(toScreenX(h.x), toScreenY(h.y), currentPointRadius, currentPointPaint);
        }

        // Draw mark and it's mirror image
        if ( isMarkValid ) {
            // Mark itself
            drawTriangle((int)toScreenX(markPoint.x), (int)toScreenY(markPoint.y),
                    (int)markWidth,
                    markAngle,
                    markPaint, true, canvas);

            // Mirror image
            drawTriangle((int)toScreenX(- markPoint.x), (int)toScreenY(markPoint.y),
                    (int)markWidth,
                    - markAngle,
                    markPaint, false, canvas);
        }

        // Draw TWA stats
        twaStatsArgBounds.set(x0 - viewWidth * extraScale, y0 - viewHeight * extraScale,
                x0 + viewWidth * extraScale, y0 + viewHeight * extraScale);
        drawTwaStats(canvas, portIqr, medianPortTwa, portTwaStatsPaint);
        drawTwaStats(canvas, stbdIqr, medianStbdTwa, stbdTwaStatsPaint);

    }

    private void drawTwaStats(Canvas canvas, Angle twaIqr, Angle medianTwa, Paint twaStatsPaint) {
        if (twaIqr.isValid()) {
            final float startAngle = 270 + (float) (medianTwa.toDegrees() + twaIqr.toDegrees() / 2);
            final float sweepAngle = (float) twaIqr.toDegrees();
            canvas.drawArc(twaStatsArgBounds, startAngle, sweepAngle, false, twaStatsPaint);
        }
    }

    private void drawTriangle(int x, int y, int width, float angle, Paint paint, boolean isFilled, Canvas canvas){

        // Mark triangle
        Point p1 = new Point(x - width/2 ,y - width/2);
        Point p2 = new Point(x,y + width/2);
        Point p3 = new Point(x + width/2 ,y - width/2);

        Path path = new Path();
        path.setFillType(Path.FillType.EVEN_ODD);
        path.moveTo(p1.x,p1.y);
        path.lineTo(p2.x,p2.y);
        path.lineTo(p3.x,p3.y);
        path.close();

        // Rotate mark
        Matrix mMatrix = new Matrix();
        RectF bounds = new RectF();
        path.computeBounds(bounds, true);
        mMatrix.postRotate(angle, bounds.centerX(), bounds.centerY());
        path.transform(mMatrix);

        if ( isFilled ){
            markPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        }else{
            markPaint.setStyle(Paint.Style.STROKE);
        }
        canvas.drawPath(path, paint);
    }

    private void plotPoints(Canvas canvas, float[] pts, Paint paint) {
        float[] scaledPts = scalePoints(pts);
        canvas.drawLines(scaledPts, paint);
    }

    private void plotPointsAsPath(Canvas canvas, float[] pts, Paint paint) {
        float[] scaledPts = scalePoints(pts);
        Path path = new Path();
        path.moveTo(scaledPts[0], scaledPts[1]);
        for(int i = 2; i < scaledPts.length; i+=2) {
            path.lineTo(scaledPts[i], scaledPts[i+1]);
        }
        canvas.drawPath(path, paint);
    }

    @NonNull
    private float[] scalePoints(float[] pts) {
        float[] scaledPts = pts.clone();

        for(int i = 0; i < scaledPts.length; i++){
            scaledPts[i] *= pixInKtsScale;
            if ( i % 2 == 0 ) // x
                scaledPts[i] += x0;
            else
                scaledPts[i] += y0;
        }
        return scaledPts;
    }

    private PolarTable polarTable;
    public void onPolarTable(PolarTable polarTable){
        this.polarTable = polarTable;
    }

    public void onNavComputerOutput(final NavComputerOutput out){
        hasValidInput = false;

        if (polarTable == null) {
            this.invalidate();
            return;
        }

        final Speed tws = out.tws;

        if ( !tws.isValid() || !out.ii.sow.isValid()) {
            this.invalidate();
            return;
        }

        hasValidInput = true;

        if ( history.size() == HISTORY_LEN){
            history.removeFirst();
        }
        history.add(new HistoryEntry(out.ii.sow.getKnots(), out.twa.toDegrees()));

        maxSpeed = 0;
        for ( HistoryEntry h : history)
            maxSpeed = Math.max(h.speed, maxSpeed);

        // Polar curve
        createPolarCurve(tws);

        double angle;
        // Target lines

        Targets upwWindTargets = polarTable.getTargets(tws, PolarTable.PointOfSail.UPWIND);
        maxSpeed = Math.max(upwWindTargets.bsp.getKnots(), maxSpeed);
        targetUpwindSpeed = (float) upwWindTargets.bsp.getKnots();
        Targets downWindTargets = polarTable.getTargets(tws, PolarTable.PointOfSail.DOWNWIND);
        maxSpeed = Math.max(downWindTargets.bsp.getKnots(), maxSpeed);
        targetDownwindSpeed = (float) downWindTargets.bsp.getKnots();
        // Quantize max speed
        maxSpeed = Math.round(maxSpeed + 0.5);

        // Upwind target angle line
        angle = upwWindTargets.twa.toDegrees();
        upwindTargetAnglePts[0] = polarX(-angle, maxSpeed); // x
        upwindTargetAnglePts[1] = polarY(angle, maxSpeed); // y
        upwindTargetAnglePts[2] = 0; // x
        upwindTargetAnglePts[3] = 0; // y
        upwindTargetAnglePts[4] = 0; // x
        upwindTargetAnglePts[5] = 0; // y
        upwindTargetAnglePts[6] = polarX(angle, maxSpeed); // x
        upwindTargetAnglePts[7] = polarY(angle, maxSpeed); // y

        // Downwind target angle line
        angle = downWindTargets.twa.toDegrees();
        downWindTargetAnglePts[0] = polarX(-angle, maxSpeed); // x
        downWindTargetAnglePts[1] = polarY(angle, maxSpeed); // y
        downWindTargetAnglePts[2] = 0; // x
        downWindTargetAnglePts[3] = 0; // y
        downWindTargetAnglePts[4] = 0; // x
        downWindTargetAnglePts[5] = 0; // y
        downWindTargetAnglePts[6] = polarX(angle, maxSpeed); // x
        downWindTargetAnglePts[7] = polarY(angle, maxSpeed); // y

        // Upwind VMG line
        float vmg = (float) (upwWindTargets.bsp.getKnots() * Math.cos(upwWindTargets.twa.toRadians()));
        angle = Math.toDegrees(Math.acos(vmg / maxSpeed));
        upwindVmgPts[0] = polarX(-angle, maxSpeed); // x
        upwindVmgPts[1] = - vmg; // y
        upwindVmgPts[2] = polarX(angle, maxSpeed); // x
        upwindVmgPts[3] = polarY(angle, maxSpeed); // y

        // Downwind VMG line
        vmg = - (float) (downWindTargets.bsp.getKnots() * Math.cos(downWindTargets.twa.toRadians()));
        angle = Math.toDegrees(Math.acos(vmg / maxSpeed));
        downWindVmgPts[0] = polarX(-angle, maxSpeed); // x
        downWindVmgPts[1] = vmg; // y
        downWindVmgPts[2] = polarX(angle, maxSpeed); // x
        downWindVmgPts[3] = - polarY(angle, maxSpeed); // y

        isMarkValid = out.watm.isValid();
        if ( isMarkValid ) {
            Angle watm = new Angle(out.twa.toDegrees() - out.atm.toDegrees());

            markPoint.x = polarX( - watm.toDegrees(), maxSpeed);
            markPoint.y = polarY( - watm.toDegrees(), maxSpeed);
            markAngle = - (float) watm.toDegrees();
        }

        medianPortTwa = out.medianPortTwa;
        portIqr = out.portTwaIqr;
        medianStbdTwa = out.medianStbdTwa;
        stbdIqr = out.stbdTwaIqr;

        zoomLevel = determineZoomLevel();

        this.invalidate();
    }

    public void onTws(Speed tws) {
        createPolarCurve(tws);

        zoomLevel = determineZoomLevel();
        this.invalidate();
    }

    private void createPolarCurve(Speed tws) {
        double angle = startAngle;
        for(int i = 0; i < polarCurvePts.length/2 - 4; i+= 4) {
            // from
            Speed bs = polarTable.getTargetSpeed(tws, new Angle(angle));
            maxSpeed = Math.max(bs.getKnots(), maxSpeed);
            double r = bs.getKnots();

            polarCurvePts[i] = polarX(angle, r); // x
            polarCurvePts[i+1] = polarY(angle, r); // y

            // to
            angle += angleStep;
            bs = polarTable.getTargetSpeed(tws, new Angle(angle));
            maxSpeed = Math.max(bs.getKnots(), maxSpeed);
            r = bs.getKnots();
            polarCurvePts[i+2] = polarX(angle, r); // x
            polarCurvePts[i+3] = polarY(angle, r); // y
        }

        // Add symmetrical part
        int half = polarCurvePts.length/2;
        for(int i = 0; i < polarCurvePts.length/2 - 2; i+= 2) {
            // X goes with the same sign
            polarCurvePts[i + half] = - polarCurvePts[i];
            // Y is the same
            polarCurvePts[i + 1 + half] = polarCurvePts[i+1];
        }
    }

    private ZoomLevel determineZoomLevel() {
        if(history.isEmpty())
            return ZoomLevel.FULL;

        double minTwa = 400;
        double maxTwa = -400;
        double minAbsTwa = 400;
        double maxAbsTwa = 0;
        for( HistoryEntry h : history ) {
            minTwa = Math.min(h.angle, minTwa);
            maxTwa = Math.max(h.angle, maxTwa);
            minAbsTwa = Math.min(Math.abs(h.angle), minAbsTwa);
            maxAbsTwa = Math.max(Math.abs(h.angle), maxAbsTwa);
        }

        boolean reach = minAbsTwa >= 60 && maxAbsTwa <= 120;
        boolean upWind = !reach && (minAbsTwa <= 90 && maxAbsTwa <= 90);
        boolean downWind = !reach &&  (minAbsTwa > 90 && maxAbsTwa > 90);
        boolean port = minTwa < 0 && maxTwa < 0;
        boolean starboard = minTwa > 0 && maxTwa > 0;

        ZoomLevel zoomLevel;
        if( upWind && starboard )
            zoomLevel = ZoomLevel.UPWIND_STARBOARD;
        else if ( upWind && port )
            zoomLevel = ZoomLevel.UPWIND_PORT;
        else if ( downWind && starboard )
            zoomLevel = ZoomLevel.DOWNWIND_STARBOARD;
        else if ( downWind && port )
            zoomLevel = ZoomLevel.DOWNWIND_PORT;
        else if ( reach && starboard )
            zoomLevel = ZoomLevel.REACH_STARBOARD;
        else if ( reach && port )
            zoomLevel = ZoomLevel.REACH_PORT;
        else
            zoomLevel = ZoomLevel.FULL;

        Timber.d("Zoom %s upwind %s reach %s", zoomLevel, upWind, reach);

        return zoomLevel;
    }

    static float polarX(double angle, double r) {
        return (float) (r * Math.sin(Math.toRadians(angle)));
    }

    static float polarY(double angle, double r) {
        return (float) (r * (0 - Math.cos(Math.toRadians(angle))));
    }

    float toScreenX(float x){
        return x0 + x *pixInKtsScale;
    }

    float toScreenY(float y){
        return y0 + y * pixInKtsScale;
    }

}
