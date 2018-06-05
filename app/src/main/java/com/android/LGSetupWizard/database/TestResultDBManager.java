package com.android.LGSetupWizard.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.android.LGSetupWizard.data.TestResultDTO;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class TestResultDBManager {
    private static final String TAG = TestResultDBManager.class.getSimpleName();

    private static TestResultDBManager mInstance;

    private Context mContext;
    private TPutMonitorTestResultDBHelper mTPutMonitorTestResultDBHelper;


    public enum TestCategory { FTP_DL_WITH_FILE_IO, FTP_DL_WITHOUT_FILE_IO, // FTP
                               iPerf, // iPerf
                               HTTP_OK_WITH_FILE_IO, HTTP_OK_WITHOUT_FILE_IO, // okhttp
                               HTTP_APACHE_WITH_FILE_IO, HTTP_APACHE_WITHOUT_FILE_IO,
                               ALL_TYPE } // apache http

    public static TestResultDBManager getInstance(Context context) {
        Log.d(TAG, "getInstance()");
        if (mInstance == null) {
            Log.d(TAG, "mInstance is null");
            mInstance = new TestResultDBManager(context);
        }
        return mInstance;
    }

    private TestResultDBManager(Context context) {
        Log.d(TAG, "TestResultDBManager constructor");
        this.mContext = context;
        this.mTPutMonitorTestResultDBHelper = new TPutMonitorTestResultDBHelper(mContext);
    }

    public void debug_testQry_DB() {
        Log.d(TAG, "TestResultDBManager testQry()");
        //this.mTPutMonitorTestResultDBHelper.debug_testQry();
    }

    public void insert(TestCategory category, float testResult, @Nullable String description) {
        this.mTPutMonitorTestResultDBHelper.insert(category, testResult, description);
    }

    public ArrayList<TestResultDTO> fetch(@NonNull TestCategory category) {
        return this.mTPutMonitorTestResultDBHelper.fetch(category);
    }

    public void exportResults() {
        this.mTPutMonitorTestResultDBHelper.exportResults();
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

        private String[] formWhereValueArray(TestCategory category) {
            ArrayList<String> tmp = new ArrayList<>();
            String catString = category.toString();
            if (catString.contains("FTP")) {
                tmp.add(TestCategory.FTP_DL_WITH_FILE_IO.toString());
                tmp.add(TestCategory.FTP_DL_WITHOUT_FILE_IO.toString());
            } else if (catString.contains("iPerf")) {
                tmp.add(TestCategory.iPerf.toString());
            } else if (catString.contains("HTTP_OK")) {
                tmp.add(TestCategory.HTTP_OK_WITH_FILE_IO.toString());
                tmp.add(TestCategory.HTTP_OK_WITHOUT_FILE_IO.toString());
            } else if (catString.contains("HTTP_APACHE")) {
                tmp.add(TestCategory.HTTP_APACHE_WITH_FILE_IO.toString());
                tmp.add(TestCategory.HTTP_APACHE_WITHOUT_FILE_IO.toString());
            } else if (catString.contains("ALL")) {
                tmp.add(TestCategory.FTP_DL_WITH_FILE_IO.toString());
                tmp.add(TestCategory.FTP_DL_WITHOUT_FILE_IO.toString());
                tmp.add(TestCategory.iPerf.toString());
                tmp.add(TestCategory.HTTP_OK_WITH_FILE_IO.toString());
                tmp.add(TestCategory.HTTP_OK_WITHOUT_FILE_IO.toString());
                tmp.add(TestCategory.HTTP_APACHE_WITH_FILE_IO.toString());
                tmp.add(TestCategory.HTTP_APACHE_WITHOUT_FILE_IO.toString());
                tmp.add(TestCategory.ALL_TYPE.toString());
            }
            return tmp.toArray(new String[tmp.size()]);
        }

        private String formWhereClause(TestCategory category) {
            String catString = category.toString();
            StringBuilder sb = new StringBuilder();

            if (catString.contains("FTP")) {
                sb.append(KEY_TEST_CATEGORY).append(" = ? ").append(" OR ");
                sb.append(KEY_TEST_CATEGORY).append(" = ? ");
            } else if (catString.contains("iPerf")) {
                sb.append(KEY_TEST_CATEGORY).append(" = ? ");
            } else if (catString.contains("HTTP_OK")) {
                sb.append(KEY_TEST_CATEGORY).append(" = ? ").append(" OR ");
                sb.append(KEY_TEST_CATEGORY).append(" = ? ");
            } else if (catString.contains("HTTP_APACHE")) {
                sb.append(KEY_TEST_CATEGORY).append(" = ? ").append(" OR ");
                sb.append(KEY_TEST_CATEGORY).append(" = ? ");
            } else if (catString.contains("ALL")) {
                sb.append(KEY_TEST_CATEGORY).append(" = ? ").append(" OR ");
                sb.append(KEY_TEST_CATEGORY).append(" = ? ").append(" OR ");
                sb.append(KEY_TEST_CATEGORY).append(" = ? ").append(" OR ");
                sb.append(KEY_TEST_CATEGORY).append(" = ? ").append(" OR ");
                sb.append(KEY_TEST_CATEGORY).append(" = ? ").append(" OR ");
                sb.append(KEY_TEST_CATEGORY).append(" = ? ").append(" OR ");
                sb.append(KEY_TEST_CATEGORY).append(" = ? ").append(" OR ");
                sb.append(KEY_TEST_CATEGORY).append(" = ? ");
            }
            return sb.toString();
        }

        private ArrayList<TestResultDTO> fetch(TestCategory category) {
            ArrayList<TestResultDTO> sResultList = new ArrayList<>();
            SQLiteDatabase db = this.getReadableDatabase();

            Cursor c = db.query(TABLE_NAME,
                    new String[] {KEY_ROW_ID, KEY_DATE_TIME, KEY_TEST_CATEGORY, KEY_TEST_RESULT, KEY_DESCRIPTION},
                    formWhereClause(category), formWhereValueArray(category), null, null, null);
            if (c != null && c.getCount() > 0) {
                c.moveToFirst();
                do  {
                    try {
                        sResultList.add(new TestResultDTO(c.getInt(c.getColumnIndex(KEY_ROW_ID)),
                                mDateFormatter.parse(c.getString(c.getColumnIndex(KEY_DATE_TIME))),
                                c.getString(c.getColumnIndex(KEY_TEST_CATEGORY)),
                                c.getFloat(c.getColumnIndex(KEY_TEST_RESULT)),
                                c.getString(c.getColumnIndex(KEY_DESCRIPTION))));
                    } catch (ParseException e) {
                        e.printStackTrace();
                        Log.d(TAG, e.getMessage());
                    }
                } while (c.moveToNext());
            } else {
                Toast.makeText(mContext, "no data to show", Toast.LENGTH_SHORT).show();
            }
            db.close();
            return sResultList;
        }

        private void exportResults() {
            File sExportDir = new File(Environment.getExternalStorageDirectory(), "");
            if (!sExportDir.exists()) {
                sExportDir.mkdirs();
            }
            SimpleDateFormat sFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
            File sResultFile = new File(sExportDir, "Result_" + sFormat.format(new Date(System.currentTimeMillis())) + ".csv");
            try {
                Log.d(TAG, "making result file as " + sResultFile.getName() + " in " + sResultFile.getAbsoluteFile());
                sResultFile.createNewFile();
                sResultFile.setReadable(true, false);
                sResultFile.setWritable(true, false);
                FileWriter sFileWriter = new FileWriter(sResultFile);
                BufferedWriter sBufferedWriter = new BufferedWriter(sFileWriter);
                SQLiteDatabase db = this.getReadableDatabase();
                Cursor sCursor = db.query(TABLE_NAME,
                        new String[] {KEY_ROW_ID, KEY_DATE_TIME, KEY_TEST_CATEGORY, KEY_TEST_RESULT, KEY_DESCRIPTION},
                        formWhereClause(TestCategory.ALL_TYPE), formWhereValueArray(TestCategory.ALL_TYPE), null, null, null);
                int rowcount = sCursor.getCount();
                int colcount = sCursor.getColumnCount();
                if (rowcount > 0) {
                    for (int i = 0; i < colcount; i++) {
                        if (i != colcount - 1) {
                            sBufferedWriter.write(sCursor.getColumnName(i) + ",");
                        } else {
                            sBufferedWriter.write(sCursor.getColumnName(i));
                        }
                    }
                    sBufferedWriter.newLine();

                    sCursor.moveToFirst();
                    for (int i = 0; i < rowcount; i++) {

                        for (int j = 0; j < colcount; j++) {
                            if (j != colcount - 1)
                                sBufferedWriter.write(sCursor.getString(j) + ",");
                            else
                                sBufferedWriter.write(sCursor.getString(j));
                        }
                        sBufferedWriter.newLine();
                        sCursor.moveToNext();
                    }
                    sBufferedWriter.flush();
                }
                Toast.makeText(mContext, "Result file " + sResultFile.getName() + " is created.", Toast.LENGTH_SHORT).show();
            } catch(Exception sqlEx) {
                Log.e(TAG, "TPutMonitorTestResultDBHelper exportResults() got exception : " + sqlEx.getMessage());
            }
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
