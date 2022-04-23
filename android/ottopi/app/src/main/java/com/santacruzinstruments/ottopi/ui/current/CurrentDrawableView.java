package com.santacruzinstruments.ottopi.ui.current;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import com.santacruzinstruments.ottopi.R;
import com.santacruzinstruments.ottopi.navengine.geo.Angle;
import com.santacruzinstruments.ottopi.navengine.geo.Speed;

import java.util.ArrayList;
import java.util.List;

public class CurrentDrawableView  extends View {

    private static final int ARROW_HEIGHT = 120;
    private static final int ARROW_TOP = 0;
    private static final int ARROW_X_OFFS = 0;
    private int viewHeight;
    private int viewWidth;
    Paint arrowPaint = new Paint(0);
    Angle currentAngle = new Angle();

    public CurrentDrawableView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.getTheme().obtainStyledAttributes(attrs,
                R.styleable.CurrentDrawableView, 0, 0);

        arrowPaint.setStyle(Paint.Style.STROKE);
        arrowPaint.setColor(a.getColor(R.styleable.CurrentDrawableView_arrow_color, Color.BLACK));
        arrowPaint.setStrokeWidth(a.getFloat(R.styleable.CurrentDrawableView_arrow_width, 6.f));

        a.recycle();

        setCurrent(new Speed(10), new Angle(0));
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        viewWidth = MeasureSpec.getSize(widthMeasureSpec);
        viewHeight = MeasureSpec.getSize(heightMeasureSpec);

        setMeasuredDimension(viewWidth, viewHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float px = viewWidth / 2.f;
        float py = viewHeight / 2.f;

        if ( currentAngle.isValid() && currentArrow.length > 4){
            float scale = Math.min(viewHeight, viewWidth) / (float)ARROW_HEIGHT;
            final float dx = viewWidth / 2.f;
            final float dy = ( viewHeight - ARROW_HEIGHT) / 2.f;
            canvas.scale(scale, scale, px, py);
            canvas.rotate((float) currentAngle.toDegrees(), px, py);
            canvas.translate(dx, dy);
            canvas.drawLines(currentArrow, arrowPaint);
        }else{
            canvas.drawCircle(px, py, viewHeight / 3.f, arrowPaint);
        }
    }

    private float [] currentArrow;
    public void setCurrent(Speed speed, Angle angle){
        currentAngle = angle;

        if ( speed.isValid() && angle.isValid() ){
            ArrayList<Float> pointsList = new ArrayList<>();

            double soc = speed.getKnots();

            float x_top = ARROW_X_OFFS;
            float y_top = ARROW_TOP;
            float y_bot = y_top + ARROW_HEIGHT;

            float d = 20;
            float dy = 0;

            // Draw the trunk
            pointsList.add(x_top); // x
            pointsList.add(y_top); // y
            pointsList.add(x_top); // x
            pointsList.add(y_bot); // y

            if ( soc  >= 0.5 ) {
                makeArrow(true, d, dy, pointsList, x_top, y_top);
            }
            if ( soc  >= 1 ) {
                makeArrow(false, d, dy, pointsList, x_top, y_top);
            }

            dy = 20;
            d = 30;

            if ( soc  >= 1.5 ) {
                makeArrow(true, d, dy, pointsList, x_top, y_top);
            }
            if ( soc  >= 2 ) {
                makeArrow(false, d, dy, pointsList, x_top, y_top);
            }

            dy = 40;
            d = 40;

            if ( soc  >= 2.5 ) {
                makeArrow(true, d, dy, pointsList, x_top, y_top);
            }
            if ( soc  >= 3 ) {
                makeArrow(false, d, dy, pointsList, x_top, y_top);
            }

            dy = 60;
            d = 50;

            if ( soc  >= 3.5 ) {
                makeArrow(true, d, dy, pointsList, x_top, y_top);
            }
            if ( soc  >= 4 ) {
                makeArrow(false, d, dy, pointsList, x_top, y_top);
            }

            // Convert to array to be drawn
            currentArrow = new float[pointsList.size()];
            for( int i = 0; i < currentArrow.length; i++)
                currentArrow[i] = pointsList.get(i);
        }

        this.invalidate();
    }

    private void makeArrow(boolean right, float d, float dy, List<Float> pointsList, float x0, float y0) {
        pointsList.add(x0); // x
        pointsList.add(y0 + dy);   // y
        if ( right )
            pointsList.add(x0 + d); // x
        else
            pointsList.add(x0 - d); // x
        pointsList.add(y0 + dy + d);  // y
    }

}
