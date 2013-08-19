package com.joyplus.tvhelper;

import info.monitorenter.cpdetector.io.CodepageDetectorProxy;
import info.monitorenter.cpdetector.io.JChardetFacade;
import info.monitorenter.cpdetector.io.ParsingDetector;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.blaznyoght.subtitles.model.Collection;
import org.blaznyoght.subtitles.model.Parser;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.TrafficStats;
import android.net.Uri;
import android.net.http.AndroidHttpClient;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.webkit.URLUtil;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.Gallery;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.VideoView;

import com.androidquery.AQuery;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.joyplus.tvhelper.db.DBServices;
import com.joyplus.tvhelper.entity.CurrentPlayDetailData;
import com.joyplus.tvhelper.entity.MoviePlayHistoryInfo;
import com.joyplus.tvhelper.entity.URLS_INDEX;
import com.joyplus.tvhelper.entity.VideoPlayUrl;
import com.joyplus.tvhelper.entity.XLLXFileInfo;
import com.joyplus.tvhelper.entity.service.ReturnProgramView;
import com.joyplus.tvhelper.ui.ArcView;
import com.joyplus.tvhelper.utils.BangDanConstant;
import com.joyplus.tvhelper.utils.Constant;
import com.joyplus.tvhelper.utils.DefinationComparatorIndex;
import com.joyplus.tvhelper.utils.DesUtils;
import com.joyplus.tvhelper.utils.HttpTools;
import com.joyplus.tvhelper.utils.Log;
import com.joyplus.tvhelper.utils.PreferencesUtils;
import com.joyplus.tvhelper.utils.SouceComparatorIndex1;
import com.joyplus.tvhelper.utils.Utils;
import com.joyplus.tvhelper.utils.XunLeiLiXianUtil;
import com.umeng.analytics.MobclickAgent;

public class VideoPlayerJPActivity extends Activity implements
		MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener,
		MediaPlayer.OnPreparedListener, MediaPlayer.OnBufferingUpdateListener,
		MediaPlayer.OnInfoListener, MediaPlayer.OnSeekCompleteListener,
		MediaPlayer.OnVideoSizeChangedListener, OnSeekBarChangeListener,
		OnClickListener{

	private static final String TAG = "VideoPlayerActivity";

	private static final int MESSAGE_RETURN_DATE_OK = 0;
	private static final int MESSAGE_URLS_READY = MESSAGE_RETURN_DATE_OK + 1;
	private static final int MESSAGE_PALY_URL_OK = MESSAGE_URLS_READY + 1;
	private static final int MESSAGE_URL_NEXT = MESSAGE_PALY_URL_OK + 1;
	private static final int MESSAGE_UPDATE_PROGRESS = MESSAGE_URL_NEXT + 1;
	private static final int MESSAGE_HIDE_PROGRESSBAR = MESSAGE_UPDATE_PROGRESS + 1;
	private static final int MESSAGE_HIDE_VOICE = MESSAGE_HIDE_PROGRESSBAR + 1;
	
	public static final int TYPE_XUNLEI = -10;
	public static final int TYPE_PUSH = TYPE_XUNLEI -1;
	public static final int TYPE_LOCAL = TYPE_PUSH -1;
	
	private MoviePlayHistoryInfo play_info;
	private boolean isRequset = false;

	/**
	 * 数据加载
	 */
	private static final int STATUE_LOADING = 0;
	/**
	 * 播放
	 */
	private static final int STATUE_PLAYING = STATUE_LOADING + 1;
	/**
	 * 暂停
	 */
	private static final int STATUE_PAUSE = STATUE_PLAYING + 1;
	/**
	 * 快退、快进
	 */
	private static final int STATUE_FAST_DRAG = STATUE_PAUSE + 1;

	private static final int OFFSET = 33;
	private int seekBarWidthOffset = 40;
	
	private static final int SEEKBAR_REFRESH_TIME = 200;//refresh time

	private TextView mVideoNameText; // 名字
	private ImageView mDefinationIcon;// 清晰度icon
	private SeekBar mSeekBar; // 进度条
	private RelativeLayout mTimeLayout; // 时间提示块
	private TextView mCurrentTimeTextView; // 当前播放时间
	private TextView mTotalTimeTextView; // 总时长
	private RelativeLayout mFastIcon; // 快进（退）标识图标
	private TextView mFastTextView; // 快进（退）标识提示

	private TextView mLastTimeTextView;// 上次播放时间
	private TextView mResourceTextView;// 视频来源
	private TextView mSpeedTextView;// 网速
	private TextView mPercentTextView;// 完成百分比

	private ImageButton mPreButton;// 上一集
	private ImageButton mNextButton;// 下一集
	private ImageButton mTopButton;// 上面的（继续）按钮
	private ImageButton mBottomButton;// 上面的（收藏）按钮
	private ImageButton mCenterButton;// 中间的按钮

	private ImageButton mContinueButton;// 继续

	private ArcView mVoiceProgress; // 声音大小显示

	/**
	 * 预加载层
	 */
	private RelativeLayout mPreLoadLayout;
	/**
	 * 播放提示相关层
	 */
	private RelativeLayout mNoticeLayout;
	/**
	 * 上下集控制层
	 */
	private LinearLayout mControlLayout;
	/**
	 * 声音相关层
	 */
	private LinearLayout mVocieLayout;

	/**
	 * 暂停继续层
	 */
	private LinearLayout mContinueLayout;
	/**
	 * subtitle
	 */
	private TextView mSubTitleTv;
	/**
	 * 基本播放参数
	 */
	private String mProd_id;
	private String mProd_name;
	private int mProd_type;
	private String mProd_src;// 来源
	
	private String url_temp;//首次url备份
	private int mDefination = 0; // 清晰度 6为尝鲜，7为普清，8为高清
	private String mProd_sub_name = null;
	private int mEpisodeIndex = -1; // 当前集数对应的index
	private long lastTime = 0;

	/**
	 * 收藏
	 */
	private boolean isShoucang = false;// 默认为没有收藏

	/**
	 * 网络数据
	 */
	private int currentPlayIndex;
	private String currentPlayUrl;
	private ReturnProgramView m_ReturnProgramView = null;
	private List<URLS_INDEX> playUrls = new ArrayList<URLS_INDEX>();
	
	private List<URLS_INDEX> playUrls_hd2 = new ArrayList<URLS_INDEX>();//超清
	private List<URLS_INDEX> playUrls_hd = new ArrayList<URLS_INDEX>();//高清
	private List<URLS_INDEX> playUrls_mp4 = new ArrayList<URLS_INDEX>();//标清
	private List<URLS_INDEX> playUrls_flv = new ArrayList<URLS_INDEX>();//流畅
	ArrayList<Integer> definationStrings = new ArrayList<Integer>();//清晰度选择
	ArrayList<Integer> zimuStrings = new ArrayList<Integer>();

	private AQuery aq;
	private MyApp app;

	private long mStartRX = 0;
	private long rxByteslast = 0;
	private long mLoadingPreparedPercent = 0;

	private int mStatue = 0;

	private int mTimeJumpSpeed = 0;
	private int mFastJumpTime = 0;
	int[] mTimes = { 1000, 333, 40 };

	/**
	 * android本身VideoView
	 */
	private VideoView mVideoView;

	private AudioManager mAudioManager;

	/** 最大声音 */
	private int mMaxVolume;
	/** 当前声音 */
	private int mVolume = -1;
	
	private Animation mAlphaDispear;
	private boolean isSeekBarIntoch = false;
	
	/**  Subtitle*/
	private Collection mSubTitleCollection = null;
	private int mStartTimeSubTitle,mEndTimeSubTitle;
	private org.blaznyoght.subtitles.model.Element mCurSubTitleE,mBefSubTitleE;
	
	private BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();

			if (action.equals(Constant.VIDEOPLAYERCMD)) {
				int mCMD = intent.getIntExtra("cmd", 0);
				Log.d(TAG, "onReceive------>" + mCMD);
				String mContent = intent.getStringExtra("content");
				String mProd_url = intent.getStringExtra("prod_url");
				if (!mProd_url.equalsIgnoreCase(url_temp)){
					Log.d(TAG, "mProd_url != url_temp");
					return ;
				}
				
				
				/*
				 * “403”：视频推送后，手机发送播放指令。 “405”：视频推送后，手机发送暂停指令。
				 * “407”：视频推送后，手机发送快进指令。 “409”：视频推送后，手机发送后退指令。
				 */
				switch (mCMD) {
				case 403:
					if (!mVideoView.isPlaying()) {
						mStatue = STATUE_PLAYING;
						mSeekBar.setEnabled(true);
						mVideoView.start();
						mContinueLayout.setVisibility(View.GONE);
						mControlLayout.setVisibility(View.GONE);
						mHandler.sendEmptyMessageDelayed(
								MESSAGE_HIDE_PROGRESSBAR, 2500);
					}
					break;
				case 405:
					if (mVideoView.isPlaying()) {
						mVideoView.pause();
						mStatue = STATUE_PAUSE;
						mSeekBar.setEnabled(false);
						mNoticeLayout.setVisibility(View.VISIBLE);
						mContinueLayout.setVisibility(View.VISIBLE);
						mContinueButton.requestFocus();
					}
					break;
				case 407:
					if (Integer.parseInt(mContent) <= mVideoView.getDuration()) {
						int destination = Integer.parseInt(mContent);
						if (destination < mVideoView.getDuration()) {
							mVideoView.seekTo(destination);
						}
						mNoticeLayout.setVisibility(View.VISIBLE);
						mHandler.sendEmptyMessageDelayed(
								MESSAGE_HIDE_PROGRESSBAR, 2500);
						// mVideoView.seekTo(c)
						// if (mPlayer.getDuration() -
						// Integer.parseInt(mContent) < 10000
						// && mCurrentPlayData.prod_type != 1)// 下一集
						// mPlayer.OnContinueVideoPlay();
						// else
						// mPlayer.onSeekMove(Integer.parseInt(mContent));
					}
					break;
				case 409:
					finish();
					break;
				}

			} else {
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		
		Log.i(TAG, "onCreate--->");
		setContentView(R.layout.video_player_main);
		aq = new AQuery(this);
		app = (MyApp) getApplication();
		mAlphaDispear = AnimationUtils.loadAnimation(this, R.anim.alpha_disappear);
		
		initViews();
		mSeekBar.setEnabled(false);
		m_ReturnProgramView = app.get_ReturnProgramView();
		initVedioDate();

		Window win = getWindow();
		WindowManager.LayoutParams winParams = win.getAttributes();
		winParams.buttonBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_OFF;
		// winParams.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
		win.setAttributes(winParams);

		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(Constant.VIDEOPLAYERCMD);
		registerReceiver(mReceiver, intentFilter);

	}
	
	private void dismissView(View v){
		v.setVisibility(View.GONE);
		v.startAnimation(mAlphaDispear);
	}

	private void initVedioDate() {
		mStatue = STATUE_LOADING;
		mSeekBar.setEnabled(false);
		mSeekBar.setProgress(0);
		mTotalTimeTextView.setText("--:--");
		mPreLoadLayout.setVisibility(View.VISIBLE);
		mNoticeLayout.setVisibility(View.VISIBLE);
		mContinueLayout.setVisibility(View.GONE);
		mControlLayout.setVisibility(View.GONE);
		mStartRX = TrafficStats.getTotalRxBytes();// 获取网络速度
		if (mStartRX == TrafficStats.UNSUPPORTED) {
			mSpeedTextView
					.setText("Your device does not support traffic stat monitoring.");
		} else {

			mHandler.postDelayed(mLoadingRunnable, 500);
		}
		// 点击某部影片播放时，会全局设置CurrentPlayData
		CurrentPlayDetailData playDate = app.getmCurrentPlayDetailData();
		if (playDate == null) {// 如果不设置就不播放
			Log.e(TAG, "playDate----->null");
			finish();
			return;
		}
		// 初始化基本播放数据
		mProd_id = playDate.prod_id;
		mProd_type = playDate.prod_type;
		mProd_name = playDate.prod_name;
		mProd_sub_name = playDate.prod_sub_name;
		currentPlayUrl = playDate.prod_url;
		url_temp = playDate.prod_url;
		mDefination = playDate.prod_qua;
		lastTime = (int) playDate.prod_time;
		mProd_src = playDate.prod_src;
		
		if(mProd_type == TYPE_PUSH || mProd_type == TYPE_LOCAL){
			play_info = (MoviePlayHistoryInfo) playDate.obj;
		}

		Log.d(TAG, "name ----->" + mProd_name);
		Log.d(TAG, "currentPlayUrl ----->" + currentPlayUrl);
		Log.d(TAG, "mProd_type ----->" + mProd_type);
		
		if(mDefination == 0){
			mDefination = 8;
		}
		
		// 更新播放来源和上次播放时间
		updateSourceAndTime();
		updateName();
		if(mProd_type != TYPE_XUNLEI) {
			
			if(mProd_type == TYPE_LOCAL){
				if(currentPlayUrl!=null){
					mHandler.sendEmptyMessage(MESSAGE_PALY_URL_OK);
				}
				return;
			}
			if (currentPlayUrl != null && URLUtil.isNetworkUrl(currentPlayUrl)) {
				if (mProd_type<0) {
//					mHandler.sendEmptyMessage(MESSAGE_PALY_URL_OK);
					if(mProd_type == TYPE_PUSH){
						new Thread(new UrlRedirectTask()).start();
					}
					return ;
				} else {
					if (app.get_ReturnProgramView() != null) {// 如果不为空，获取服务器返回的详细数据

						m_ReturnProgramView = app.get_ReturnProgramView();
						mHandler.sendEmptyMessage(MESSAGE_RETURN_DATE_OK);
					} else {// 如果为空，就重新获取

						getProgramViewDetailServiceData();
					}
				}
			} else {
				if(mProd_type == TYPE_PUSH){
					MyApp.pool.execute(new getPlayList());
				}else{
					if (app.get_ReturnProgramView() != null) {// 如果不为空，获取服务器返回的详细数据

						m_ReturnProgramView = app.get_ReturnProgramView();
						mHandler.sendEmptyMessage(MESSAGE_RETURN_DATE_OK);
					} else {// 如果为空，就重新获取

						getProgramViewDetailServiceData();
					}
				}
			}
		}else {//迅雷 传递-10
			
			//取list
			MyApp.pool.execute(new Runnable() {
				
				@Override
				public void run() {
					// TODO Auto-generated method stub
					
					XLLXFileInfo xllxFileInfo = (XLLXFileInfo) app.getmCurrentPlayDetailData().obj;
					if(xllxFileInfo != null && !xllxFileInfo.isDir) {
						
						ArrayList<VideoPlayUrl> list = 
								XunLeiLiXianUtil.getLXPlayUrl(VideoPlayerJPActivity.this, xllxFileInfo);
						//get subtitle
						byte[] subTitle = XunLeiLiXianUtil.getSubtitle(VideoPlayerJPActivity.this,xllxFileInfo);
						initSubTitleCollection(subTitle);
						
						if(list != null && list.size() > 0) {
							
							if(playUrls != null && playUrls.size() > 0) {
								
								playUrls.clear();
							}
							for(int i=0;i<list.size();i++) {
								
								VideoPlayUrl videoPlayUrl = list.get(i);
								Log.i(TAG, "VideoPlayUrl--->" + videoPlayUrl.toString());
								if(videoPlayUrl != null && videoPlayUrl.playurl != null) {
									
									URLS_INDEX url = new URLS_INDEX();
									url.url = videoPlayUrl.playurl;
									url.source_from = "XUNLEI";
									if(videoPlayUrl.isCanDrag){// can drag hd2 hd mp4 
										
										if(videoPlayUrl.sharp != null) {
											
											int index = videoPlayUrl.sharp.getIndex();
											switch (index) {
											case 0:
												url.defination_from_server ="mp4";
												break;
											case 2:
												url.defination_from_server ="hd";
												break;
											case 3:
												url.defination_from_server ="hd2";
												break;

											default:
												break;
											}
										}
									}else {//can't drag flv
										url.defination_from_server ="flv";
//										playUrls_flv.a
									}
								
									playUrls.add(url);
								}
							}
							
						}
					}
					initFourList();
					sortPushUrls(mDefination);
					mHandler.sendEmptyMessage(MESSAGE_URLS_READY);
				}
			});
//			URLS_INDEX url = new URLS_INDEX();
//			url.defination_from_server ="hd2";
//			url.source_from = "xunlei";
//			url.url ="";
			
//			playUrls = new ArrayList<URLS_INDEX>
			
		}
	}
	
	private void initSubTitleCollection(byte[] subTitle){
		
		if(subTitle != null && subTitle.length > 3
				&& mSubTitleCollection == null){
			
			Parser parser = new Parser();
			
			String charsetName = Utils.getCharset(subTitle, 128);
			Log.d(TAG, "initSubTitleCollection-->charsetName:" + charsetName);
			if(charsetName.equals("")){
				
				boolean isUtf8 = Utils.isUTF_8(subTitle);
				Log.i(TAG, "isUtf8--->" + isUtf8);
				
				if(!isUtf8){
					
					parser.setCharset("GBK");
				} else {
					
					parser.setCharset("UTF-8");
				}
			}else {
				
				parser.setCharset(charsetName);
			}
			parser.parse(new ByteArrayInputStream(subTitle));
			
			mSubTitleCollection = parser.getCollection();
			Log.d(TAG, "mSubTitleCollection--->" + mSubTitleCollection.toString());
			return;
		}
		
//		Utils.showToast(this, "获取字幕失败");
	}

	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			switch (msg.what) {
			case MESSAGE_RETURN_DATE_OK:
				new Thread(new PrepareTask()).start();
				break;
			case MESSAGE_URLS_READY:// url 准备好了
				if(playUrls.size()<=0){
					if(mProd_type==TYPE_PUSH){
						if(isRequset){
							if(URLUtil.isNetworkUrl(URLDecoder.decode(play_info.getPush_url()))){
								Intent intent_web = new Intent(VideoPlayerJPActivity.this, WebViewActivity.class);
								intent_web.putExtra("url", URLDecoder.decode(play_info.getPush_url()));
								startActivity( intent_web);
							}else{
								if(!isFinishing()){
									showDialog(0);
								}
							}
						}else{
							//失效了 接着搞
							new Thread(new RequestNewUrl()).start();
						}
					}else if(mProd_type == TYPE_XUNLEI){
						
						if(!isFinishing()){
							showDialog(0);
						}
					}
					return;
				}
				
				//字幕获取
				if(mProd_type == TYPE_PUSH && mSubTitleCollection == null){
					
					if(play_info != null && play_info.getPush_url() != null
							&& !play_info.getPush_url().equals("")){
						MyApp.pool.execute(new Runnable() {
							
							@Override
							public void run() {
								// TODO Auto-generated method stub
								
								String subTitleUrl = Constant.BASE_URL + "/xunlei/subtitle/?url="
										+ URLEncoder.encode(play_info.getPush_url()) + "&md5_code=" + 
										PreferencesUtils.getPincodeMd5(VideoPlayerJPActivity.this);
								byte[] arraySubTitleBytes = XunLeiLiXianUtil.getSubtitle4Push(subTitleUrl, Constant.APPKEY);
								initSubTitleCollection(arraySubTitleBytes);
							}
						});

					}
					
				}
				
				currentPlayIndex = 0;
				currentPlayUrl = playUrls.get(currentPlayIndex).url;
				Log.d(TAG,"MESSAGE_URLS_READY--->currentPlayUrl:" + currentPlayUrl);
				mProd_src = playUrls.get(currentPlayIndex).source_from;
				if (currentPlayUrl != null
						&& URLUtil.isNetworkUrl(currentPlayUrl)) {
					// 地址跳转相关。。。
					new Thread(new UrlRedirectTask()).start();
//					mHandler.sendEmptyMessage(MESSAGE_PALY_URL_OK);
					// 要根据不同的节目做相应的处理。这里仅仅是为了验证上下集
				}else {
					
					mHandler.sendEmptyMessage(MESSAGE_URL_NEXT);
				}
				break;
			case MESSAGE_URL_NEXT:
				if (playUrls.size() <= 0) {
					if (app.get_ReturnProgramView() != null) {
						m_ReturnProgramView = app.get_ReturnProgramView();
						mHandler.sendEmptyMessage(MESSAGE_RETURN_DATE_OK);
					} else {
						if (mProd_type > 0 && !"-1".equals(mProd_id)
								&& mProd_id != null) {
							getProgramViewDetailServiceData();
						}else if(mProd_type==TYPE_LOCAL){
							if(!isFinishing()){
								showDialog(0);
							}
						}
						else if(mProd_type==TYPE_PUSH){
							if(isRequset){
								if(URLUtil.isNetworkUrl(URLDecoder.decode(play_info.getPush_url()))){
									Intent intent_web = new Intent(VideoPlayerJPActivity.this, WebViewActivity.class);
									intent_web.putExtra("url", URLDecoder.decode(play_info.getPush_url()));
									startActivity( intent_web);
								}else{
									if(!isFinishing()){
										showDialog(0);
									}
								}
								
							}else{
								//失效了 接着搞
								new Thread(new RequestNewUrl()).start();
							}
						}
					}
				} else {
					if (currentPlayIndex < playUrls.size() - 1) {
						currentPlayIndex += 1;
						currentPlayUrl = playUrls.get(currentPlayIndex).url;
						mProd_src = playUrls.get(currentPlayIndex).source_from;
						if (currentPlayUrl != null
								&& URLUtil.isNetworkUrl(currentPlayUrl)) {
							// 地址跳转相关。。。
							Log.d(TAG, "currentPlayUrl" + currentPlayUrl);
							Log.d(TAG, "mProd_src" + mProd_src);
							new Thread(new UrlRedirectTask()).start();
//							mHandler.sendEmptyMessage(MESSAGE_PALY_URL_OK);
						}
					} else {
						// 所有的片源都不能播放
						Log.e(TAG, "no url can play!");
						if(mProd_type==TYPE_PUSH){
							if(isRequset){
								if(URLUtil.isNetworkUrl(URLDecoder.decode(play_info.getPush_url()))){
									Intent intent_web = new Intent(VideoPlayerJPActivity.this, WebViewActivity.class);
									intent_web.putExtra("url", URLDecoder.decode(play_info.getPush_url()));
									startActivity(intent_web);
								}else{
									if(!isFinishing()){
										showDialog(0);
									}
								}
							}else{
								//失效了 接着搞
								new Thread(new RequestNewUrl()).start();
							}
						}else if(!VideoPlayerJPActivity.this.isFinishing()){
							showDialog(0);
							
							//所有url不能播放，向服务器传递-1
//							saveToServer(-1, 0);
						}
					}
				}
				break;
			case MESSAGE_PALY_URL_OK:
				
				updateName();
				updateSourceAndTime();
				mVideoView.setVideoURI(Uri.parse(currentPlayUrl));
				if (lastTime > 0) {
					mVideoView.seekTo((int) lastTime);
				}
				mVideoView.start();
				break;
			case MESSAGE_UPDATE_PROGRESS:
				updateSeekBar();
				
				break;
			case MESSAGE_HIDE_PROGRESSBAR:
				dismissView(mNoticeLayout);
//				mNoticeLayout.setVisibility(View.GONE);
				break;
			case MESSAGE_HIDE_VOICE:
				dismissView(mVocieLayout);
//				mVocieLayout.setVisibility(View.GONE);
				break;
			default:
				break;
			}
		}
	};

	private void updateName() {
		switch (mProd_type) {
		case -1:
		case 1:
			mVideoNameText.setText(mProd_name);
			break;
		case 2:
		case 131:
			mVideoNameText.setText(mProd_name + " 第" + mProd_sub_name + "集");
			break;
		case 3:
			mVideoNameText.setText(mProd_name + " " + mProd_sub_name);
			break;
		default:
			mVideoNameText.setText(mProd_name);
		}
	}

	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		
		Log.i(TAG, "onStart--->");
		
		super.onStart();
		mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
		mAudioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC,
				AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
		mMaxVolume = mAudioManager
				.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
	}

	private void onVolumeSlide(int index) {
		if (index > mMaxVolume)
			index = mMaxVolume;
		else if (index < 0)
			index = 0;

		// 变更声音
		mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, index, 0);
		int mAngle = index * 360 / mMaxVolume;
		// 变更进度条
		if (index == 0)
			mVoiceProgress.setBackgroundResource(R.drawable.player_volume_mute);
		else {
			mVoiceProgress.setBackgroundResource(R.drawable.player_volume);

		}
		mVoiceProgress.SetAngle(mAngle);

	}

	private void initViews() {
		mVideoNameText = (TextView) findViewById(R.id.tv_play_name);
		mDefinationIcon = (ImageView) findViewById(R.id.iv_1080_720);
		mSeekBar = (SeekBar) findViewById(R.id.sb_seekbar);
		mTimeLayout = (RelativeLayout) findViewById(R.id.rl_popup_time);
		mCurrentTimeTextView = (TextView) findViewById(R.id.tv_popup_time_current_time);
		mTotalTimeTextView = (TextView) findViewById(R.id.tv_total_time);
		mFastIcon = (RelativeLayout) findViewById(R.id.rl_popup_time_fast);
		mFastTextView = (TextView) findViewById(R.id.tv_popup_time_fast);

		mLastTimeTextView = (TextView) findViewById(R.id.tv_preload_bofang_record);
		mResourceTextView = (TextView) findViewById(R.id.tv_preload_source_laizi);// 视频来源
		mSpeedTextView = (TextView) findViewById(R.id.tv_preload_network_kb);
		mPercentTextView = (TextView) findViewById(R.id.tv_preload_network_accomplish);

		mPreButton = (ImageButton) findViewById(R.id.ib_control_left);
		mNextButton = (ImageButton) findViewById(R.id.ib_control_right);
		mTopButton = (ImageButton) findViewById(R.id.ib_control_top);
		mBottomButton = (ImageButton) findViewById(R.id.ib_control_bottom);
		mCenterButton = (ImageButton) findViewById(R.id.ib_control_center);
		mContinueButton = (ImageButton) findViewById(R.id.btn_continue);
		
		mSubTitleTv = (TextView) findViewById(R.id.tv_subtitle);

		mPreButton.setOnClickListener(this);
		mNextButton.setOnClickListener(this);
		mTopButton.setOnClickListener(this);
		mBottomButton.setOnClickListener(this);
		mCenterButton.setOnClickListener(this);
		mContinueButton.setOnClickListener(this);

		mVoiceProgress = (ArcView) findViewById(R.id.av_volume);

		mPreLoadLayout = (RelativeLayout) findViewById(R.id.rl_preload);
		BitmapFactory.Options opt = new BitmapFactory.Options();
		// opt.inPreferredConfig = Bitmap.Config.RGB_565; // Each pixel is
		// stored 2 bytes
		// opt.inPreferredConfig = Bitmap.Config.ARGB_8888; //Each pixel is
		// stored 4 bytes

		opt.inTempStorage = new byte[16 * 1024];
		opt.inPurgeable = true;
		opt.inInputShareable = true;

		try {
			mPreLoadLayout.setBackgroundDrawable(new BitmapDrawable(BitmapFactory.decodeResource(
					getResources(), R.drawable.player_bg, opt)));
		} catch (OutOfMemoryError e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		mNoticeLayout = (RelativeLayout) findViewById(R.id.rl_titile_seekbar);
		mControlLayout = (LinearLayout) findViewById(R.id.ll_control_buttons);
		mVocieLayout = (LinearLayout) findViewById(R.id.ll_volume);
		mContinueLayout = (LinearLayout) findViewById(R.id.ll_continue);
		mVideoView = (VideoView) findViewById(R.id.surface_view);
		mVideoView.setOnErrorListener(this);
		mVideoView.setOnCompletionListener(this);
		mVideoView.setOnPreparedListener(this);
		mVideoView.setOnTouchListener(new View.OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				// TODO Auto-generated method stub
				if(mStatue == STATUE_PLAYING){
					mHandler.removeMessages(MESSAGE_HIDE_PROGRESSBAR);
					mNoticeLayout.setVisibility(View.VISIBLE);
					mHandler.sendEmptyMessageDelayed(MESSAGE_HIDE_PROGRESSBAR, 2500);	
				}
				return false;
			}
		});
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		Log.d(TAG, "keycode ---------->" + keyCode);
		Log.d(TAG, "mStatue ---------->" + mStatue);
		Log.d(TAG, "mProdType ---------->" + mProd_type);
		switch (keyCode) {
		case KeyEvent.KEYCODE_BACK:
		case KeyEvent.KEYCODE_ESCAPE:
			switch (mStatue) {
			case STATUE_LOADING:
				finish();
				return true;
			case STATUE_PLAYING:
				if (mProd_type == 2 || mProd_type == 131 || mProd_type == 3) {
					showControlLayout();
					return true;
				} else {
					// mVideoView.stopPlayback();
					finish();
				}
				break;
			case STATUE_PAUSE:
				return true;
			case STATUE_FAST_DRAG:
				mTimeJumpSpeed = 0;
				upDateFastTimeBar();
//				mHandler.removeMessages(MESSAGE_UPDATE_PROGRESS);
//				mHandler.sendEmptyMessageDelayed(MESSAGE_UPDATE_PROGRESS, SEEKBAR_REFRESH_TIME);
				endUpdateSeekBar();
				startUpdateSeekBar(SEEKBAR_REFRESH_TIME);
				mStatue = STATUE_PLAYING;
				mSeekBar.setProgress(mVideoView.getCurrentPosition());
				mSeekBar.setEnabled(true);
				return true;
			}
			break;
		case KeyEvent.KEYCODE_DPAD_CENTER:
		case KeyEvent.KEYCODE_ENTER:
			switch (mStatue) {
			case STATUE_PLAYING:
				
				mVocieLayout.setVisibility(View.GONE);
				mHandler.removeMessages(MESSAGE_HIDE_VOICE);
				mStatue = STATUE_PAUSE;
				mSeekBar.setEnabled(false);
				mVideoView.pause();
				mHandler.removeMessages(MESSAGE_HIDE_PROGRESSBAR);
				mContinueLayout.setVisibility(View.VISIBLE);
				mNoticeLayout.setVisibility(View.VISIBLE);
				mContinueButton.requestFocus();
				break;
			case STATUE_FAST_DRAG:
				if (mFastJumpTime < mVideoView.getDuration()) {
					mVideoView.seekTo(mFastJumpTime);
					mSeekBar.setProgress(mFastJumpTime);
				}else{
					mSeekBar.setProgress(mVideoView.getCurrentPosition());
				}
				mTimeJumpSpeed = 0;
				upDateFastTimeBar();
//				mHandler.removeMessages(MESSAGE_UPDATE_PROGRESS);
//				mHandler.sendEmptyMessageDelayed(MESSAGE_UPDATE_PROGRESS, SEEKBAR_REFRESH_TIME);
				endUpdateSeekBar();
				startUpdateSeekBar(SEEKBAR_REFRESH_TIME);
				mStatue = STATUE_PLAYING;
				mSeekBar.setEnabled(true);
				break;
			}
			break;
		case KeyEvent.KEYCODE_MENU:
			if(mStatue == STATUE_PLAYING&&(mProd_type == TYPE_PUSH||mProd_type==TYPE_XUNLEI)){
				try{
					final Dialog dialog = new AlertDialog.Builder(this).create();
					dialog.show();
					LayoutInflater inflater = LayoutInflater.from(this);
					View view = inflater.inflate(R.layout.video_choose_defination, null);
					Button btn_ok = (Button) view.findViewById(R.id.btn_ok_def);
					Button btn_cancel = (Button) view.findViewById(R.id.btn_cancle_def);
//					final LinearLayout layout_def = (LinearLayout) view.findViewById(R.id.layout_def);
//					final LinearLayout layout_zimu = (LinearLayout) view.findViewById(R.id.layout_zimu);
					final Gallery gallery = (Gallery) view.findViewById(R.id.gallery_def);
					final Gallery gallery_zm = (Gallery) view.findViewById(R.id.gallery_zimu);
					
					definationStrings.clear();
					zimuStrings.clear();
					
//					gallery.setOnFocusChangeListener(new OnFocusChangeListener() {
//						
//						@Override
//						public void onFocusChange(View v, boolean hasFocus) {
//							// TODO Auto-generated method stub
//							if(hasFocus){
//								layout_def.setBackgroundColor(Color.DKGRAY);
//								Log.d(TAG, "layout_def---DKGRAY-->");
//							}else{
//								layout_def.setBackgroundColor(Color.TRANSPARENT);
//							}
//						}
//					});
//					
//					gallery_zm.setOnFocusChangeListener(new OnFocusChangeListener() {
//						
//						@Override
//						public void onFocusChange(View v, boolean hasFocus) {
//							// TODO Auto-generated method stub
//							if(hasFocus){
//								Log.d(TAG, "layout_zimu---DKGRAY-->");
//								layout_zimu.setBackgroundColor(Color.DKGRAY);
//							}else{
//								layout_zimu.setBackgroundColor(Color.TRANSPARENT);
//							}
//						}
//					});
//					
					if(mSubTitleCollection == null){
						zimuStrings.add(-1);//暂无字幕
					}else{
						zimuStrings.add(0);//字幕开
						zimuStrings.add(1);//字幕关
					}
//					definationStrings.add("超    清");
//					definationStrings.add("高    清");
//					definationStrings.add("标    清");
//					definationStrings.add("流    畅");
					if(playUrls_hd2.size()>0){
						definationStrings.add(Constant.DEFINATION_HD2);
					}
					if(playUrls_hd.size()>0){
						definationStrings.add(Constant.DEFINATION_HD);
					}
					if(playUrls_mp4.size()>0){
						definationStrings.add(Constant.DEFINATION_MP4);
					}
					if(playUrls_flv.size()>0){
						definationStrings.add(Constant.DEFINATION_FLV);
					}
					
					gallery.setAdapter(new DefinationAdapter(this, definationStrings));
					gallery.setSelection(definationStrings.indexOf(mDefination));
					
					gallery_zm.setAdapter(new ZimuAdapter(this, zimuStrings));
					if(mSubTitleTv.getVisibility()==View.VISIBLE){
						gallery_zm.setSelection(0);
					}else{
						gallery_zm.setSelection(1);
					}
					
					gallery.requestFocus();
					btn_ok.setOnClickListener(new OnClickListener() {
						
						@Override
						public void onClick(View v) {
							// TODO Auto-generated method stub
							dialog.dismiss();
							if(gallery_zm.getChildCount()>1){
								if(gallery_zm.getSelectedItemPosition()==0){
									mSubTitleTv.setVisibility(View.VISIBLE);
								}else{
									mSubTitleTv.setVisibility(View.INVISIBLE);
								}
							}
							changeDefination(definationStrings.get(gallery.getSelectedItemPosition()));
						}
					});
					btn_cancel.setOnClickListener(new OnClickListener() {
						
						@Override
						public void onClick(View v) {
							// TODO Auto-generated method stub
							dialog.dismiss();
						}
					});
					dialog.setContentView(view);
				}catch (Exception e) {
					// TODO: handle exception
					e.printStackTrace();
				}
				
			}
			return true;
		case KeyEvent.KEYCODE_VOLUME_UP:
			if (mStatue == STATUE_PLAYING) {
				mHandler.removeMessages(MESSAGE_HIDE_VOICE);
				mVolume = mAudioManager
						.getStreamVolume(AudioManager.STREAM_MUSIC);
				if (mVolume < 0) {
					mVolume = 0;
				}
				mVocieLayout.setVisibility(View.VISIBLE);
				mVolume++;
				onVolumeSlide(mVolume);
				mHandler.sendEmptyMessageDelayed(MESSAGE_HIDE_VOICE, 2500);
			}
			return true;
		case KeyEvent.KEYCODE_DPAD_UP:
			if (mStatue == STATUE_PLAYING) {
				mHandler.removeMessages(MESSAGE_HIDE_VOICE);
				mVolume = mAudioManager
						.getStreamVolume(AudioManager.STREAM_MUSIC);
				if (mVolume < 0) {
					mVolume = 0;
				}
				mVocieLayout.setVisibility(View.VISIBLE);
				mVolume++;
				onVolumeSlide(mVolume);
				mHandler.sendEmptyMessageDelayed(MESSAGE_HIDE_VOICE, 2500);
				return true;
			}
			break;
		case KeyEvent.KEYCODE_VOLUME_DOWN:
			if (mStatue == STATUE_PLAYING) {
				mHandler.removeMessages(MESSAGE_HIDE_VOICE);
				mVolume = mAudioManager
						.getStreamVolume(AudioManager.STREAM_MUSIC);
				if (mVolume < 0) {
					mVolume = 0;
				}
				mVocieLayout.setVisibility(View.VISIBLE);
				mVolume--;
				onVolumeSlide(mVolume);
				mHandler.sendEmptyMessageDelayed(MESSAGE_HIDE_VOICE, 2500);
			}
			return true;
		case KeyEvent.KEYCODE_DPAD_DOWN:
			if (mStatue == STATUE_PLAYING) {
				mHandler.removeMessages(MESSAGE_HIDE_VOICE);
				mVolume = mAudioManager
						.getStreamVolume(AudioManager.STREAM_MUSIC);
				if (mVolume < 0) {
					mVolume = 0;
				}
				mVocieLayout.setVisibility(View.VISIBLE);
				mVolume--;
				onVolumeSlide(mVolume);
				mHandler.sendEmptyMessageDelayed(MESSAGE_HIDE_VOICE, 2500);
				return true;
			}
			break;
		case KeyEvent.KEYCODE_DPAD_LEFT:
			if (mStatue == STATUE_PLAYING) {
				mStatue = STATUE_FAST_DRAG;
				mSeekBar.setEnabled(false);
				mTimeJumpSpeed = -1;
				mFastJumpTime = (int) mVideoView.getCurrentPosition();
				upDateFastTimeBar();
				return true;
			} else if (mStatue == STATUE_FAST_DRAG) {
				switch (mTimeJumpSpeed) {
				case -1:
				case -2:
					mTimeJumpSpeed -= 1;
					break;
				case 1:
					mTimeJumpSpeed = -1;
					break;
				case 2:
				case 3:
					mTimeJumpSpeed = 1;
				}
				upDateFastTimeBar();
				return true;
			}
			break;
		case KeyEvent.KEYCODE_DPAD_RIGHT:
			if (mStatue == STATUE_PLAYING) {
				mStatue = STATUE_FAST_DRAG;
				mSeekBar.setEnabled(false);
				mTimeJumpSpeed = 1;
				mFastJumpTime = (int) mVideoView.getCurrentPosition();
				upDateFastTimeBar();
				return true;
			} else if (mStatue == STATUE_FAST_DRAG) {
				switch (mTimeJumpSpeed) {
				case 1:
				case 2:
					mTimeJumpSpeed += 1;
					break;
				case -1:
					mTimeJumpSpeed = 1;
					break;
				case -2:
				case -3:
					mTimeJumpSpeed = -1;
				}
				upDateFastTimeBar();
				return true;
			}
			break;
		default:
			break;
		}
		return super.onKeyDown(keyCode, event);
	}
	
	private void startUpdateSeekBar(long time){
		
		mHandler.sendEmptyMessageDelayed(MESSAGE_UPDATE_PROGRESS, time);
		
		if(time == SEEKBAR_REFRESH_TIME || mStatue == STATUE_PLAYING){
			
			updateSubtitle();
		}else {
			
			mCurSubTitleE = null;//当前
			mBefSubTitleE = null;//之前
			mSubTitleTv.setText("");
		}
		
		
		
	}
	
	private void updateSubtitle(){
		
		if(mVideoView != null && mVideoView.getCurrentPosition() >= 0){
			long currentPosition = mVideoView.getCurrentPosition();
			if(mSubTitleCollection != null){
				
				if(mCurSubTitleE == null) {
					
					for(int i=0;i<mSubTitleCollection.getElementSize();i++){
						
						org.blaznyoght.subtitles.model.Element element = 
								mSubTitleCollection.getElements().get(i);
						if(currentPosition < element.getStartTime().getTime()){
							
							mCurSubTitleE = element;
							
							break;
						}
					}
				} else {
					
					long startTime = mCurSubTitleE.getStartTime().getTime();
					long endTime = mCurSubTitleE.getEndTime().getTime();
					
					if(currentPosition - startTime > 0){
						
						if(mSubTitleTv.getText().toString().equals("")){

							Log.d(TAG, "subtitle start--->startTime:" + startTime);
							if(mBefSubTitleE == null
									|| mCurSubTitleE.getRank() - mBefSubTitleE.getRank() == 0
									|| mCurSubTitleE.getRank() - mBefSubTitleE.getRank() == 1){
								mSubTitleTv.setText(mCurSubTitleE.getText().replaceAll("<font.*>", ""));
							}else {
								
								StringBuilder sb = new StringBuilder();
								for(int i=mBefSubTitleE.getRank();i<mCurSubTitleE.getRank();i++){
									org.blaznyoght.subtitles.model.Element element = 
											mSubTitleCollection.getElements().get(i);
									sb.append(element.getText().replaceAll("<font.*>", ""));
//									if(i<=mCurSubTitleE.getRank() -1){
//										
//										sb.append("\n");
//									}
									mSubTitleTv.setText(sb.toString());
								}
							}
							
							mBefSubTitleE = mCurSubTitleE;
						}
					}
					
					if (currentPosition - endTime > 0) {
						Log.d(TAG, "subtitle over--->endTime:" + endTime);
						if(!mSubTitleTv.getText().toString().equals("")){
							
							mSubTitleTv.setText("");
							mCurSubTitleE = null;
						}
						
					}
				}
			}
		}
	}
	
	private void endUpdateSeekBar(){
		
		mHandler.removeMessages(MESSAGE_UPDATE_PROGRESS);
		mCurSubTitleE = null;//当前
		mBefSubTitleE = null;//之前
	}

	private void showControlLayout() {
		// 判断上下集能不能用
		Log.d(TAG, "mEpisodeIndex----->" + mEpisodeIndex);
		if (mProd_type == 3) {
			if (mEpisodeIndex > 0&&m_ReturnProgramView.show.episodes[mEpisodeIndex-1].down_urls!=null) {
				mNextButton.setEnabled(true);
				mNextButton.setFocusable(true);
			} else {
				mNextButton.setEnabled(false);
				mNextButton.setFocusable(false);
			}

			if (mEpisodeIndex < (m_ReturnProgramView.show.episodes.length - 1)&&m_ReturnProgramView.show.episodes[mEpisodeIndex+1].down_urls!=null) {
				mPreButton.setEnabled(true);
				mPreButton.setFocusable(true);
			} else {
				mPreButton.setEnabled(false);
				mPreButton.setFocusable(false);
			}

		} else {
			if (mEpisodeIndex > 0&&m_ReturnProgramView.tv.episodes[mEpisodeIndex-1].down_urls!=null) {
				mPreButton.setEnabled(true);
				mPreButton.setFocusable(true);
			} else {
				mPreButton.setEnabled(false);
				mPreButton.setFocusable(false);
			}

			if (mEpisodeIndex < (m_ReturnProgramView.tv.episodes.length - 1)&&m_ReturnProgramView.tv.episodes[mEpisodeIndex+1].down_urls!=null) {
				mNextButton.setEnabled(true);
				mNextButton.setFocusable(true);
			} else {
				mNextButton.setEnabled(false);
				mNextButton.setFocusable(false);
			}
		}

		if (isShoucang) {

			mBottomButton.setBackgroundResource(R.drawable.player_btn_unfav);
		} else {

			mBottomButton.setBackgroundResource(R.drawable.player_btn_fav);
		}
		
		mVocieLayout.setVisibility(View.GONE);
		mHandler.removeMessages(MESSAGE_HIDE_VOICE);
		mStatue = STATUE_PAUSE;
		mSeekBar.setEnabled(false);
		mVideoView.pause();
		mHandler.removeMessages(MESSAGE_HIDE_PROGRESSBAR);
		mControlLayout.setVisibility(View.VISIBLE);
		mNoticeLayout.setVisibility(View.VISIBLE);
		mCenterButton.requestFocus();
		// if( getCurrentFocus().getId() != mSeekBar.getId()) {
		//
		// mSeekBar.requestFocus();
		// }
		//
		// Log.d(TAG,"FOUCED ID -->" + getCurrentFocus().getId());
		// mHandler.postDelayed(new Runnable() {
		//
		// @Override
		// public void run() {
		// // TODO Auto-generated method stub
		// mTopButton.requestFocus();
		// }
		// }, 200);
	}

	private void upDateFastTimeBar() {
		if (mTimeJumpSpeed > 0) {
			mFastIcon.setVisibility(View.VISIBLE);
			mFastIcon.setBackgroundResource(R.drawable.play_time_right);
			mFastTextView.setText("x" + Math.abs(mTimeJumpSpeed));
			mHandler.removeMessages(MESSAGE_HIDE_PROGRESSBAR);
			mNoticeLayout.setVisibility(View.VISIBLE);
		} else if (mTimeJumpSpeed < 0) {
			mFastIcon.setVisibility(View.VISIBLE);
			mFastIcon.setBackgroundResource(R.drawable.play_time_left);
			mFastTextView.setText("x" + Math.abs(mTimeJumpSpeed));
			mHandler.removeMessages(MESSAGE_HIDE_PROGRESSBAR);
			mNoticeLayout.setVisibility(View.VISIBLE);
		} else if (mTimeJumpSpeed == 0) {
			mFastIcon.setVisibility(View.GONE);
			mFastTextView.setText("");
			mHandler.removeMessages(MESSAGE_HIDE_PROGRESSBAR);
			mHandler.sendEmptyMessageDelayed(MESSAGE_HIDE_PROGRESSBAR, 2500);
		}
		// mHandler.removeMessages(MESSAGE_UPDATE_PROGRESS);
		// mHandler.sendEmptyMessageDelayed(MESSAGE_UPDATE_PROGRESS, 500);
	}

	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		// TODO Auto-generated method stub
		// 播放有问题 选下一个地址
		mHandler.sendEmptyMessage(MESSAGE_URL_NEXT);
		return true;
	}

	@Override
	public void onCompletion(MediaPlayer mp) {
		// TODO Auto-generated method stub
		// 播放完成
		autoPlayNext();
	}
	
	private void autoPlayNext(){
		switch (mProd_type) {
		case 1:
			finish();
			break;
		case 2:
		case 131:
			if(mEpisodeIndex<m_ReturnProgramView.tv.episodes.length-1){
				playNext();
			}else{
				
				finish();
			}
			break;
		case 3:
			if(mEpisodeIndex>0){
				playNext();
			}else{

				finish();
			}
			break;
		default:
			finish();
			break;
		}
	}

	@Override
	public void onPrepared(MediaPlayer mp) {
		// TODO Auto-generated method stub
		// 准备好了
		mTotalTimeTextView.setText(Utils.formatDuration(mVideoView
				.getDuration()));
		mSeekBar.setMax((int) mVideoView.getDuration());
		mSeekBar.setOnSeekBarChangeListener(VideoPlayerJPActivity.this);
		mSeekBar.setProgress((int) lastTime);
//		mHandler.sendEmptyMessageDelayed(MESSAGE_UPDATE_PROGRESS, SEEKBAR_REFRESH_TIME);
		startUpdateSeekBar(SEEKBAR_REFRESH_TIME);

	}

	@Override
	public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onSeekComplete(MediaPlayer mp) {
		// TODO Auto-generated method stub
		// 快进好了（拖动） 系统不支持？
	}

	@Override
	public boolean onInfo(MediaPlayer mp, int what, int extra) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onBufferingUpdate(MediaPlayer mp, int percent) {
		// TODO Auto-generated method stub
		// 缓冲进度
	}

	private void updateSeekBar() {
		switch (mStatue) {
		case STATUE_LOADING:
			long current = mVideoView.getCurrentPosition();// 当前进度
			long lastProgress = mSeekBar.getProgress();
//			Log.d(TAG, "loading --->" + current);
			// updateTimeNoticeView(mSeekBar.getProgress());
			if(current>lastProgress){
				hidePreLoad(); 
			}else{
				mSeekBar.setProgress((int) current);
				startUpdateSeekBar(SEEKBAR_REFRESH_TIME);
//				mHandler.sendEmptyMessageDelayed(MESSAGE_UPDATE_PROGRESS, SEEKBAR_REFRESH_TIME);
			}
			break;
		case STATUE_PLAYING:
			if(!isSeekBarIntoch){
				long current1 = mVideoView.getCurrentPosition();// 当前进度
				mSeekBar.setProgress((int) current1);
				// updateTimeNoticeView(mSeekBar.getProgress());
			}
//			mHandler.sendEmptyMessageDelayed(MESSAGE_UPDATE_PROGRESS, SEEKBAR_REFRESH_TIME);
			startUpdateSeekBar(SEEKBAR_REFRESH_TIME);
			break;
		case STATUE_FAST_DRAG:
			if (mTimeJumpSpeed > 0) {
				mFastJumpTime = (int) (mFastJumpTime + (mVideoView
						.getDuration() / 500));
			} else if (mTimeJumpSpeed < 0) {
				mFastJumpTime = (int) (mFastJumpTime - (mVideoView
						.getDuration() / 500));
			}

			if (mFastJumpTime > mVideoView.getDuration()) {
				mFastJumpTime = (int) mVideoView.getDuration();
			}
			if (mFastJumpTime < 0) {
				mFastJumpTime = 0;
			}
			mSeekBar.setProgress(mFastJumpTime);
			// updateTimeNoticeView(mSeekBar.getProgress());
//			mHandler.sendEmptyMessageDelayed(MESSAGE_UPDATE_PROGRESS,
//					mTimes[Math.abs(mTimeJumpSpeed) - 1]);
			startUpdateSeekBar(mTimes[Math.abs(mTimeJumpSpeed) - 1]);
			break;
		default:
			startUpdateSeekBar(SEEKBAR_REFRESH_TIME);
//			mHandler.sendEmptyMessageDelayed(MESSAGE_UPDATE_PROGRESS, SEEKBAR_REFRESH_TIME);
			break;
		}
	}

	private void updateTimeNoticeView(int progress) {
		RelativeLayout.LayoutParams parms = new RelativeLayout.LayoutParams(
				RelativeLayout.LayoutParams.WRAP_CONTENT,
				RelativeLayout.LayoutParams.WRAP_CONTENT);
		parms.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);

		double mLeft = (double) progress / mVideoView.getDuration()
				* (mSeekBar.getMeasuredWidth() - seekBarWidthOffset) + OFFSET;

		if (progress > 0)
			parms.leftMargin = (int) mLeft;
		else
			parms.leftMargin = OFFSET;
		parms.bottomMargin = 20 + 10;
		mTimeLayout.setLayoutParams(parms);

		mCurrentTimeTextView.setText(Utils.formatDuration(progress));
		mCurrentTimeTextView.setVisibility(View.VISIBLE);
	}

	private final Runnable mLoadingRunnable = new Runnable() {
		long beginTimeMillis, timeTakenMillis, m_bitrate;

		public void run() {

			// long txBytes = TrafficStats.getTotalTxBytes()- mStartTX;
			// TX.setText(Long.toString(txBytes));
			long rxBytes = TrafficStats.getTotalRxBytes() - mStartRX;

			timeTakenMillis = System.currentTimeMillis() - beginTimeMillis;
			beginTimeMillis = System.currentTimeMillis();
			if(timeTakenMillis!=0){
				m_bitrate = ((rxBytes - rxByteslast) * 8 * 1000 / timeTakenMillis) / 8000;
				rxByteslast = rxBytes;

				mSpeedTextView.setText("（" + Long.toString(m_bitrate) + "kb/s）");
				mLoadingPreparedPercent = mLoadingPreparedPercent + m_bitrate;
				if (mLoadingPreparedPercent >= 100
						&& mLoadingPreparedPercent / 100 < 100)
					mPercentTextView.setText(", 已完成"
							+ Long.toString(mLoadingPreparedPercent / 100) + "%");

				// Fun_downloadrate();
			}
			mHandler.postDelayed(mLoadingRunnable, 500);
		}
	};

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromUser) {
		// TODO Auto-generated method stub
		updateTimeNoticeView(mSeekBar.getProgress());
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub
		isSeekBarIntoch = true;
		mHandler.removeMessages(MESSAGE_HIDE_PROGRESSBAR);
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub
		isSeekBarIntoch = false;
		mVideoView.seekTo(mSeekBar.getProgress());
		mHandler.sendEmptyMessageDelayed(MESSAGE_HIDE_PROGRESSBAR, 2500);
//		mHandler.re
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch (v.getId()) {
		case R.id.ib_control_top:
//			mControlLayout.setVisibility(View.GONE);
			dismissView(mControlLayout);
			mHandler.sendEmptyMessageDelayed(MESSAGE_HIDE_PROGRESSBAR, 2500);
			mStatue = STATUE_PLAYING;
			mSeekBar.setEnabled(true);
			mVideoView.requestFocus();
			mVideoView.start();

			break;
		case R.id.btn_continue:
//			mContinueLayout.setVisibility(View.GONE);
			dismissView(mContinueLayout);
			mHandler.sendEmptyMessageDelayed(MESSAGE_HIDE_PROGRESSBAR, 2500);
			mStatue = STATUE_PLAYING;
			mSeekBar.setEnabled(true);
			mVideoView.requestFocus();
			mVideoView.start();
			
			break;
		case R.id.ib_control_center:
			// mVideoView.stopPlayback();
			finish();
			break;
		case R.id.ib_control_left:
			playPrevious();
			break;
		case R.id.ib_control_right:
			playNext();
			break;
		case R.id.ib_control_bottom:
			if (!isShoucang) {
				String url = Constant.BASE_URL + "program/favority";

				Map<String, Object> params = new HashMap<String, Object>();
				params.put("prod_id", mProd_id);

				AjaxCallback<JSONObject> cb = new AjaxCallback<JSONObject>();
				cb.SetHeader(app.getHeaders());

				cb.params(params).url(url).type(JSONObject.class)
						.weakHandler(this, "favorityResult");
				aq.ajax(cb);
			} else {// 取消收藏
				String url = Constant.BASE_URL + "program/unfavority";

				Map<String, Object> params = new HashMap<String, Object>();
				params.put("prod_id", mProd_id);

				AjaxCallback<JSONObject> cb = new AjaxCallback<JSONObject>();
				cb.SetHeader(app.getHeaders());

				cb.params(params).url(url).type(JSONObject.class)
						.weakHandler(this, "unfavorityResult");

				aq.ajax(cb);
			}
			break;
		default:
			break;
		}
	}

	private void showLoading() {
		
		mLoadingPreparedPercent = 0;
		rxByteslast = 0;
		mStartRX = TrafficStats.getTotalRxBytes();
		if (mStartRX == TrafficStats.UNSUPPORTED) {
			mSpeedTextView
					.setText("Your device does not support traffic stat monitoring.");
		} else {
			mHandler.postDelayed(mLoadingRunnable, 500);
		}
		mPercentTextView.setText("已完成0%");
		mPreLoadLayout.setVisibility(View.VISIBLE);
		mNoticeLayout.setVisibility(View.VISIBLE);
//		mHandler.sendEmptyMessageDelayed(MESSAGE_HIDE_PROGRESSBAR, 2500);
	}

	private void playNext() {
		// TODO Auto-generated method stub
		url_temp = null;
		mStatue = STATUE_LOADING;
		mSeekBar.setProgress(0);
		mSeekBar.setEnabled(false);
//		mTotalTimeTextView.setText(UtilTools.formatDuration(0));
		mTotalTimeTextView.setText("--:--");
		mHandler.removeCallbacksAndMessages(null);
		mControlLayout.setVisibility(View.GONE);
		lastTime = 0;
		mVideoView.stopPlayback();
		showLoading();
		if (mProd_type == 3) {
			mEpisodeIndex -= 1;
		} else {
			mEpisodeIndex += 1;
		}
		mHandler.sendEmptyMessage(MESSAGE_RETURN_DATE_OK);
	}

	private void playPrevious() {
		// TODO Auto-generated method stub
		url_temp = null;
		mStatue = STATUE_LOADING;
		mSeekBar.setProgress(0);
		mSeekBar.setEnabled(false);
//		mTotalTimeTextView.setText(UtilTools.formatDuration(0));
		mTotalTimeTextView.setText("--:--");
		mHandler.removeCallbacksAndMessages(null);
		mControlLayout.setVisibility(View.GONE);
		lastTime = 0;
		mVideoView.stopPlayback();
		showLoading();
		if (mProd_type == 3) {
			mEpisodeIndex += 1;
		} else {
			mEpisodeIndex -= 1;
		}
		mHandler.sendEmptyMessage(MESSAGE_RETURN_DATE_OK);
	}

	protected void getServiceData(String url, String interfaceName) {
		// TODO Auto-generated method stub

		AjaxCallback<JSONObject> cb = new AjaxCallback<JSONObject>();
		cb.url(url).type(JSONObject.class).weakHandler(this, interfaceName);

		cb.SetHeader(app.getHeaders());

		Log.d(TAG, url);
		Log.d(TAG, "header appkey" + app.getHeaders().get("app_key"));

		aq.ajax(cb);
	}

	private void getProgramViewDetailServiceData() {
		// TODO Auto-generated method stub

		String url = Constant.BASE_URL + "program/view" + "?prod_id="
				+ mProd_id;
		getServiceData(url, "initMovieDate");
	}

	public void initMovieDate(String url, JSONObject json, AjaxStatus status) {

		if (status.getCode() == AjaxStatus.NETWORK_ERROR || json == null) {
			Utils.showToast(aq.getContext(),
					getResources().getString(R.string.networknotwork));

			return;
		}

		if (json == null || json.equals(""))
			return;

		Log.d(TAG, "data = " + json.toString());
		ObjectMapper mapper = new ObjectMapper();
		try {
			m_ReturnProgramView = null;
			m_ReturnProgramView = mapper.readValue(json.toString(),
					ReturnProgramView.class);
			// 检测URL
			mHandler.sendEmptyMessage(MESSAGE_RETURN_DATE_OK);
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

	private void updateSourceAndTime() {
		Log.d(TAG, " ---- sre = " + mProd_src);
		if (mProd_src == null || mProd_src.length() == 1
				|| "null".equals(mProd_src)) {
			mResourceTextView.setText("");
		} else {
			String strSrc = "";
			if (mProd_src.equalsIgnoreCase("wangpan")) {
				strSrc = "PPTV";
			} else if (mProd_src.equalsIgnoreCase("le_tv_fee")) {
				strSrc = "乐  视";
			} else if (mProd_src.equalsIgnoreCase("letv")) {
				strSrc = "乐  视";
			} else if (mProd_src.equalsIgnoreCase("fengxing")) {
				strSrc = "风  行";
			} else if (mProd_src.equalsIgnoreCase("qiyi")) {
				strSrc = "爱  奇  艺";
			} else if (mProd_src.equalsIgnoreCase("youku")) {
				strSrc = "优  酷";
			} else if (mProd_src.equalsIgnoreCase("sinahd")) {
				strSrc = "新  浪  视  频";
			} else if (mProd_src.equalsIgnoreCase("sohu")) {
				strSrc = "搜  狐  视  频";
			} else if (mProd_src.equalsIgnoreCase("qq")) {
				strSrc = "腾  讯  视  频";
			} else if (mProd_src.equalsIgnoreCase("pptv")) {
				strSrc = "PPTV";
			} else if (mProd_src.equalsIgnoreCase("m1905")) {
				strSrc = "电  影  网";
			} else if(mProd_src.equalsIgnoreCase("XUNLEI")) {
				strSrc = "迅  雷";
			}else {
				strSrc = "PPTV";
			}
			mResourceTextView.setText(strSrc);
		}
		if(lastTime>0){
			mLastTimeTextView.setVisibility(View.VISIBLE);
			mLastTimeTextView.setText("上次播放: " + Utils.formatDuration(lastTime));
		}else{
			mLastTimeTextView.setVisibility(View.GONE);
		}
		if(playUrls.size()>0&&currentPlayIndex<=playUrls.size()-1){
			Log.d(TAG, "type---->" + playUrls.get(currentPlayIndex).defination_from_server);
			if(mProd_type == TYPE_PUSH||mProd_type == TYPE_XUNLEI){
				mDefinationIcon.setVisibility(View.VISIBLE);
				if("hd2".equalsIgnoreCase(playUrls.get(currentPlayIndex).defination_from_server)){
					mDefinationIcon.setImageResource(R.drawable.icon_def_hd2);
					mDefination = 8;
				}else if("hd".equalsIgnoreCase(playUrls.get(currentPlayIndex).defination_from_server)){
					mDefinationIcon.setImageResource(R.drawable.icon_def_hd);
					mDefination = 7;
				}else if("mp4".equalsIgnoreCase(playUrls.get(currentPlayIndex).defination_from_server)){
					mDefinationIcon.setImageResource(R.drawable.icon_def_mp4);
					mDefination = 6;
				}else{
//					mDefinationIcon.setVisibility(View.INVISIBLE);
					mDefinationIcon.setImageResource(R.drawable.icon_def_flv);
					mDefination = 5;
				}
			}else{
				mDefinationIcon.setVisibility(View.GONE);
//				if(Constant.player_quality_index[0].equalsIgnoreCase(playUrls.get(currentPlayIndex).defination_from_server)){
//					mDefinationIcon.setImageResource(R.drawable.player_1080p);
//				}else if(Constant.player_quality_index[1].equalsIgnoreCase(playUrls.get(currentPlayIndex).defination_from_server)){
//					mDefinationIcon.setImageResource(R.drawable.player_720p);
//				}else{
//					mDefinationIcon.setVisibility(View.INVISIBLE);
//				}
			}
		}
	}

	/**
	 * 把m_ReturnProgramView中数据转化成基本数据
	 * 
	 * @author Administrator
	 * 
	 */
	class PrepareTask implements Runnable {

		@Override
		public void run() {
			// TODO Auto-generated method stub
			playUrls.clear();
			switch (mProd_type) {
			case 1:
				
				if(m_ReturnProgramView.movie != null) {
					
					mProd_name = m_ReturnProgramView.movie.name;
					
					if(m_ReturnProgramView.movie.episodes != null 
							&& m_ReturnProgramView.movie.episodes.length > 0 
							&& m_ReturnProgramView.movie.episodes[0].down_urls != null) {
						
						for (int i = 0; i < m_ReturnProgramView.movie.episodes[0].down_urls.length; i++) {
							
							if(m_ReturnProgramView.movie.episodes[0].down_urls[i] != null) {
								
								String souces = m_ReturnProgramView.movie.episodes[0].down_urls[i].source;
								
								if(m_ReturnProgramView.movie.episodes[0].down_urls[i].urls != null) {
									
									for (int j = 0; j < m_ReturnProgramView.movie.episodes[0].down_urls[i].urls.length; j++) {
										
										if(m_ReturnProgramView.movie.episodes[0].down_urls[i].urls[j] != null) {
											
											URLS_INDEX url = new URLS_INDEX();
											url.source_from = souces;
											url.defination_from_server = m_ReturnProgramView.movie.episodes[0].down_urls[i].urls[j].type;
											url.url = m_ReturnProgramView.movie.episodes[0].down_urls[i].urls[j].url;
											playUrls.add(url);
										}

									}
								}
							}
						}
					}

				}
				
				break;
			case 2:
			case 131:
				
				if(m_ReturnProgramView.tv != null) {
					
					mProd_name = m_ReturnProgramView.tv.name;
					
					if(m_ReturnProgramView.tv.episodes != null) {
						
						if (mEpisodeIndex == -1) {
							for (int i = 0; i < m_ReturnProgramView.tv.episodes.length; i++) {
								if (m_ReturnProgramView.tv.episodes[i] != null 
										&& mProd_sub_name
										.equals(m_ReturnProgramView.tv.episodes[i].name)) {
									mEpisodeIndex = i;
									if(m_ReturnProgramView.tv.episodes[i].down_urls == null){
										mHandler.sendEmptyMessage(MESSAGE_URLS_READY);
										return; 
									}
									for (int j = 0; j < m_ReturnProgramView.tv.episodes[i].down_urls.length; j++) {
										
										if(m_ReturnProgramView.tv.episodes[i].down_urls[j] != null) {
											
											String souces = m_ReturnProgramView.tv.episodes[i].down_urls[j].source;
											
											if(m_ReturnProgramView.tv.episodes[i].down_urls[j].urls != null) {
												
												for (int k = 0; k < m_ReturnProgramView.tv.episodes[i].down_urls[j].urls.length; k++) {
													
													if(m_ReturnProgramView.tv.episodes[i].down_urls[j].urls[k] != null) {
														
														URLS_INDEX url = new URLS_INDEX();
														url.source_from = souces;
														url.defination_from_server = m_ReturnProgramView.tv.episodes[i].down_urls[j].urls[k].type;
														url.url = m_ReturnProgramView.tv.episodes[i].down_urls[j].urls[k].url;
														playUrls.add(url);
													}
												}
											}
										}

									}
								}
							}
						} else {
							
							if(m_ReturnProgramView.tv.episodes.length > mEpisodeIndex
									&& m_ReturnProgramView.tv.episodes[mEpisodeIndex] != null) {
								
								if(m_ReturnProgramView.tv.episodes[mEpisodeIndex].down_urls != null) {
									
									for (int j = 0; j < m_ReturnProgramView.tv.episodes[mEpisodeIndex].down_urls.length; j++) {
										
										if(m_ReturnProgramView.tv.episodes[mEpisodeIndex].down_urls[j] != null) {
											
											String souces = m_ReturnProgramView.tv.episodes[mEpisodeIndex].down_urls[j].source;
											
											if( m_ReturnProgramView.tv.episodes[mEpisodeIndex].down_urls[j].urls != null) {
												
												for (int k = 0; k < m_ReturnProgramView.tv.episodes[mEpisodeIndex].down_urls[j].urls.length; k++) {
													
													if(m_ReturnProgramView.tv.episodes[mEpisodeIndex].down_urls[j].urls[k] != null) {
														
														URLS_INDEX url = new URLS_INDEX();
														url.source_from = souces;
														url.defination_from_server = m_ReturnProgramView.tv.episodes[mEpisodeIndex].down_urls[j].urls[k].type;
														url.url = m_ReturnProgramView.tv.episodes[mEpisodeIndex].down_urls[j].urls[k].url;
														playUrls.add(url);
													}
												}
											}
										}
									}
								}
								mProd_sub_name = m_ReturnProgramView.tv.episodes[mEpisodeIndex].name;
							}
						}
					}
				}
				
				break;
			case 3:
				
				if(m_ReturnProgramView.show != null) {
					
					mProd_name = m_ReturnProgramView.show.name;
					
					if(m_ReturnProgramView.show.episodes != null) {
						
						if (mEpisodeIndex == -1) {
							for (int i = 0; i < m_ReturnProgramView.show.episodes.length; i++) {
								
								if(m_ReturnProgramView.show.episodes[i] != null) {
									
									if (Utils.isSame4Str(mProd_sub_name, m_ReturnProgramView.show.episodes[i].name)) {
										mEpisodeIndex = i;
										mProd_sub_name = m_ReturnProgramView.show.episodes[i].name;
										if(m_ReturnProgramView.show.episodes[i].down_urls==null){
											mHandler.sendEmptyMessage(MESSAGE_URLS_READY);
											return ;
										}
										for (int j = 0; j < m_ReturnProgramView.show.episodes[i].down_urls.length; j++) {
											
											
											if(m_ReturnProgramView.show.episodes[i].down_urls[j] != null) {
												
												String souces = m_ReturnProgramView.show.episodes[i].down_urls[j].source;
												
												if(m_ReturnProgramView.show.episodes[i].down_urls[j].urls != null) {
													
													for (int k = 0; k < m_ReturnProgramView.show.episodes[i].down_urls[j].urls.length; k++) {
														
														if(m_ReturnProgramView.show.episodes[i].down_urls[j].urls[k] != null) {
															
															URLS_INDEX url = new URLS_INDEX();
															url.source_from = souces;
															url.defination_from_server = m_ReturnProgramView.show.episodes[i].down_urls[j].urls[k].type;
															url.url = m_ReturnProgramView.show.episodes[i].down_urls[j].urls[k].url;
															playUrls.add(url);
														}
													}
												}
											}
										}
									}
								}
							}
						} else {
							
							if(m_ReturnProgramView.show.episodes.length > mEpisodeIndex ) {
								
								if(m_ReturnProgramView.show.episodes[mEpisodeIndex] != null 
										&& m_ReturnProgramView.show.episodes[mEpisodeIndex].down_urls != null) {
									
									for (int j = 0; j < m_ReturnProgramView.show.episodes[mEpisodeIndex].down_urls.length; j++) {
										
										if(m_ReturnProgramView.show.episodes[mEpisodeIndex].down_urls[j] != null) {
											
											String souces = m_ReturnProgramView.show.episodes[mEpisodeIndex].down_urls[j].source;
											
											if(m_ReturnProgramView.show.episodes[mEpisodeIndex].down_urls[j].urls != null) {
												
												for (int k = 0; k < m_ReturnProgramView.show.episodes[mEpisodeIndex].down_urls[j].urls.length; k++) {
													
													if(m_ReturnProgramView.show.episodes[mEpisodeIndex].down_urls[j].urls[k] != null) {
														
														URLS_INDEX url = new URLS_INDEX();
														url.source_from = souces;
														url.defination_from_server = m_ReturnProgramView.show.episodes[mEpisodeIndex].down_urls[j].urls[k].type;
														url.url = m_ReturnProgramView.show.episodes[mEpisodeIndex].down_urls[j].urls[k].url;
														playUrls.add(url);
													}
												}
											}
										}

									}
								}
								mProd_sub_name = m_ReturnProgramView.show.episodes[mEpisodeIndex].name;
							}
						}
					}
				}
				break;
			}
			Log.d(TAG, "playUrls size ------->" + playUrls.size());
			for (int i = 0; i < playUrls.size(); i++) {
				URLS_INDEX url_index = playUrls.get(i);
				if (url_index.source_from.trim().equalsIgnoreCase(
						Constant.video_index[0])) {
					url_index.souces = 0;
				} else if (url_index.source_from.trim().equalsIgnoreCase(
						Constant.video_index[1])) {
					url_index.souces = 1;
				} else if (url_index.source_from.trim().equalsIgnoreCase(
						Constant.video_index[2])) {
					url_index.souces = 2;
				} else if (url_index.source_from.trim().equalsIgnoreCase(
						Constant.video_index[3])) {
					url_index.souces = 3;
				} else if (url_index.source_from.trim().equalsIgnoreCase(
						Constant.video_index[4])) {
					url_index.souces = 4;
				} else if (url_index.source_from.trim().equalsIgnoreCase(
						Constant.video_index[5])) {
					url_index.souces = 5;
				} else if (url_index.source_from.trim().equalsIgnoreCase(
						Constant.video_index[6])) {
					url_index.souces = 6;
				} else if (url_index.source_from.trim().equalsIgnoreCase(
						Constant.video_index[7])) {
					url_index.souces = 7;
				} else if (url_index.source_from.trim().equalsIgnoreCase(
						Constant.video_index[8])) {
					url_index.souces = 8;
				} else if (url_index.source_from.trim().equalsIgnoreCase(
						Constant.video_index[9])) {
					url_index.souces = 9;
				} else if (url_index.source_from.trim().equalsIgnoreCase(
						Constant.video_index[10])) {
					url_index.souces = 10;
				} else if (url_index.source_from.trim().equalsIgnoreCase(
						Constant.video_index[11])) {
					url_index.souces = 11;
				} else {
					url_index.souces = 12;
				}
				switch (mDefination) {
				case BangDanConstant.GAOQING:// 高清
					if (url_index.defination_from_server.trim()
							.equalsIgnoreCase(Constant.player_quality_index[1])) {
						url_index.defination = 1;
					} else if (url_index.defination_from_server.trim()
							.equalsIgnoreCase(Constant.player_quality_index[0])) {
						url_index.defination = 2;
					} else if (url_index.defination_from_server.trim()
							.equalsIgnoreCase(Constant.player_quality_index[2])) {
						url_index.defination = 3;
					} else if (url_index.defination_from_server.trim()
							.equalsIgnoreCase(Constant.player_quality_index[3])) {
						url_index.defination = 4;
					} else {
						url_index.defination = 5;
					}
					break;
				case BangDanConstant.CHAOQING:// 超清
					if (url_index.defination_from_server.trim()
							.equalsIgnoreCase(Constant.player_quality_index[0])) {
						url_index.defination = 1;
					} else if (url_index.defination_from_server.trim()
							.equalsIgnoreCase(Constant.player_quality_index[1])) {
						url_index.defination = 2;
					} else if (url_index.defination_from_server.trim()
							.equalsIgnoreCase(Constant.player_quality_index[2])) {
						url_index.defination = 3;
					} else if (url_index.defination_from_server.trim()
							.equalsIgnoreCase(Constant.player_quality_index[3])) {
						url_index.defination = 4;
					} else {
						url_index.defination = 5;
					}
					break;
				case BangDanConstant.CHANGXIAN:// 标清
					if (url_index.defination_from_server.trim()
							.equalsIgnoreCase(Constant.player_quality_index[2])) {
						url_index.defination = 1;
					} else if (url_index.defination_from_server.trim()
							.equalsIgnoreCase(Constant.player_quality_index[3])) {
						url_index.defination = 2;
					} else if (url_index.defination_from_server.trim()
							.equalsIgnoreCase(Constant.player_quality_index[1])) {
						url_index.defination = 3;
					} else if (url_index.defination_from_server.trim()
							.equalsIgnoreCase(Constant.player_quality_index[0])) {
						url_index.defination = 4;
					} else {
						url_index.defination = 5;
					}
					break;
				}
				if (url_index.source_from
						.equalsIgnoreCase(Constant.BAIDU_WANGPAN)) {
					Document doc = null;
					try {
						doc = Jsoup.connect(url_index.url).timeout(10000).get();
						// doc = Jsoup.connect(htmlStr).timeout(10000).get();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					if (doc != null) {
						Element e = doc.getElementById("fileDownload");
						if (e != null) {
							Log.d(TAG, "url = " + e.attr("href"));
							if (e.attr("href") != null
									&& e.attr("href").length() > 0) {
								url_index.url = e.attr("href");
							}
						}
					}
				}
			}
			if (playUrls.size() > 1) {
				Collections.sort(playUrls, new SouceComparatorIndex1());
				Collections.sort(playUrls, new DefinationComparatorIndex());
			}
			// url list 准备完成
			mHandler.sendEmptyMessage(MESSAGE_URLS_READY);
		}
	}
	
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		MobclickAgent.onResume(this);
		super.onResume();
	}

	@Override
	protected void onPause() {
		
		Log.i(TAG, "onPause--->");
		
		// TODO Auto-generated method stub
		MobclickAgent.onPause(this);
//		if (mProd_type < 0&&mStatue!=STATUE_LOADING) {
			// SaveToServer(mVideoView.getDuration(),
			// mVideoView.getCurrentPosition());
			long duration = mVideoView.getDuration();
			long curretnPosition = mVideoView.getCurrentPosition();
			Log.d(TAG, "duration ->" + duration);
			Log.d(TAG, "curretnPosition ->" + curretnPosition);
			if(mProd_type == TYPE_PUSH||mProd_type == TYPE_LOCAL){
				if(duration-curretnPosition<10*1000&&duration>0){
					saveToDB(duration / 1000, (duration / 1000) -10);
				}else{
					saveToDB(duration / 1000, curretnPosition / 1000);
				}
			}
//		}
		super.onPause();
	}

	private void saveToDB(long duration, long playBackTime) {
		//save play date
		Log.d(TAG, "mProd_type---------------->" + mProd_type);
		play_info.setDuration((int) duration);
		play_info.setPlayback_time((int) playBackTime);
		if(mProd_type == TYPE_PUSH){
			play_info.setDefination(mDefination);
//			play_info.setDownload_url(currentPlayUrl);
		}else if(mProd_type == TYPE_LOCAL){
			play_info.setLocal_url(currentPlayUrl);
		}
		play_info.setCreat_time(System.currentTimeMillis());
//		MoviePlayHistoryInfo info = new MoviePlayHistoryInfo();
//		info.setDuration((int) duration);
//		info.setPlayback_time((int) playBackTime);
//		info.setName(mProd_name);
		DBServices services = DBServices.getInstance(this);
//		if(mProd_type == TYPE_PUSH){
//			info.setPlay_type(MoviePlayHistoryInfo.PLAY_TYPE_ONLINE);
//			info.setPush_url(currentPlayUrl);
////			services.insertMoviePlayHistory(info);
//		}else if(mProd_type == TYPE_LOCAL){
//			info.setPlay_type(MoviePlayHistoryInfo.PLAY_TYPE_LOCAL);
//			info.setLocal_url(currentPlayUrl);
////			services.insertMoviePlayHistory(info);
//		}
//		if(services.hasMoviePlayHistory(info)){
//			Log.d(TAG, "updateMoviePlayHistory");
			services.updateMoviePlayHistory(play_info);
//		}else{
//			Log.d(TAG, "insertMoviePlayHistory");
//			services.insertMoviePlayHistory(info);
//		}
	}
	
	@Override
	protected void onStop() {
		
		Log.i(TAG, "onStop--->");
		
		// TODO Auto-generated method stub
		// if(mStatue != STATUE_LOADING&&mProd_type>0){
		// // SaveToServer(mVideoView.getDuration(),
		// mVideoView.getCurrentPosition());
		// long duration = mVideoView.getDuration();
		// long curretnPosition = mVideoView.getCurrentPosition();
		// Log.d(TAG, "duration ->" + duration);
		// Log.d(TAG, "curretnPosition ->" + curretnPosition);
		// SaveToServer(duration/1000, curretnPosition/1000);
		// }
		if(!isFinishing()){
			finish();
		}
		super.onStop();
	}

//	public void saveToServer(long duration, long playBackTime) {
//		String url = Constant.BASE_URL + "program/play";
//
//		Map<String, Object> params = new HashMap<String, Object>();
//		params.put("app_key", Constant.APPKEY);// required string
//		params.put("prod_id", mProd_id);
//		params.put("prod_name", mProd_name);// required
//		params.put("prod_subname", mProd_sub_name);
//		params.put("prod_type", mProd_type);// required int 视频类别
//		params.put("play_type", "1");
//		params.put("playback_time", playBackTime);// _time required int
//		params.put("duration", duration);// required int 视频时长， 单位：秒
//		params.put("video_url", currentPlayUrl);// required
//		AjaxCallback<JSONObject> cb = new AjaxCallback<JSONObject>();
//		cb.SetHeader(app.getHeaders());
//		cb.params(params).url(url).type(JSONObject.class)
//				.weakHandler(this, "CallProgramPlayResult");
//		aq.ajax(cb);
//		
//	}

	@Override
	protected void onNewIntent(Intent intent) {
		// TODO Auto-generated method stub
		Log.d(TAG, "--------on new Intent--------------");
		super.onNewIntent(intent);
		mHandler.removeCallbacksAndMessages(null);
		m_ReturnProgramView = null;
		if (mVideoView!=null) { 
			mVideoView.stopPlayback();
			mVideoView.resume();
		}
		lastTime = 0;
		rxByteslast = 0;
		mLoadingPreparedPercent = 0;
		mEpisodeIndex = -1;
		mPercentTextView.setText(", 已完成"
				+ Long.toString(mLoadingPreparedPercent / 100) + "%");
		play_info = null;
		playUrls.clear();
		playUrls_flv.clear();
		playUrls_hd.clear();
		playUrls_hd2.clear();
		playUrls_mp4.clear();
		mSubTitleCollection = null;
		mSubTitleTv.setVisibility(View.VISIBLE);
		initVedioDate();
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		
		Log.i(TAG, "onDestroy--->");
		
		unregisterReceiver(mReceiver);
		if (mVideoView != null) {
			mVideoView.stopPlayback();
		}
		
		if(mPreLoadLayout.getBackground() != null) {
			
			Utils.recycleBitmap(((BitmapDrawable)mPreLoadLayout.getBackground()).getBitmap());
		}
		
		super.onDestroy();
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		// TODO Auto-generated method stub
		switch (id) {
		case 0:
			Dialog alertDialog = new AlertDialog.Builder(this). 
            setTitle("提示"). 
            setMessage("该视频无法播放"). 
            setPositiveButton("确定", new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					// TODO Auto-generated method stub
					finish();
				}

			}).
            create();
		 	alertDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
				
				@Override
				public void onCancel(DialogInterface dialog) {
					// TODO Auto-generated method stub
					finish();
				}
			});
		    alertDialog.show(); 
		    return alertDialog;
//		case 1:
//			Dialog dialog = new AlertDialog.Builder(VideoPlayerJPActivity.this).create();
//			dialog.show();
//			LayoutInflater inflater = LayoutInflater.from(VideoPlayerJPActivity.this);
//			View view = inflater.inflate(R.layout.video_choose_defination, null);
//			Button btn_hd2 = (Button) view.findViewById(R.id.btn_hd2);
//			Button btn_hd = (Button) view.findViewById(R.id.btn_hd);
//			Button btn_mp4 = (Button) view.findViewById(R.id.btn_mp4);
//			Button btn_flv = (Button) view.findViewById(R.id.btn_flv);
//			if(playUrls_hd2.size()<=0){
//				btn_hd2.setVisibility(View.GONE);
//			}
//			if(playUrls_hd.size()<=0){
//				btn_hd.setVisibility(View.GONE);
//			}
//			if(playUrls_mp4.size()<=0){
//				btn_mp4.setVisibility(View.GONE);
//			}
//			if(playUrls_flv.size()<=0){
//				btn_flv.setVisibility(View.GONE);
//			}
//			btn_hd2.setOnClickListener(new OnClickListener() {
//				
//				@Override
//				public void onClick(View v) {
//					// TODO Auto-generated method stub
//					changeDefination(Constant.DEFINATION_HD2);
//					removeDialog(1);
//				}
//			});
//			btn_hd.setOnClickListener(new OnClickListener() {
//				
//				@Override
//				public void onClick(View v) {
//					// TODO Auto-generated method stub
//					changeDefination(Constant.DEFINATION_HD);
//					removeDialog(1);
//				}
//			});
//			btn_mp4.setOnClickListener(new OnClickListener() {
//				
//				@Override
//				public void onClick(View v) {
//					// TODO Auto-generated method stub
//					changeDefination(Constant.DEFINATION_MP4);
//					removeDialog(1);
//				}
//			});
//			btn_flv.setOnClickListener(new OnClickListener() {
//				
//				@Override
//				public void onClick(View v) {
//					// TODO Auto-generated method stub
//					changeDefination(Constant.DEFINATION_FLV);
//					removeDialog(1);
//				}
//			});
//			dialog.setContentView(view);
//			return dialog;
		default:
			return super.onCreateDialog(id);
		}
	}
	
	/**
	 * 地址跳转
	 */
	
	class  UrlRedirectTask implements Runnable{

		@Override
		public void run() {
			
			// TODO Auto-generated method stub
			
			Log.i(TAG, "UrlRedirectTask-->" + currentPlayUrl);
			
			if(currentPlayUrl != null && !currentPlayUrl.equals("")) {
				
				if(currentPlayUrl.indexOf(("{now_date}")) != -1) {
					
					currentPlayUrl = currentPlayUrl.replace("{now_date}", System.currentTimeMillis()/1000 + "");
				}
			}
			
			String str = getRedirectUrl();
			
			if(str!=null){
				currentPlayUrl = str;
				mHandler.sendEmptyMessage(MESSAGE_PALY_URL_OK);
			}else{
				mHandler.sendEmptyMessage(MESSAGE_URL_NEXT);
			}
		}
		
	}
	
	private String getRedirectUrl(){
		String urlStr = null;
//		while(urlStr == null) {
			
		List<String> list = new ArrayList<String>();
		
		try {
			urlRedirect(currentPlayUrl,list);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			//超时异常
		}
		if(list.size() > 0) {
			 urlStr = list.get(list.size() -1);
		}
//		}
		return urlStr;
	}
	
	private void urlRedirect(String urlStr,List<String> list) {
		
		// 模拟火狐ios发用请求 使用userAgent
		AndroidHttpClient mAndroidHttpClient = AndroidHttpClient
				.newInstance(Constant.USER_AGENT_IOS);

		HttpParams httpParams = mAndroidHttpClient.getParams();
		// 连接时间最长5秒，可以更改
		HttpConnectionParams.setConnectionTimeout(httpParams, 5000 * 1);
		
		URL url;
		try {
			url = new URL(urlStr);
//			URI uri = new URI(url.getProtocol(), url.getHost(), url.getPath(), url.getQuery(),null);
//			HttpGet mHttpGet = new HttpGet(uri);
			HttpGet mHttpGet = new HttpGet(url.toURI());
			HttpResponse response = mAndroidHttpClient.execute(mHttpGet);
			StatusLine statusLine = response.getStatusLine();
			
			int status = statusLine.getStatusCode();
			Log.i(TAG, "HTTP STATUS : " + status);
			
			if (status == HttpStatus.SC_OK) {
				Log.i(TAG, "HttpStatus.SC_OK--->" + urlStr);
				// 正确的话直接返回，不进行下面的步骤
				mAndroidHttpClient.close();
				list.add(urlStr);
				
				return;//后面不执行
			} else {
				
				Log.i(TAG, "NOT HttpStatus.SC_OK--->" + urlStr);
				
				if (status == HttpStatus.SC_MOVED_PERMANENTLY || // 网址被永久移除
						status == HttpStatus.SC_MOVED_TEMPORARILY || // 网址暂时性移除
						status == HttpStatus.SC_SEE_OTHER || // 重新定位资源
						status == HttpStatus.SC_TEMPORARY_REDIRECT) {// 暂时定向
					
					Header header = response.getFirstHeader("Location");// 拿到重新定位后的header
					
					if(header != null) {
						
						String location = header.getValue();// 从header重新取出信息
						Log.i(TAG, "Location: " + location);
						if(location != null && !location.equals("")) {
							
							urlRedirect(location, list);
							
							mAndroidHttpClient.close();// 关闭此次连接
							return;//后面不执行
						}
					}
					
					list.add(null);
					mAndroidHttpClient.close();
					
					return;

				} else {//地址真的不存在
					
					mAndroidHttpClient.close();
					list.add(null);
					
					return;//后面不执行
				}
			}
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
	
	
	class RequestNewUrl implements Runnable{

		@Override
		public void run() {
			// TODO Auto-generated method stub
			isRequset = true;	
			playUrls.clear();
			//updateXunleiurl
			String url = Constant.BASE_URL + "/updateXunleiurl?url=" + play_info.getPush_url()
					+ "&id=" + play_info.getPush_id()
					+ "&md5_code=" + PreferencesUtils.getPincodeMd5(VideoPlayerJPActivity.this);
			String response = HttpTools.get(VideoPlayerJPActivity.this, url);
			Log.d(TAG, "response--->" + response);
			try {
				JSONObject json = new JSONObject(response);
				String reciveData = json.getString("downurl");
				if(play_info!=null){
					play_info.setRecivedDonwLoadUrls(reciveData);
				}
				String downLoadurls = DesUtils.decode(Constant.DES_KEY, reciveData);
				Log.d(TAG, "downLoadurls--->" + downLoadurls);
				String[] urls = downLoadurls.split("\\{mType\\}");
//				List<URLS_INDEX> list = new ArrayList<URLS_INDEX>();
				
//				playUrls_flv.clear();
//				playUrls_hd.clear();
//				playUrls_hd2.clear();
//				playUrls_mp4.clear();
				for(String str : urls){
					URLS_INDEX url_index_info = new URLS_INDEX();
					String[] p = str.split("\\{m\\}");
					if(p.length<2){
						continue;
					}
					url_index_info.defination_from_server = p[0];
					url_index_info.url = p[1];
//					if("hd2".equalsIgnoreCase(p[0])){
//						playUrls_hd2.add(url_index_info);
//					}else if("hd".equalsIgnoreCase(p[0])){
//						playUrls_hd.add(url_index_info);
//					}else if("mp4".equalsIgnoreCase(p[0])){
//						playUrls_mp4.add(url_index_info);
//					}else{
//						playUrls_flv.add(url_index_info);;
//					}
					playUrls.add(url_index_info);
				}
				initFourList();
				sortPushUrls(play_info.getDefination());
				mHandler.sendEmptyMessage(MESSAGE_URLS_READY);
//				currentPlayUrl = Utils.getUrl(downLoadurls);
//				currentPlayUrl = Utils.getRedirectUrl(currentPlayUrl);
//				mHandler.sendEmptyMessage(MESSAGE_PALY_URL_OK);
			} catch (Exception e) {
				// TODO: handle exception
//				mHandler.sendEmptyMessage(MESSAGE_URL_NEXT);
				e.printStackTrace();
				mHandler.sendEmptyMessage(MESSAGE_URLS_READY);
			}
		}
		
	}
	
	class getPlayList implements Runnable{

		@Override
		public void run() {
			// TODO Auto-generated method stub
			try{
				Log.d(TAG, "getPlayList----> start");
				if(play_info!=null){
					String data = DesUtils.decode(Constant.DES_KEY, play_info.getRecivedDonwLoadUrls());
					Log.d(TAG, "getPlayList--->data:" + data);
					String[] urls = data.split("\\{mType\\}");
//					List<URLS_INDEX> list = new ArrayList<URLS_INDEX>();
					playUrls.clear();
//					playUrls_flv.clear();
//					playUrls_hd.clear();
//					playUrls_hd2.clear();
//					playUrls_mp4.clear();
					for(String str : urls){
						URLS_INDEX url_index_info = new URLS_INDEX();
						String[] p = str.split("\\{m\\}");
						if(p.length<2){
							continue;
						}
						url_index_info.defination_from_server = p[0];
						url_index_info.url = p[1];
//						if("hd2".equalsIgnoreCase(p[0])){
//							playUrls_hd2.add(url_index_info);
//						}else if("hd".equalsIgnoreCase(p[0])){
//							playUrls_hd.add(url_index_info);
//						}else if("mp4".equalsIgnoreCase(p[0])){
//							playUrls_mp4.add(url_index_info);
//						}else{
//							playUrls_flv.add(url_index_info);;
//						}
						playUrls.add(url_index_info);
					}
					initFourList();
					sortPushUrls(mDefination);
					mHandler.sendEmptyMessage(MESSAGE_URLS_READY);
				}else{
					Log.d(TAG, "play_info----> = null");
					mHandler.sendEmptyMessage(MESSAGE_URLS_READY);
				}
			}catch (Exception e) {
				// TODO: handle exception
				e.printStackTrace();
				mHandler.sendEmptyMessage(MESSAGE_URLS_READY);
			}
		}
		
	}
	
	private void initFourList(){
		playUrls_flv.clear();
		playUrls_hd.clear();
		playUrls_hd2.clear();
		playUrls_mp4.clear();
		for(URLS_INDEX url_index_info:playUrls){
			if("hd2".equalsIgnoreCase(url_index_info.defination_from_server)){
				playUrls_hd2.add(url_index_info);
			}else if("hd".equalsIgnoreCase(url_index_info.defination_from_server)){
				playUrls_hd.add(url_index_info);
			}else if("mp4".equalsIgnoreCase(url_index_info.defination_from_server)){
				playUrls_mp4.add(url_index_info);
			}else{
				playUrls_flv.add(url_index_info);;
			}
		}
	}

	private void sortPushUrls(int defination){
		
		
		
		for(URLS_INDEX url_index_info:playUrls){
			switch (defination) {
			case Constant.DEFINATION_HD2:
				if("hd2".equalsIgnoreCase(url_index_info.defination_from_server)){
					url_index_info.defination = 0;
				}else if("hd".equalsIgnoreCase(url_index_info.defination_from_server)){
					url_index_info.defination = 1;
				}else if("mp4".equalsIgnoreCase(url_index_info.defination_from_server)){
					url_index_info.defination = 2;
				}else if("flv".equalsIgnoreCase(url_index_info.defination_from_server)){
					url_index_info.defination = 3;
				}else{
					url_index_info.defination = 4;
				}
				break;
			case Constant.DEFINATION_HD:
				if("hd2".equalsIgnoreCase(url_index_info.defination_from_server)){
					url_index_info.defination = 1;
				}else if("hd".equalsIgnoreCase(url_index_info.defination_from_server)){
					url_index_info.defination = 0;
				}else if("mp4".equalsIgnoreCase(url_index_info.defination_from_server)){
					url_index_info.defination = 2;
				}else if("flv".equalsIgnoreCase(url_index_info.defination_from_server)){
					url_index_info.defination = 3;
				}else{
					url_index_info.defination = 4;
				}
				break;
			case Constant.DEFINATION_MP4:
				if("hd2".equalsIgnoreCase(url_index_info.defination_from_server)){
					url_index_info.defination = 1;
				}else if("hd".equalsIgnoreCase(url_index_info.defination_from_server)){
					url_index_info.defination = 2;
				}else if("mp4".equalsIgnoreCase(url_index_info.defination_from_server)){
					url_index_info.defination = 0;
				}else if("flv".equalsIgnoreCase(url_index_info.defination_from_server)){
					url_index_info.defination = 3;
				}else{
					url_index_info.defination = 4;
				}
				break;
			case Constant.DEFINATION_FLV:
				if("hd2".equalsIgnoreCase(url_index_info.defination_from_server)){
					url_index_info.defination = 1;
				}else if("hd".equalsIgnoreCase(url_index_info.defination_from_server)){
					url_index_info.defination = 2;
				}else if("mp4".equalsIgnoreCase(url_index_info.defination_from_server)){
					url_index_info.defination = 3;
				}else if("flv".equalsIgnoreCase(url_index_info.defination_from_server)){
					url_index_info.defination = 0;
				}else{
					url_index_info.defination = 4;
				}
				break;
			default:
				break;
			}
		}
		if(playUrls.size()>1){
			Collections.sort(playUrls, new DefinationComparatorIndex());
		}
	}
	
	
	private void changeDefination(int defination){
		if(mDefination == defination){
			return ;
		}
		lastTime = mVideoView.getCurrentPosition();
		rxByteslast = 0;
		mLoadingPreparedPercent = 0;
		mEpisodeIndex = -1;
		mPercentTextView.setText(", 已完成"
				+ Long.toString(mLoadingPreparedPercent / 100) + "%");
		mDefination = defination;
		mVideoView.stopPlayback();
		mStatue = STATUE_LOADING;
		mSeekBar.setEnabled(false);
		mSeekBar.setProgress(0);
		mTotalTimeTextView.setText("--:--");
		mPreLoadLayout.setVisibility(View.VISIBLE);
		mNoticeLayout.setVisibility(View.VISIBLE);
		mContinueLayout.setVisibility(View.GONE);
		mControlLayout.setVisibility(View.GONE);
		mStartRX = TrafficStats.getTotalRxBytes();// 获取网络速度
		if (mStartRX == TrafficStats.UNSUPPORTED) {
			mSpeedTextView
					.setText("Your device does not support traffic stat monitoring.");
		} else {

			mHandler.postDelayed(mLoadingRunnable, 500);
		}
		sortPushUrls(mDefination);
		mHandler.sendEmptyMessage(MESSAGE_URLS_READY);
	}
	
	
	private void hidePreLoad(){
		Log.d(TAG, "hidePreLoad----------->");
		mPreLoadLayout.setVisibility(View.GONE);
		mHandler.removeCallbacks(mLoadingRunnable);
		mStatue = STATUE_PLAYING;
		mSeekBar.setEnabled(true);
		startUpdateSeekBar(SEEKBAR_REFRESH_TIME);
//		mHandler.sendEmptyMessageDelayed(MESSAGE_UPDATE_PROGRESS, SEEKBAR_REFRESH_TIME);
		mHandler.sendEmptyMessageDelayed(MESSAGE_HIDE_PROGRESSBAR, 5000);
	}
	
	class DefinationAdapter extends BaseAdapter{

		List<Integer> list;
		Context c;
		
		public DefinationAdapter(Context c, List<Integer> list){
			this.c = c;
			this.list = list;
		}
		
		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			return list.size();
		}

		@Override
		public Object getItem(int position) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public long getItemId(int position) {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			// TODO Auto-generated method stub
			TextView tv = new TextView(c);
			tv.setBackgroundResource(R.drawable.bg_choose_defination_selector);
			tv.setTextColor(Color.WHITE);
			tv.setTextSize(25);
			switch (list.get(position)) {
			case Constant.DEFINATION_HD2:
				tv.setText("超    清");
				break;
			case Constant.DEFINATION_HD:
				tv.setText("高    清");
				break;
			case Constant.DEFINATION_MP4:
				tv.setText("标    清");
				break;
			case Constant.DEFINATION_FLV:
				tv.setText("流    畅");
				break;
			}
			Gallery.LayoutParams param = new Gallery.LayoutParams(165, 40);
			tv.setGravity(Gravity.CENTER);
			tv.setLayoutParams(param);
			return tv;
		}

		
	}
	
	
	
	class ZimuAdapter extends BaseAdapter{

		List<Integer> list;
		Context c;
		
		public ZimuAdapter(Context c, List<Integer> list){
			this.c = c;
			this.list = list;
		}
		
		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			return list.size();
		}

		@Override
		public Object getItem(int position) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public long getItemId(int position) {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			// TODO Auto-generated method stub
			TextView tv = new TextView(c);
			tv.setTextColor(Color.WHITE);
			tv.setBackgroundResource(R.drawable.bg_choose_defination_selector);
			tv.setTextSize(25);
			switch (list.get(position)) {
			case -1://无字幕
				tv.setText("暂无字幕");
				break;
			case 0://字幕开
				tv.setText("开");
				break;
			case 1://字幕关
				tv.setText("关");
				break;
			}
			Gallery.LayoutParams param = new Gallery.LayoutParams(165, 40);
			tv.setGravity(Gravity.CENTER);
			tv.setLayoutParams(param);
			return tv;
		}

	}
	
	
}
