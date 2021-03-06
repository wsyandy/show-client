package com.joyplus.tvhelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.TextView;

import com.androidquery.AQuery;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.joyplus.tvhelper.adapter.AppRecommendAdapter;
import com.joyplus.tvhelper.entity.ApkDownloadInfoParcel;
import com.joyplus.tvhelper.entity.ApkInfo;
import com.joyplus.tvhelper.entity.AppRecommendInfo;
import com.joyplus.tvhelper.entity.PushedApkDownLoadInfo;
import com.joyplus.tvhelper.entity.service.AppRecommendView;
import com.joyplus.tvhelper.faye.FayeService;
import com.joyplus.tvhelper.ui.WaitingDialog;
import com.joyplus.tvhelper.utils.Constant;
import com.joyplus.tvhelper.utils.Global;
import com.joyplus.tvhelper.utils.Log;
import com.joyplus.tvhelper.utils.PackageUtils;
import com.joyplus.tvhelper.utils.Utils;
import com.umeng.analytics.MobclickAgent;

public class AppRecommendActivity extends Activity {
	
	public static final String TAG = "AppRecommendActivity";
	
	private static final int DIALOG_WAITING = 0;
	
//	private List<AppRecommendInfo> list = new ArrayList<AppRecommendInfo>();
	
//	private int[] egAppIds = {R.drawable.app_bg_1,R.drawable.app_bg_2,R.drawable.app_bg_3,
//							   R.drawable.app_bg_4,R.drawable.app_bg_5,R.drawable.app_bg_6,
//			                   R.drawable.app_bg_7,R.drawable.app_bg_8};
	
	private GridView gridView;
	private TextView downloadTv;
	private AppRecommendAdapter adapter;
	
	private List<ApkInfo> apkLists = new ArrayList<ApkInfo>();
	private List<AppRecommendInfo> serviceList = new ArrayList<AppRecommendInfo>();
	
	private MyApp app;
	private AQuery aq;
	
	private FrameLayout flGv;
	
	private Button backBtn;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.activity_app_recommend);
		
		app = (MyApp) getApplication();
		aq = new AQuery(this);
		
		gridView = (GridView) findViewById(R.id.gv);
		downloadTv = (TextView) findViewById(R.id.tv_download_bg);
		backBtn = (Button) findViewById(R.id.bt_back);
		
		apkLists = PackageUtils.getInstalledApkInfos(this);
		
		gridView.setNextFocusUpId(R.id.bt_back);
		flGv = (FrameLayout) findViewById(R.id.fl_gv);
		
		initListener();
		
		adapter = new AppRecommendAdapter(this,aq, serviceList);
		gridView.setAdapter(adapter);
		
		getAppRecommendServiceData();
		
		IntentFilter filter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
		filter.addDataScheme("package");
		this.registerReceiver(reciver, filter);
		
		showDialog(DIALOG_WAITING);
	}
	
	private BroadcastReceiver reciver = new BroadcastReceiver(){

		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			
			Log.d("TAG", "--------------------------> app installed");
			 String packageName = intent.getData().getEncodedSchemeSpecificPart();
			 for(int i=0;i<serviceList.size();i++){
				 
				 if(packageName.equals(serviceList.get(i).getPackage_name())){
					 
					 apkLists = PackageUtils.getInstalledApkInfos(AppRecommendActivity.this);
					 for(ApkInfo info:apkLists){
						 
						 if(packageName.equals(info.getPackageName())){
							 
							 serviceList.get(i).setInstalled(true);
							 downloadTv.setText("已安装");
							 adapter.notifyDataSetChanged();
						 }
					 }
					 
					 return;
				 }
			 }
			
		}
		
	};
	
	@Override
	protected Dialog onCreateDialog(int id) {
		// TODO Auto-generated method stub
		
		switch (id) {
		case DIALOG_WAITING:
			WaitingDialog dlg = new WaitingDialog(this);
			dlg.show();
			dlg.setOnCancelListener(new OnCancelListener() {

				@Override
				public void onCancel(DialogInterface dialog) {
					// TODO Auto-generated method stub
					finish();
				}
			});
			dlg.setDialogWindowStyle();
			return dlg;

		default:
			break;
		}
		return super.onCreateDialog(id);
	}
	
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		unregisterReceiver(reciver);
		super.onDestroy();
	}
	
	private void updateList(){
		
		for(int i=0;i<serviceList.size();i++){
			
			String servicePackageName = serviceList.get(i).getPackage_name();
			for(ApkInfo apkInfo:apkLists){
				
				String localPackageName = apkInfo.getPackageName();
				if(servicePackageName.equals(localPackageName)){
					
					serviceList.get(i).setInstalled(true);
				}
			}
		}
		
		adapter.notifyDataSetChanged();
		gridView.requestFocus();
		removeDialog(DIALOG_WAITING);
	}
	
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		MobclickAgent.onPause(this);
	}
	
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		MobclickAgent.onResume(this);
	}
	
	private void initListener(){
		
		backBtn.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				finish();
			}
		});
		
		backBtn.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				// TODO Auto-generated method stub
			
				if(hasFocus){
					
					downloadTv.setVisibility(View.INVISIBLE);
					gridView.setSelection(-1);
				}
			}
		});
		
		gridView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
				// TODO Auto-generated method stub
				
				if(view == null) {
					
					return;
				} 
				
				AppRecommendInfo info = serviceList.get(position);
				
				
				setStartDownLoadVisible(view, true,info.isInstalled());
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				// TODO Auto-generated method stub
				
			}
		});
		
		gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				// TODO Auto-generated method stub
				
				AppRecommendInfo info = serviceList.get(position);
				Log.d(TAG, "onItemClick-->info:" + info.getPackage_name());
				if(!info.isInstalled()){
					
					List<PushedApkDownLoadInfo> tempList = FayeService.userPushApkInfos;
					if(tempList!= null && tempList.size() > 0){
						
						for(int i=0;i<tempList.size();i++){
							
							if(info.getPackage_name().equals(tempList.get(i).getPackageName())){
								Log.d(TAG, "onItemClick-->info:" + info.getPackage_name());
								
								startActivity(new Intent(AppRecommendActivity.this,ManagePushApkActivity.class));
								return;
							}
						}
					}
					
					//进行下载
					ApkDownloadInfoParcel infoParcel = new ApkDownloadInfoParcel();
					infoParcel.setApk_url(info.getApk_url());
					infoParcel.setApp_name(info.getApp_name());
					infoParcel.setIcon_url(info.getIcon_url());
					infoParcel.setMd5(info.getMd5());
					infoParcel.setVersion(info.getVersion_name());
					infoParcel.setPackage_name(info.getPackage_name());
					
//					infoParcel.setApk_url("http://upgrade.joyplus.tv/joyplustv/joyplustv.apk");
//					infoParcel.setApp_name("悦视频");
//					infoParcel.setIcon_url("");
//					infoParcel.setMd5("");
//					infoParcel.setVersion("");
//					infoParcel.setPackage_name("");
					Intent downloadApkIntent  = new Intent(Global.ACTION_NEW_APK_DWONLOAD);
					downloadApkIntent.putExtra("new_apk_download", infoParcel);
					sendBroadcast(downloadApkIntent);
					startActivity(new Intent(AppRecommendActivity.this,ManagePushApkActivity.class));
				}else {
					
					Utils.showToast(AppRecommendActivity.this, "已经安装");
				}
			}
		});
	}
	
	private void setStartDownLoadVisible(View v,boolean isVisible,boolean isInstall) {
		
		if( v == null) {
			
			return;
		}
	
		Log.i(TAG, "isVisible--->x:" + v.getX() + " y:" + v.getY()
				+ " w:" + v.getWidth() + " h:"+ v.getHeight());
		if(isInstall){
			
			downloadTv.setText("已安装");
		}else{
			
			downloadTv.setText("立即下载");
		}
		if(isVisible){
			
			downloadTv.setVisibility(View.VISIBLE);
			
			FrameLayout.LayoutParams param = new FrameLayout.LayoutParams(v.getWidth(), v.getHeight()/5 + Utils.getStandardValue(this,4));
			param.setMargins((int)v.getX(), (int)(v.getY() +v.getHeight()/5 * 4) , 0, 0);
			downloadTv.setLayoutParams(param);
		}
		
		Log.i(TAG, "downloadTv--->x:" + downloadTv.getX() + " y:" + downloadTv.getY()
				+ " w:" + downloadTv.getWidth() + " h:"+ downloadTv.getHeight());
//		
		
	}
	
	protected void getServiceData(String url, String interfaceName) {
		// TODO Auto-generated method stub

		AjaxCallback<JSONObject> cb = new AjaxCallback<JSONObject>();
		cb.SetHeader(app.getHeaders());
		cb.url(url).type(JSONObject.class).weakHandler(this, interfaceName);

		Log.d(TAG, url);
		Log.d(TAG, "header appkey" + app.getHeaders().get("app_key"));

		aq.ajax(cb);
	}
	
	private void getAppRecommendServiceData(){
		
		String url = Constant.BASE_URL + "/top_app";
		getServiceData(url, "initAppRecommendServiceData");
	}
	
	public void initAppRecommendServiceData(String url, JSONObject json, AjaxStatus status) {

		if (status.getCode() == AjaxStatus.NETWORK_ERROR || json == null) {
			Utils.showToast(this,
					getResources().getString(R.string.networknotwork));

			return;
		}

		if (json == null || json.equals(""))
			return;

		Log.d(TAG, "initTvLivingServiceData = " + json.toString());
		ObjectMapper mapper = new ObjectMapper();
		try {
			AppRecommendView appRecommendView = mapper.readValue(json.toString(),
					AppRecommendView.class);

			if(serviceList != null ){
				
				serviceList.clear();
			}
			if(appRecommendView != null && appRecommendView.resources != null) {
				
				for(int i=0;i<appRecommendView.resources.length;i++){
					
					AppRecommendInfo info = new AppRecommendInfo();
					info.setApk_url(appRecommendView.resources[i].apk_url);
					info.setApp_name(appRecommendView.resources[i].app_name);
					
					info.setIcon_url(appRecommendView.resources[i].icon_url);
					info.setPic_url(appRecommendView.resources[i].pic_url);
					info.setMd5(appRecommendView.resources[i].md5);
					info.setPackage_name(appRecommendView.resources[i].package_name);
					info.setVersion_name(appRecommendView.resources[i].version_name);
					info.setVersion_code(appRecommendView.resources[i].version_code);
					info.setApk_size(appRecommendView.resources[i].apk_size);
					Log.d(TAG, "info--->" + info.toString());
					serviceList.add(info);
				}
			}
			updateList();
		} catch (JsonParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
