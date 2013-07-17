package com.joyplus.tvhelper.adapter;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;

import com.joyplus.tvhelper.R;
import com.joyplus.tvhelper.entity.service.TvLiveView;
import com.joyplus.tvhelper.ui.GridSwitcherView;

public class TvLiveSrcUpdateAdapter extends BaseAdapter {
	
	private Context mContext;
	private int mNumClonumns;
	private GridSwitcherView mGridSwitcherView;
	private List<TvLiveView> mList;
	
	public TvLiveSrcUpdateAdapter(Context context,int numClonumns,GridSwitcherView gridSwitcherView,List<TvLiveView> list) {
		// TODO Auto-generated constructor stub
		this.mContext = context;
		this.mNumClonumns = numClonumns;
		this.mGridSwitcherView = gridSwitcherView;
		this.mList = list;
	}

	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		int currentIndex = mGridSwitcherView.getCurrentItem() + 1;
		int listSize = mList.size();
		
		if(listSize < mGridSwitcherView.getRows() * mNumClonumns) {
			
			return listSize;
		}
		
		if(listSize >= currentIndex * mGridSwitcherView.getRows() * mNumClonumns) {
			
			return mGridSwitcherView.getRows() * mNumClonumns;
		} else {
			
			return listSize - (currentIndex - 1) * mGridSwitcherView.getRows() * mNumClonumns;
		}
		
	}

	@Override
	public Object getItem(int position) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// TODO Auto-generated method stub
		
		if(convertView == null) {
			
			LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = inflater.inflate(R.layout.item_tv_src_update_grid, null);
		}
		
		AbsListView.LayoutParams layoutParams = new AbsListView.LayoutParams(187, 187);
		convertView.setLayoutParams(layoutParams);
		return convertView;
	}

}
