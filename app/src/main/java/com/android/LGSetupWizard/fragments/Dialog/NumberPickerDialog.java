package com.android.LGSetupWizard.fragments.Dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.TextView;

import com.android.LGSetupWizard.R;

import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * Created by hyukbin.ko on 2018-05-21.
 */

@Accessors(prefix = "m")
public class NumberPickerDialog extends Dialog implements View.OnClickListener {

    @Setter private int mSelectedValue = 0;
    @Setter private int mMaxValue = Integer.MAX_VALUE;
    @Setter private int mMinValue = 0;
    @Setter private String mTitle = "title";
    @Setter private String mDescription = "description";
    @Setter private OnSetDialogListener mOnSetDialogListener;

    private Button btn_set;
    private Button btn_cancel;
    private Button btn_delete;
    private NumberPicker mNumberPicker;

    public NumberPickerDialog(@NonNull Context context, String title, String description,
                              int minValue, int maxValue, int defaultValue) {
        super(context);
        mTitle = title;
        mDescription = description;
        mMaxValue = maxValue;
        mMinValue = minValue;
        mSelectedValue = ((defaultValue < minValue || defaultValue > maxValue))? minValue : defaultValue;
    }

    @Override
    protected void onCreate(Bundle saveInstanceState){
        super.onCreate(saveInstanceState);
        //getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_number_picker);


        TextView titleView = (TextView) findViewById(R.id.tv_title_numbePicker_dialog);
        titleView.setText(mTitle);

        TextView descriptionView = (TextView) findViewById(R.id.tv_description_numberPicker_dialog);
        descriptionView.setText(mDescription);


        mNumberPicker= (NumberPicker) findViewById(R.id.numberPicker);
        mNumberPicker.setWrapSelectorWheel(false);
        mNumberPicker.setMinValue(mMinValue);
        mNumberPicker.setMaxValue(mMaxValue);
        mNumberPicker.setValue(mSelectedValue);

        btn_set = (Button) findViewById(R.id.btn_set_numberPicker_dialog);
        btn_set.setOnClickListener(this);
        btn_cancel = (Button) findViewById(R.id.btn_cancel_numberPicker_dialog);
        btn_cancel.setOnClickListener(this);
        btn_delete = (Button) findViewById(R.id.btn_del_numberPicker_dialog);
        btn_delete.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if( v.getId() == btn_set.getId() ){
            if(mOnSetDialogListener!=null)mOnSetDialogListener.onSelect(mNumberPicker.getValue());
        }
        if( v.getId() == btn_cancel.getId() ){
            if(mOnSetDialogListener!=null)mOnSetDialogListener.onCancel();
        }
        if( v.getId() == btn_delete.getId() ){
            if(mOnSetDialogListener!=null)mOnSetDialogListener.onDelete();
        }
        dismiss();
    }

}
