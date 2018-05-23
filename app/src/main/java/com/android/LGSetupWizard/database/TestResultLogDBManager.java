package com.android.LGSetupWizard.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class TestResultLogDBManager {
    private static final String TAG = TestResultLogDBManager.class.getSimpleName();


    private static TestResultLogDBManager mInstance;

    private Context mContext;
    private TPutMonitorTestResultDBHelper mTPutMonitorTestResultDBHelper;


    public static TestResultLogDBManager getInstance(Context context) {
        Log.d(TAG, "getInstance()");
        if (mInstance == null) {
            Log.d(TAG, "mInstance is null");
            mInstance = new TestResultLogDBManager(context);
        }
        return mInstance;
    }

    private TestResultLogDBManager(Context context) {
        Log.d(TAG, "TestResultLogDBManager constructor");
        this.mContext = context;
        this.mTPutMonitorTestResultDBHelper = new TPutMonitorTestResultDBHelper(mContext);
    }

    public void testQry() {
        Log.d(TAG, "TestResultLogDBManager testQry()");
        this.mTPutMonitorTestResultDBHelper.testQry();
    }

    private class TPutMonitorTestResultDBHelper extends SQLiteOpenHelper {

        private static final String DB_NAME = "tput_test_log";
        private static final String TABLE_NAME = "result_table";
        private static final String KEY_ROW_ID = "_id";
        private static final String KEY_DATE_TIME = "date_time";
        private static final String KEY_TEST_CATEGORY = "category";
        private static final String KEY_TEST_TYPE = "type";
        private static final String KEY_TEST_RESULT = "";

        public TPutMonitorTestResultDBHelper(Context context) {
            super(context, DB_NAME, null, 2);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            Log.d(TAG, "onCreate() enter");
            db.execSQL("create table " + TABLE_NAME + " (" +
                    KEY_ROW_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    KEY_DATE_TIME  + " TEXT, " +
                    KEY_TEST_CATEGORY + " TEXT, " +
                    KEY_TEST_TYPE + " TEXT, " +
                    KEY_TEST_RESULT + " TEXT );");

            Log.d(TAG, "onCreate() exit");
            Log.d(TAG, "table creation completed");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.d(TAG, "onUpgrade() " + oldVersion + " to " + newVersion);
            db.execSQL("drop table " + TABLE_NAME);
            onCreate(db);
        }

        @Override
        public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            //super.onDowngrade(db, oldVersion, newVersion);
            Log.d(TAG, "onDowngrade() " + oldVersion + " to " + newVersion);
            db.execSQL("drop table " + TABLE_NAME);
            onCreate(db);
        }

        public void testQry() {
            Log.d(TAG, "TPutMonitorTestResultDBHelper testQry()");
            ContentValues cv = new ContentValues();
            cv.put(KEY_DATE_TIME, "1111");
            SQLiteDatabase db = this.getWritableDatabase();

            db.insert(TABLE_NAME, null, cv);

            db.close();

            db = this.getReadableDatabase();

            Cursor c = db.query(TABLE_NAME, new String[] {KEY_ROW_ID, KEY_DATE_TIME}, null, null, null, null, null);

            if (c != null && c.getCount() > 0) {
                c.moveToFirst();
            }

            Log.d(TAG, "record count = " + c.getCount());
            Log.d(TAG, "cursor position = " + c.getPosition());
            do  {
                Log.d(TAG, "cursor position = " + c.getPosition());
                int index = c.getInt(c.getColumnIndex(KEY_ROW_ID));
                String dateTime = c.getString(c.getColumnIndex(KEY_DATE_TIME));
                Log.d(TAG, "index = " + index + ", dateTime = " + dateTime);
            } while (c.moveToNext());

        }
    }
}
