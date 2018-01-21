package com.lge.tputmaster.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.lge.tputmaster.R;
import com.lge.tputmaster.clients.LGFTPItemView;

import org.apache.commons.net.ftp.FTPFile;

import java.util.ArrayList;

import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * Created by wonsik.lee on 2018-01-08.
 */
@Accessors(prefix = "m")
public class FileListViewAdapter extends BaseAdapter {

    @Setter private ArrayList<FTPFile> mFileList;

    public FileListViewAdapter() {
        this.mFileList = new ArrayList<>();
    }

    @Override
    public int getCount() {
        return this.mFileList.size();
    }

    @Override
    public Object getItem(int position) {
        return this.mFileList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public LGFTPItemView getView(int position, View convertView, ViewGroup parent) {
        final int sPos = position;
        final Context sContext = parent.getContext();

        if (convertView == null) {
            LayoutInflater li = (LayoutInflater.from(sContext));
            convertView = li.inflate(R.layout.file_list_view_item, parent, false);
        }

        ImageView sImgViewIcon = convertView.findViewById(R.id.imageView_file_icon);
        TextView sTextViewFileName = convertView.findViewById(R.id.txtView_file_name);
        TextView sTextViewFileSize = convertView.findViewById(R.id.txtView_file_name);

        sImgViewIcon.setImageDrawable(sContext.getDrawable(R.drawable.record_start));
        sTextViewFileName.setText("position");
        sTextViewFileSize.setText(position * 100);

        return (LGFTPItemView) convertView;
    }
}
