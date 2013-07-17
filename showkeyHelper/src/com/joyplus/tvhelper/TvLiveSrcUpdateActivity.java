package com.joyplus.tvhelper;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.os.Bundle;
import android.widget.GridView;

import com.joyplus.tvhelper.adapter.TvLiveSrcUpdateAdapter;
import com.joyplus.tvhelper.entity.service.TvLiveView;
import com.joyplus.tvhelper.ui.GridSwitcherView;

public class TvLiveSrcUpdateActivity extends Activity {
	
	private TvLiveSrcUpdateAdapter adapter;
	private GridSwitcherView gridSwitcherView;
	private List<TvLiveView> list = new ArrayList<TvLiveView>();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.activity_tv_live_src_update);
		
		gridSwitcherView = (GridSwitcherView) findViewById(R.id.gridswitcherview);
		for(int i=0;i< 10;i++) {
			
			TvLiveView info = new TvLiveView();
			list.add(info);
		}
		
		gridSwitcherView.setRows(GridSwitcherView.ROW_1);
		gridSwitcherView.initGridSwitcherView(list.size(), GridSwitcherView.NUM_CLOUMNS_4);
		
		adapter = new TvLiveSrcUpdateAdapter(this,gridSwitcherView.getNumCloumns(),gridSwitcherView,list);

		gridSwitcherView.setGridAdapter(adapter);
		
	}

}
