package com.lge.kobinfactory.lgiperf;

import android.content.Context;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.StateListDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.util.AttributeSet;
import android.widget.Switch;

/**
 * Created by hyukbin.ko on 2018-06-15.
 */

public class IperfSwitch extends Switch {
    private static final String TAG = IperfSwitch.class.getSimpleName();

    public IperfSwitch(Context context) {
        super(context); init();
    }

    public IperfSwitch(Context context, AttributeSet attrs) {
        super(context, attrs);init();
    }

    public IperfSwitch(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);init();
    }

    public IperfSwitch(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);init();
    }

    private StateListDrawable thumbDrawable;
    private ShapeDrawable checkedDrawable;
    private ShapeDrawable uncheckedDrawable;
    private ShapeDrawable trackDrawable;

    private void init(){

        checkedDrawable = new ShapeDrawable(new RoundRectShape(new float[]{10,10,10,10,10,10,10,10},null,null));
        checkedDrawable.getPaint().setColor(0xFFFF4040);

        uncheckedDrawable = new ShapeDrawable(new RoundRectShape(new float[]{10,10,10,10,10,10,10,10},null,null));
        uncheckedDrawable.getPaint().setColor(0xFF40FF40);

        trackDrawable = new ShapeDrawable(new RoundRectShape(new float[]{10,10,10,10,10,10,10,10},null,null));
        trackDrawable.getPaint().setColor(0xFF555555);

    }

    private void setThumbSize(int w, int h){
        thumbDrawable  = new StateListDrawable();

        checkedDrawable.setIntrinsicWidth((w-10)/2);
        checkedDrawable.setIntrinsicHeight(h-10);
        checkedDrawable.setPadding(6,6,6,6);
        thumbDrawable.addState(new int[]{android.R.attr.state_checked}, checkedDrawable);

        uncheckedDrawable.setIntrinsicWidth((w-10)/2);
        uncheckedDrawable.setIntrinsicHeight(h-10);
        uncheckedDrawable.setPadding(6,6,6,6);
        thumbDrawable.addState(new int[]{-android.R.attr.state_checked}, uncheckedDrawable);

        this.setThumbDrawable(thumbDrawable);
    }

    private void setTrack(int w, int h){
        trackDrawable.setIntrinsicWidth(w-10);
        trackDrawable.setIntrinsicHeight(h-10);
        trackDrawable.setPadding(6,6,6,6);
        this.setTrackDrawable(trackDrawable);
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec){

        int w = 0, h = 0;
        switch (MeasureSpec.getMode(widthMeasureSpec)) {
            case MeasureSpec.EXACTLY:
                w = MeasureSpec.getSize(widthMeasureSpec) ;
                setSwitchMinWidth(w/2+12);
                break;
        }

        switch (MeasureSpec.getMode(heightMeasureSpec)) {
            case MeasureSpec.EXACTLY:
                h= MeasureSpec.getSize(heightMeasureSpec) ;

                break;
        }

        if ( w != 0 && h != 0) {
            setThumbSize(w , h);
            setTrack(w , h);
        }
        super.onMeasure(widthMeasureSpec,heightMeasureSpec);

    }
}
