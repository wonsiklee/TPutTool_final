package com.android.LGSetupWizard.ui.popup;

import android.animation.Animator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.text.InputFilter;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupWindow;

import com.android.LGSetupWizard.R;
import com.android.LGSetupWizard.utils.NumberFormatFilter;

public class CounterSettingPopupWindow extends PopupWindow {
    private final String TAG = CounterSettingPopupWindow.class.getSimpleName();

    private EditText mParentView;
    private View mPopupWindowView;
    private Context mContext;

    private EditText mEditTextCounterValue;
    private ImageButton mImgBtnIncrease;
    private ImageButton mImgBtnDecrease;
    private Button mBtnDismissChange;
    private Button mBtnApplyChange;

    private boolean mIsLongClickMarked = false;

    private static final int MSG_INCREASE = 0x00;
    private static final int MSG_DECREASE = 0x01;
    private static final int MSG_STOP = 0x02;

    @SuppressLint("HandlerLeak")
    private Handler mRepeatIncreaseHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_INCREASE:
                    increase();
                    sendEmptyMessageDelayed(MSG_INCREASE, 30);
                    break;
                case MSG_DECREASE:
                    decrease();
                    sendEmptyMessageDelayed(MSG_DECREASE, 30);
                    break;
                case MSG_STOP:
                    this.removeMessages(MSG_DECREASE);
                    this.removeMessages(MSG_INCREASE);
                    break;
            }
        }
    };

    final static int MAX = 50000;
    final static int MIN = 1;

    @SuppressLint("ClickableViewAccessibility")
    public CounterSettingPopupWindow(Context context, EditText parentView) {
        this.mParentView = parentView;
        this.mContext = context;
        this.mPopupWindowView = ((LayoutInflater) this.mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.popup_view_count_input, null);

        this.mEditTextCounterValue = this.mPopupWindowView.findViewById(R.id.txtView_counter_value);
        this.mEditTextCounterValue.setText(mParentView.getText());
        this.mEditTextCounterValue.setFilters(new InputFilter[]{new NumberFormatFilter(this.mEditTextCounterValue, CounterSettingPopupWindow.MIN, CounterSettingPopupWindow.MAX)});
        this.mEditTextCounterValue.setSelection(0, mEditTextCounterValue.getText().length());

        this.mImgBtnIncrease = this.mPopupWindowView.findViewById(R.id.btn_increase);
        this.mImgBtnIncrease.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (CounterSettingPopupWindow.this.mIsLongClickMarked) {
                        CounterSettingPopupWindow.this.mRepeatIncreaseHandler.sendEmptyMessage(MSG_STOP);
                        CounterSettingPopupWindow.this.mIsLongClickMarked = false;
                    } else {
                        increase();
                    }
                }
                return false;
            }
        });
        this.mImgBtnIncrease.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                CounterSettingPopupWindow.this.mIsLongClickMarked = true;
                CounterSettingPopupWindow.this.mRepeatIncreaseHandler.sendEmptyMessage(MSG_INCREASE);
                return false;
            }
        });

        this.mImgBtnDecrease = this.mPopupWindowView.findViewById(R.id.btn_decrease);
        this.mImgBtnDecrease.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (CounterSettingPopupWindow.this.mIsLongClickMarked) {
                        CounterSettingPopupWindow.this.mRepeatIncreaseHandler.sendEmptyMessage(MSG_STOP);
                        CounterSettingPopupWindow.this.mIsLongClickMarked = false;
                    } else {
                        CounterSettingPopupWindow.this.decrease();
                    }
                }
                return false;
            }
        });
        this.mImgBtnDecrease.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                CounterSettingPopupWindow.this.mIsLongClickMarked = true;
                CounterSettingPopupWindow.this.mRepeatIncreaseHandler.sendEmptyMessage(MSG_DECREASE);
                return false;
            }
        });

        this.mBtnDismissChange = this.mPopupWindowView.findViewById(R.id.btn_discard_change);
        this.mBtnDismissChange.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CounterSettingPopupWindow.this.dismiss();
            }
        });

        this.mBtnApplyChange = this.mPopupWindowView.findViewById(R.id.btn_apply_change);
        this.mBtnApplyChange.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CounterSettingPopupWindow.this.mParentView.setText(mEditTextCounterValue.getText());
                CounterSettingPopupWindow.this.dismiss();
            }
        });

        this.setContentView(this.mPopupWindowView);
        DisplayMetrics sMetrics = new DisplayMetrics();
        (((WindowManager) this.mContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay()).getMetrics(sMetrics);

        this.setWidth((int)(sMetrics.widthPixels * 0.8f));
        this.setHeight((int) (this.getWidth() * 0.5f));

        LinearLayout sLinearLayout = this.mPopupWindowView.findViewById(R.id.ll_popup_view_count_input);
        Log.d(TAG, sLinearLayout.getMeasuredWidth() + ", " + sLinearLayout.getMeasuredHeight());
        sLinearLayout.setAlpha(0.0f);
        this.setFocusable(true);
    }

    @Override
    public void dismiss() {
        InputMethodManager sInputMethodManager = (InputMethodManager) this.mContext.getSystemService(Activity.INPUT_METHOD_SERVICE);
        sInputMethodManager.hideSoftInputFromWindow(this.mParentView.getWindowToken(), 0);
        super.dismiss();
    }

    public void show(int x, int y) {
        this.mPopupWindowView.setVisibility(View.VISIBLE);
        this.mPopupWindowView.setAlpha(0.0f);
        this.showAtLocation(this.mParentView, Gravity.NO_GRAVITY, x, y);
        this.mPopupWindowView.animate()
                .alpha(1.0f)
                .setDuration(300)
                .setListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        CounterSettingPopupWindow.this.mPopupWindowView.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) { }

                    @Override
                    public void onAnimationRepeat(Animator animation) { }
                });
    }

    private void increase() {
        int sCurrentValue = Integer.valueOf(mEditTextCounterValue.getText().toString());
        if (sCurrentValue < CounterSettingPopupWindow.MAX) {
            mEditTextCounterValue.setText(String.valueOf(sCurrentValue + 1));
        }
        mEditTextCounterValue.setSelection(0, mEditTextCounterValue.getText().length());
    }

    private void decrease() {
        int sCurrentValue = Integer.valueOf(mEditTextCounterValue.getText().toString());
        if (sCurrentValue > CounterSettingPopupWindow.MIN) {
            mEditTextCounterValue.setText(String.valueOf(sCurrentValue - 1));
        }
        mEditTextCounterValue.setSelection(0, mEditTextCounterValue.getText().length());
    }
}