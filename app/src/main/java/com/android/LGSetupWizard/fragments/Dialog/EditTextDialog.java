package com.android.LGSetupWizard.fragments.Dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.InputType;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.TextView;

import com.android.LGSetupWizard.R;

import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * Created by hyukbin.ko on 2018-05-21.
 */

@Accessors(prefix = "m")
public class EditTextDialog extends Dialog implements View.OnClickListener{
    @Setter private String mValue ;
    @Setter private String mTitle = "title";
    @Setter private String mDescription = "description";
    @Setter private OnSetDialogListener mOnSetDialogListener;

    private Button btn_set;
    private Button btn_cancel;
    private Button btn_delete;
    private EditText editText;

    @Setter private int mInputType = InputType.TYPE_CLASS_TEXT;

    public EditTextDialog(@NonNull Context context, String title, String description,
                              String defaultValue) {
        super(context);
        mTitle = title;
        mDescription = description;
        mValue = defaultValue;
    }

    @Override
    protected void onCreate(Bundle saveInstanceState){
        super.onCreate(saveInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_edit_text);

        TextView titleView = (TextView) findViewById(R.id.tv_title_dialog);
        titleView.setText(mTitle);

        TextView descriptionView = (TextView) findViewById(R.id.tv_description_dialog);
        descriptionView.setText(mDescription);


        editText= (EditText) findViewById(R.id.editText_dialog);
        editText.setText(mValue);
        editText.setInputType(mInputType);

        btn_set = (Button) findViewById(R.id.btn_set_dialog);
        btn_set.setOnClickListener(this);
        btn_cancel = (Button) findViewById(R.id.btn_cancel_dialog);
        btn_cancel.setOnClickListener(this);
        btn_delete = (Button) findViewById(R.id.btn_del_dialog);
        btn_delete.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if( v.getId() == btn_set.getId() ){
            if(mOnSetDialogListener!=null)mOnSetDialogListener.onSelect(editText.getText().toString());
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
