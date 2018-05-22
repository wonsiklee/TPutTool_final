package com.android.LGSetupWizard.adapters;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class TPutMonitorTestResultDBAdapter {
    private static final String TAG = TPutMonitorTestResultDBAdapter.class.getSimpleName();
    private static final String DB_NAME = "tput_test_log";
    private static final String DB_TABLE_NAME = "result_table";
    private static final String KEY_DATE_TIME = "date_time";
    private static final String KEY_ROW_ID = "_id";
    private static final String KEY_TEST_CATEGORY = "category";
    private static final String KEY_TEST_TYPE = "type";
    private static final String KEY_TEST_RESULT = "";

    private static TPutMonitorTestResultDBAdapter mInstance;

    private Context mContext;
    private TPutMonitorTestResultDBHelper mTPutMonitorTestResultDBHelper;
    private SQLiteDatabase mSqLiteDatabase;

    private TPutMonitorTestResultDBAdapter(Context context) {
        this.mContext = context;
        this.mTPutMonitorTestResultDBHelper = new TPutMonitorTestResultDBHelper(context);
        this.mSqLiteDatabase = mTPutMonitorTestResultDBHelper.getWritableDatabase();
    }

    static public TPutMonitorTestResultDBAdapter open(Context context) throws SQLException {
        if (mInstance == null) {
            mInstance = new TPutMonitorTestResultDBAdapter(context);
        }
        return mInstance;
    }

    static public void close() {
        if (mInstance != null) {
            if (mInstance.mSqLiteDatabase != null) {
                mInstance.mSqLiteDatabase.close();
            }
            if (mInstance.mTPutMonitorTestResultDBHelper != null) {
                mInstance.mTPutMonitorTestResultDBHelper.close();
            }
        }
    }

    private class TPutMonitorTestResultDBHelper extends SQLiteOpenHelper {
        public TPutMonitorTestResultDBHelper(Context context) {
            super(context, DB_NAME, null, 1);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            Log.d(TAG, "onCreate() ");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.d(TAG, "onUpgrade() " + oldVersion + " to " + newVersion);
        }

        @Override
        public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            //super.onDowngrade(db, oldVersion, newVersion);
            Log.d(TAG, "onDowngrade() " + oldVersion + " to " + newVersion);
        }
    }
}
