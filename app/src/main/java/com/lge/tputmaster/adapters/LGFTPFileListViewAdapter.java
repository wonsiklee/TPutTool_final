package com.lge.tputmaster.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.lge.tputmaster.R;
import org.apache.commons.net.ftp.FTPFile;

import java.util.ArrayList;
import java.util.HashMap;

import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * Created by wonsik.lee on 2018-01-08.
 */
@Accessors(prefix = "m")
public class LGFTPFileListViewAdapter extends BaseAdapter {
    private static String TAG = LGFTPFileListViewAdapter.class.getSimpleName();

    @Setter private ArrayList<FTPFile> mFileList;

    private Context mContext;

    public LGFTPFileListViewAdapter(Context context) {
        Log.d(TAG, "constructor : LGFTPFileListViewAdapter()");
        this.mContext = context;
    }

    @Override
    public int getCount() {
        if (this.mFileList != null) {
            return this.mFileList.size();
        } else {
            return 0;
        }
    }

    @Override
    public Object getItem(int position) {
        if (this.mFileList != null) {
            return this.mFileList.get(position);
        } else {
            return null;
        }
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder sHolder = null;

        if (convertView == null) {
            sHolder = new ViewHolder();

            LayoutInflater li = (LayoutInflater.from(this.mContext));
            convertView = li.inflate(R.layout.file_list_view_item, null);
            sHolder.mIcon = (ImageView) convertView.findViewById(R.id.imageView_file_icon);
            sHolder.mFileName = (TextView) convertView.findViewById(R.id.txtView_file_name);
            sHolder.mFileSize = (TextView) convertView.findViewById(R.id.txtView_file_size);
            sHolder.mIsChecked = (CheckBox) convertView.findViewById(R.id.checkbox_file_selected);

            convertView.setTag(sHolder);
        } else {
            sHolder = (ViewHolder) convertView.getTag();
        }

        Log.d(TAG, "checkbox visibility : " + mIsCheckboxShown);
        if (mIsCheckboxShown) {
            sHolder.mIsChecked.setVisibility(View.VISIBLE);
        } else {
            sHolder.mIsChecked.setVisibility(View.INVISIBLE);
        }

        FTPFile ff = this.mFileList.get(position);
        if (FTPFile.DIRECTORY_TYPE == ff.getType()) {
            sHolder.mIcon.setImageDrawable(this.mContext.getDrawable(R.drawable.ic_folder_close));
        } else {
            sHolder.mIcon.setImageDrawable(this.mContext.getDrawable(R.drawable.ic_file));
        }

        Log.d(TAG, "sHolder : " + sHolder + ", " + ff.toString());

        sHolder.mFileName.setText(ff.getName());
        sHolder.mFileSize.setText((ff.getSize() / 1024/ 1024) + " MB");

        Log.d(TAG, "getView() : completed");
        return convertView;
    }

    /*public ArrayList<FTPFile> getSelectedFileList() {
        ArrayList<FTPFile> sSelectedFileList = new ArrayList<>();

        return sSelectedFileList;
    }*/

    private boolean mIsCheckboxShown = false;
    public void enabledCheckBoxVisibility(boolean b) {
        this.mIsCheckboxShown = true;
        this.notifyDataSetChanged();
    }

    private class ViewHolder {
        public ImageView mIcon;
        public TextView mFileName;
        public TextView mFileSize;
        public CheckBox mIsChecked;
    }
}
