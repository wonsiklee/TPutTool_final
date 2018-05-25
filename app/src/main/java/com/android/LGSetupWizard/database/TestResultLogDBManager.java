package com.android.LGSetupWizard.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class TestResultLogDBManager {
    private static final String TAG = TestResultLogDBManager.class.getSimpleName();

    private static TestResultLogDBManager mInstance;

    private Context mContext;
    private TPutMonitorTestResultDBHelper mTPutMonitorTestResultDBHelper;


    public enum TestCategory { FTP_DL_WITH_FILE_IO, FTP_DL_WITHOUT_FILE_IO, // FTP
                               iPerf, // iPerf
                               HTTP_OK_WITH_FILE_IO, HTTP_OK_WITHOUT_FILE_IO, // okhttp
                               HTTP_APACHE_WITH_FILE_IO, HTTP_APACHE_WITHOUT_FILE_IO } // apache http

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

    public void debug_testQry_DB() {
        Log.d(TAG, "TestResultLogDBManager testQry()");
        this.mTPutMonitorTestResultDBHelper.debug_testQry();
    }

    public void insert(TestCategory category, float testResult, @Nullable String description) {
        this.mTPutMonitorTestResultDBHelper.insert(category, testResult, description);
    }

    private class TPutMonitorTestResultDBHelper extends SQLiteOpenHelper {

        private static final String DB_NAME = "tput_test_log";
        private static final String TABLE_NAME = "test_result_log";
        private static final String KEY_ROW_ID = "_id";
        private static final String KEY_DATE_TIME = "date_time";
        private static final String KEY_TEST_CATEGORY = "category";
        private static final String KEY_TEST_RESULT = "result";
        private static final String KEY_DESCRIPTION = "description";

        private final SimpleDateFormat mDateFormatter = new SimpleDateFormat("MM-dd HH:mm:ss");

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
                    KEY_TEST_RESULT + " TEXT," +
                    KEY_DESCRIPTION + " TEXT);");

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
            Log.d(TAG, "onDowngrade() " + oldVersion + " to " + newVersion);
            db.execSQL("drop table " + TABLE_NAME);
            onCreate(db);
        }

        private void insert(TestCategory category, float testResult, @Nullable String description) {
            Log.d(TAG, "insert() " + category.toString() + ", " + String.format("%.2f", testResult) + ", " + description);
            ContentValues cv = new ContentValues();
            cv.put(KEY_DATE_TIME, this.mDateFormatter.format(Calendar.getInstance().getTime()));
            cv.put(KEY_TEST_CATEGORY, category.toString());
            cv.put(KEY_TEST_RESULT, testResult);
            cv.put(KEY_DESCRIPTION, description == null ? "null" : description);

            SQLiteDatabase db = this.getWritableDatabase();
            db.insert(TABLE_NAME, null, cv);
            db.close();
        }

        private void debug_testQry() {
            Log.d(TAG, "TPutMonitorTestResultDBHelper testQry()");

            SQLiteDatabase db = this.getReadableDatabase();
            Cursor c = db.query(TABLE_NAME, new String[] {KEY_ROW_ID, KEY_DATE_TIME, KEY_TEST_CATEGORY, KEY_TEST_RESULT, KEY_DESCRIPTION}, null, null, null, null, null);
            if (c != null && c.getCount() > 0) {
                c.moveToFirst();
                Log.d(TAG, "record count = " + c.getCount());
                do  {
                    Log.d(TAG, debug_parseCursorToString(c));
                } while (c.moveToNext());
            } else {
                Toast.makeText(mContext, "no data to show", Toast.LENGTH_SHORT).show();
            }
            db.close();
        }

        private String debug_parseCursorToString(Cursor cursor) {
            Log.d(TAG, "debug_parseCursorToString");
            StringBuilder sb = new StringBuilder();
            sb.append("position : ").append(cursor.getPosition()).append("\n")
                    .append("row_id : ").append(cursor.getInt(cursor.getColumnIndex(KEY_ROW_ID))).append("\n")
                    .append("date : ").append(cursor.getString(cursor.getColumnIndex(KEY_DATE_TIME))).append("\n")
                    .append("category : ").append(cursor.getString(cursor.getColumnIndex(KEY_TEST_CATEGORY))).append("\n")
                    .append("result : ").append(cursor.getString(cursor.getColumnIndex(KEY_TEST_RESULT))).append(" Mbps\n")
                    .append("description : ").append(cursor.getString(cursor.getColumnIndex(KEY_DESCRIPTION)));
            return sb.toString();
        }
    }
}
