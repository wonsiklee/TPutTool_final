package com.android.LGSetupWizard.adapters;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.android.LGSetupWizard.R;

import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * Created by hyukbin.ko on 2018-05-17.
 */
@Accessors(prefix = "m")
public class LGIperfMenuListAdapter extends BaseAdapter{

    private Context mContext;
    @Setter @Getter private int mCurrentSelectedPosition = -1;
    @Getter private List<String> mCommandList;
    private static String DEFAULT_COMMAND="iperf";

    public void setCommandList(List<String> commandList){
        mCommandList = commandList;
        if (mCommandList==null || mCommandList.size()==0 ){
            mCurrentSelectedPosition = -1;
        }
    }
    public boolean deleteCommand(int position){
        if ( position <0 ||  position > mCommandList.size() ) return false;
        mCommandList.remove(position);
        return true;
    }
    public void addCommand(){
        mCommandList.add(0, DEFAULT_COMMAND);
    }

    public void modifyCommand(String command){
        if(mCurrentSelectedPosition<0) return;
        mCommandList.remove(mCurrentSelectedPosition);
        mCommandList.add(mCurrentSelectedPosition,command);
    }

    public LGIperfMenuListAdapter(@NonNull Context context) {
        mContext = context;
    }

    @Override
    public int getCount() {
        if(mCommandList==null) return 0;
        return mCommandList.size();
    }

    @Override
    public Object getItem(int position) {
        if(position < 0) return null;
        if(mCommandList==null) return null;


        return mCommandList.get(position);
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
            convertView = li.inflate(R.layout.iperf_command_list_item, null);

            sHolder.textview_iperf_menu_item = (TextView) convertView.findViewById(R.id.textview_iperf_menu_item);
            convertView.setTag(sHolder);
        } else {
            sHolder = (ViewHolder) convertView.getTag();
        }

        if(mCurrentSelectedPosition == position) {
            convertView.setBackgroundColor(mContext.getColor(R.color.selected_color));
        }
        else {
            convertView.setBackgroundColor(Color.WHITE);
        }

        sHolder.textview_iperf_menu_item.setText(mCommandList.get(position));

        return convertView;
    }
    class ViewHolder {
        TextView textview_iperf_menu_item;
    }
}
