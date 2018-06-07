package com.lge.kobinfactory.lgiperf.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.InputType;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.lge.kobinfactory.lgiperf.R;

import java.util.List;

import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * Created by hyukbin.ko on 2018-05-28.
 */


@Accessors(prefix="m")
public class EditTextUnitDialog extends Dialog implements View.OnClickListener{
    private Context mContext;
    @Setter private String mDefaultValue;
    @Setter private int mDefaultUnitPosition;

    private List<String> mUnitList;

    @Setter private String mTitle = "title";
    @Setter private String mDescription = "description";
    @Setter private OnSetDialogListener mOnSetDialogListener;

    private Button btn_set;
    private Button btn_cancel;
    private Button btn_delete;
    private EditText editText;
    private Spinner spinner_dialog;

    @Setter private int mInputType = InputType.TYPE_CLASS_TEXT;

    public EditTextUnitDialog(@NonNull Context context, String title, String description,
                              String defaultValue, int defaultUnitPosition, List<String> unitList) {
        super(context);
        mContext = context;
        mTitle = title;
        mDescription = description;
        mDefaultValue = defaultValue;
        mDefaultUnitPosition = defaultUnitPosition;
        mUnitList = unitList;
    }

    @Override
    protected void onCreate(Bundle saveInstanceState){
        super.onCreate(saveInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_edit_text_unit);

        TextView titleView = (TextView) findViewById(R.id.tv_title_dialog);
        titleView.setText(mTitle);

        TextView descriptionView = (TextView) findViewById(R.id.tv_description_dialog);
        descriptionView.setText(mDescription);


        editText= (EditText) findViewById(R.id.editText_dialog);
        editText.setText((mDefaultValue !=null)? mDefaultValue : "");
        editText.setInputType(mInputType);

        spinner_dialog = (Spinner) findViewById(R.id.spinner_dialog);
        ArrayAdapter<String> spinner_Adpator = new ArrayAdapter<String>(mContext,android.R.layout.simple_spinner_item, mUnitList);
        spinner_Adpator.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner_dialog.setAdapter(spinner_Adpator);
        spinner_dialog.setSelection(mDefaultUnitPosition);

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
            if(mOnSetDialogListener!=null)mOnSetDialogListener.onSelect(this, editText.getText().toString(), spinner_dialog.getSelectedItemPosition());
        }
        if( v.getId() == btn_cancel.getId() ){
            if(mOnSetDialogListener!=null)mOnSetDialogListener.onCancel(this);
        }
        if( v.getId() == btn_delete.getId() ){
            if(mOnSetDialogListener!=null)mOnSetDialogListener.onDelete(this);
        }
        dismiss();
    }


}
