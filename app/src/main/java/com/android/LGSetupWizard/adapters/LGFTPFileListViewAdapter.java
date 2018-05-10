package com.android.LGSetupWizard.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.LGSetupWizard.data.LGFTPFile;
import com.android.LGSetupWizard.R;

import org.apache.commons.net.ftp.FTPFile;

import java.io.File;
import java.util.ArrayList;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * Created by wonsik.lee on 2018-01-08.
 */
@Accessors(prefix = "m")
public class LGFTPFileListViewAdapter extends BaseAdapter {
    private static String TAG = LGFTPFileListViewAdapter.class.getSimpleName();

    @Setter @Getter private ArrayList<LGFTPFile> mFileList;

    @Setter @Getter
    private ArrayList<Integer> mSelectedFilePositionList;

    private Context mContext;

    public LGFTPFileListViewAdapter(Context context) {
        Log.d(TAG, "constructor : LGFTPFileListViewAdapter()");
        this.mContext = context;
        this.mSelectedFilePositionList = new ArrayList<>();
    }

    public void toggleFileSelectedStatusAt(int position) {
        if (this.isFileSelectedAt(position)) {
            int targetIndex = 0;

            for (int i = 0; i != this.mSelectedFilePositionList.size(); ++i) {
                if (this.mSelectedFilePositionList.get(i) == position) {
                    targetIndex = i;
                }
            }

            this.mSelectedFilePositionList.remove(targetIndex);
        } else {
            this.mSelectedFilePositionList.add(position);
        }
        notifyDataSetChanged();
    }

    public void clearSelectedFilePositionList() {
        this.mSelectedFilePositionList.clear();
    }

    public ArrayList<LGFTPFile> getSelectedFileList() {
        ArrayList<LGFTPFile> sSelectedFileList = new ArrayList<>();
        for (Integer position: this.mSelectedFilePositionList) {
            sSelectedFileList.add(this.mFileList.get(position));
        }
        return sSelectedFileList;
    }

    private boolean isFileSelectedAt(int position) {
        return this.mSelectedFilePositionList.contains(position);
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

    @SuppressLint("ResourceAsColor")
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
            sHolder.mIsSelected = (CheckBox) convertView.findViewById(R.id.checkbox_file_selected);

            convertView.setTag(sHolder);
        } else {
            sHolder = (ViewHolder) convertView.getTag();
        }

        sHolder.mIsSelected.setVisibility(View.INVISIBLE);

        FTPFile ff = this.mFileList.get(position);
        if (FTPFile.DIRECTORY_TYPE == ff.getType()) {
            sHolder.mIcon.setImageDrawable(this.mContext.getDrawable(R.drawable.ic_folder_close));
        } else {
            sHolder.mIcon.setImageDrawable(this.mContext.getDrawable(R.drawable.ic_file));
        }

        sHolder.mFileName.setText(ff.getName());
        sHolder.mFileSize.setText((ff.getSize() / 1024/ 1024) + " MB");
        if (this.isFileSelectedAt(position)) {
            convertView.setBackgroundColor(R.color.selected_color);
        } else {
            convertView.setBackgroundColor(Color.WHITE);
        }

        return convertView;
    }

    public int getSelectedFileCount() {
        return this.mSelectedFilePositionList.size();
    }

    public boolean isSelectedFileListEmpty() {
        return (this.mSelectedFilePositionList.size() == 0) ? true : false;
    }

    private class ViewHolder {
        public ImageView mIcon;
        public TextView mFileName;
        public TextView mFileSize;
        public CheckBox mIsSelected;
    }
}
