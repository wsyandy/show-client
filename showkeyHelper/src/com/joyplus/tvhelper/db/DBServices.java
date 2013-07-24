package com.joyplus.tvhelper.db;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.joyplus.network.filedownload.manager.DownloadManager;
import com.joyplus.tvhelper.entity.ApkInfo;
import com.joyplus.tvhelper.entity.MoviePlayHistoryInfo;
import com.joyplus.tvhelper.entity.PushedApkDownLoadInfo;
import com.joyplus.tvhelper.entity.PushedMovieDownLoadInfo;
import com.joyplus.tvhelper.utils.PackageUtils;
import com.joyplus.utils.Log;


public class DBServices {
	
	private static final String TAG = "DBServices";
	
	private static DBServices dao = null;
	private Context context;
	private DownloadManager dmg;

	private DBServices(Context context) {
		this.context = context;
		dmg = DownloadManager.getInstance(context);
	}

	public static DBServices getInstance(Context context) {
		if (dao == null) {
			dao = new DBServices(context);
		}
		return dao;
	}

	public SQLiteDatabase getConnection() {
		SQLiteDatabase sqliteDatabase = null;
		try {
			sqliteDatabase = new DBHelper(context).getWritableDatabase();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return sqliteDatabase;
	}
	
	
	public synchronized long insertApkInfo(PushedApkDownLoadInfo info){
		SQLiteDatabase db = getConnection();
		ContentValues values = new ContentValues();
		values.put(DBConstant.KEY_APK_INFO_NAME, info.getName());
		values.put(DBConstant.KEY_APK_INFO_PUSH_ID, info.getPush_id());
		values.put(DBConstant.KEY_APK_INFO_FILE_PATH, info.getFile_path());
		values.put(DBConstant.KEY_APK_INFO_DOWNLOAD_STATE, info.getDownload_state());
		values.put(DBConstant.KEY_APK_INFO_DOWNLOADUUID, info.getTast().getUUId());
		values.put(DBConstant.KEY_APK_INFO_ISUSER, info.getIsUser());
		values.put(DBConstant.KEY_SYN_C1, info.getIcon_url());
		long _id = db.insert(DBConstant.TABLE_APK_INFO, null, values);
		db.close();
        return _id;
	}
	
	public synchronized void updateApkInfo(PushedApkDownLoadInfo info){
        SQLiteDatabase db = getConnection();
        ContentValues values = new ContentValues();
		values.put(DBConstant.KEY_APK_INFO_NAME, info.getName());
		values.put(DBConstant.KEY_APK_INFO_PUSH_ID, info.getPush_id());
		values.put(DBConstant.KEY_APK_INFO_FILE_PATH, info.getFile_path());
		values.put(DBConstant.KEY_APK_INFO_DOWNLOAD_STATE, info.getDownload_state());
		values.put(DBConstant.KEY_APK_INFO_DOWNLOADUUID, info.getTast().getUUId());
		values.put(DBConstant.KEY_APK_INFO_ISUSER, info.getIsUser());
		values.put(DBConstant.KEY_SYN_C1, info.getIcon_url());
//
        int rows = db.update(DBConstant.TABLE_APK_INFO, values,
        		DBConstant.KEY_ID + " = ? ", new String[] {
        		info.get_id()+ ""
                });
        db.close();
        Log.d(TAG, rows + "--->update");
    }
	
	public synchronized void deleteApkInfo(PushedApkDownLoadInfo info) {
        SQLiteDatabase db = getConnection();
        int rows = db.delete(DBConstant.TABLE_APK_INFO,
        		DBConstant.KEY_ID + " = ? ", new String[] {
        		String.valueOf(info.get_id())
                });
        Log.i(TAG, rows + "rows deleted");
        db.close();
    }
	
	public synchronized ArrayList<PushedApkDownLoadInfo> queryUserApkDownLoadInfo() {
        SQLiteDatabase db = getConnection();
        Cursor cr = db.query(DBConstant.TABLE_APK_INFO, null,
        		DBConstant.KEY_APK_INFO_ISUSER + " = ? ", new String[] {
        		PushedApkDownLoadInfo.IS_USER + ""}, null, null, null);
        ArrayList<PushedApkDownLoadInfo> taskes = new ArrayList<PushedApkDownLoadInfo>();
        PushedApkDownLoadInfo info;
        while (cr.moveToNext()) {
        	
        	int _id = cr.getInt(cr.getColumnIndex(DBConstant.KEY_ID));
        	String name = cr.getString(cr.getColumnIndex(DBConstant.KEY_APK_INFO_NAME));
        	Log.d(TAG, "PushedApkDownLoadInfo----------->" + name);
        	int push_id = cr.getInt(cr.getColumnIndex(DBConstant.KEY_APK_INFO_PUSH_ID));
        	String file_path = cr.getString(cr.getColumnIndex(DBConstant.KEY_APK_INFO_FILE_PATH));
        	int download_statue = cr.getInt(cr.getColumnIndex(DBConstant.KEY_APK_INFO_DOWNLOAD_STATE));
        	String download_uuid = cr.getString(cr.getColumnIndex(DBConstant.KEY_APK_INFO_DOWNLOADUUID));
        	String icon_url = cr.getString(cr.getColumnIndex(DBConstant.KEY_SYN_C1));
        	
        	info = new PushedApkDownLoadInfo();
        	info.setIcon_url(icon_url);
        	info.set_id(_id);
        	info.setName(name);
        	info.setPush_id(push_id);
        	if(download_statue == PushedApkDownLoadInfo.STATUE_DOWNLOADING){
        		download_statue = PushedApkDownLoadInfo.STATUE_DOWNLOAD_PAUSE;
        	}
        	if(download_statue == PushedApkDownLoadInfo.STATUE_DOWNLOAD_COMPLETE||download_statue == PushedApkDownLoadInfo.STATUE_INSTALL_FAILE){
        		ApkInfo apkinfo = PackageUtils.getUnInstalledApkInfo(context, file_path);
        		if(apkinfo!=null){
        			info.setPackageName(apkinfo.getPackageName());
            		info.setIcon(apkinfo.getDrawble());
        		}
        	}
        	info.setDownload_state(download_statue);
        	info.setFile_path(file_path);
        	info.setIsUser(PushedApkDownLoadInfo.IS_USER);
        	info.setTast(dmg.findTaksByUUID(download_uuid));
        	
        	taskes.add(info);
        	
        }
        cr.close();
        db.close();
        return taskes;
    }
	
	public synchronized ArrayList<PushedApkDownLoadInfo> queryNotUserApkDownLoadInfo() {
        SQLiteDatabase db = getConnection();
        Cursor cr = db.query(DBConstant.TABLE_APK_INFO, null,
        		DBConstant.KEY_APK_INFO_ISUSER + " = ? ", new String[] {
        		PushedApkDownLoadInfo.IS_NOT_USER + ""}, null, null, null);
        ArrayList<PushedApkDownLoadInfo> taskes = new ArrayList<PushedApkDownLoadInfo>();
        PushedApkDownLoadInfo info;
        while (cr.moveToNext()) {
        	int _id = cr.getInt(cr.getColumnIndex(DBConstant.KEY_ID));
        	String name = cr.getString(cr.getColumnIndex(DBConstant.KEY_APK_INFO_NAME));
        	Log.d(TAG, "not user PushedApkDownLoadInfo----------->" + name);
        	int push_id = cr.getInt(cr.getColumnIndex(DBConstant.KEY_APK_INFO_PUSH_ID));
        	String file_path = cr.getString(cr.getColumnIndex(DBConstant.KEY_APK_INFO_FILE_PATH));
        	int download_statue = cr.getInt(cr.getColumnIndex(DBConstant.KEY_APK_INFO_DOWNLOAD_STATE));
        	String download_uuid = cr.getString(cr.getColumnIndex(DBConstant.KEY_APK_INFO_DOWNLOADUUID));
        	
        	
        	info = new PushedApkDownLoadInfo();
        	info.set_id(_id);
        	info.setName(name);
        	info.setPush_id(push_id);
        	info.setDownload_state(download_statue);
        	info.setFile_path(file_path);
        	info.setIsUser(PushedApkDownLoadInfo.IS_NOT_USER);
        	info.setTast(dmg.findTaksByUUID(download_uuid));
        	if(download_statue == PushedApkDownLoadInfo.STATUE_DOWNLOAD_COMPLETE||download_statue == PushedApkDownLoadInfo.STATUE_INSTALL_FAILE){
        		ApkInfo apkinfo = PackageUtils.getUnInstalledApkInfo(context, file_path);
        		if(info!=null){
        			info.setPackageName(apkinfo.getPackageName());
            		info.setIcon(apkinfo.getDrawble());
        		}
        	}
        	taskes.add(info);
        }
        cr.close();
        db.close();
        return taskes;
    }
	
	public synchronized long insertMovieDownLoadInfo(PushedMovieDownLoadInfo info){
		SQLiteDatabase db = getConnection();
		ContentValues values = new ContentValues();
		values.put(DBConstant.KEY_MOVIE_DOWNLOAD_INFO_NAME, info.getName());
		values.put(DBConstant.KEY_MOVIE_DOWNLOAD_INFO_PUSH_ID, info.getPush_id());
		values.put(DBConstant.KEY_MOVIE_DOWNLOAD_INFO_FILE_PATH, info.getFile_path());
		values.put(DBConstant.KEY_MOVIE_DOWNLOAD_INFO_DOWNLOAD_STATE, info.getDownload_state());
		values.put(DBConstant.KEY_MOVIE_DOWNLOAD_INFO_PUSH_URL, info.getPush_url());
		values.put(DBConstant.KEY_MOVIE_DOWNLOAD_INFO_DOWNLOADUUID, info.getTast().getUUId());
		long _id = db.insert(DBConstant.TABLE_MOVIE_INFO, null, values);
		db.close();
        return _id;
	}
	
	public synchronized void deleteMovieDownLoadInfo(PushedMovieDownLoadInfo info) {
		SQLiteDatabase db = getConnection();
		int rows = db.delete(DBConstant.TABLE_MOVIE_INFO,
				DBConstant.KEY_ID + " = ? ", new String[] {
				String.valueOf(info.get_id())
		});
		Log.i(TAG, rows + "rows deleted");
		db.close();
	}
	
	public synchronized void updateMovieDownLoadInfo(PushedMovieDownLoadInfo info){
        SQLiteDatabase db = getConnection();
        ContentValues values = new ContentValues();
        values.put(DBConstant.KEY_MOVIE_DOWNLOAD_INFO_NAME, info.getName());
		values.put(DBConstant.KEY_MOVIE_DOWNLOAD_INFO_PUSH_ID, info.getPush_id());
		values.put(DBConstant.KEY_MOVIE_DOWNLOAD_INFO_FILE_PATH, info.getFile_path());
		values.put(DBConstant.KEY_MOVIE_DOWNLOAD_INFO_DOWNLOAD_STATE, info.getDownload_state());
		values.put(DBConstant.KEY_MOVIE_DOWNLOAD_INFO_PUSH_URL, info.getPush_url());
		values.put(DBConstant.KEY_MOVIE_DOWNLOAD_INFO_DOWNLOADUUID, info.getTast().getUUId());
//
        int rows = db.update(DBConstant.TABLE_MOVIE_INFO, values,
        		DBConstant.KEY_ID + " = ? ", new String[] {
        		info.get_id()+ ""
                });
        db.close();
        Log.d(TAG, rows + "--->update");
    }
	
	
	public synchronized ArrayList<PushedMovieDownLoadInfo> queryMovieDownLoadInfos() {
        SQLiteDatabase db = getConnection();
        Cursor cr = db.query(DBConstant.TABLE_MOVIE_INFO, null,
        		DBConstant.KEY_MOVIE_DOWNLOAD_INFO_DOWNLOAD_STATE + " <? ", new String[] {
        		PushedMovieDownLoadInfo.STATUE_DOWNLOAD_COMPLETE + ""}, null, null, null);
        ArrayList<PushedMovieDownLoadInfo> taskes = new ArrayList<PushedMovieDownLoadInfo>();
        PushedMovieDownLoadInfo info;
        while (cr.moveToNext()) {
        	
        	int _id = cr.getInt(cr.getColumnIndex(DBConstant.KEY_ID));
        	String name = cr.getString(cr.getColumnIndex(DBConstant.KEY_MOVIE_DOWNLOAD_INFO_NAME));
        	int push_id = cr.getInt(cr.getColumnIndex(DBConstant.KEY_MOVIE_DOWNLOAD_INFO_PUSH_ID));
        	String file_path = cr.getString(cr.getColumnIndex(DBConstant.KEY_MOVIE_DOWNLOAD_INFO_FILE_PATH));
        	int download_statue = cr.getInt(cr.getColumnIndex(DBConstant.KEY_MOVIE_DOWNLOAD_INFO_DOWNLOAD_STATE));
        	String download_uuid = cr.getString(cr.getColumnIndex(DBConstant.KEY_MOVIE_DOWNLOAD_INFO_DOWNLOADUUID));
        	String push_url = cr.getString(cr.getColumnIndex(DBConstant.KEY_MOVIE_DOWNLOAD_INFO_PUSH_URL));
        	Log.d(TAG, "PushedMovieDownLoadInfo---url-------->" + push_url);
        	
        	info = new PushedMovieDownLoadInfo();
        	info.set_id(_id);
        	info.setName(name);
        	info.setPush_id(push_id);
        	download_statue = PushedApkDownLoadInfo.STATUE_DOWNLOAD_PAUSE;
        	info.setDownload_state(download_statue);
        	info.setFile_path(file_path);
        	info.setPush_url(push_url);
        	info.setTast(dmg.findTaksByUUID(download_uuid));
        	taskes.add(info);
        }
        cr.close();
        db.close();
        return taskes;
    }
	
	public synchronized ArrayList<PushedMovieDownLoadInfo> queryMovieDownLoadedInfos() {
        SQLiteDatabase db = getConnection();
        Cursor cr = db.query(DBConstant.TABLE_MOVIE_INFO, null,
        		DBConstant.KEY_MOVIE_DOWNLOAD_INFO_DOWNLOAD_STATE + " =? ", new String[] {
        		PushedMovieDownLoadInfo.STATUE_DOWNLOAD_COMPLETE + ""}, null, null, null);
        ArrayList<PushedMovieDownLoadInfo> taskes = new ArrayList<PushedMovieDownLoadInfo>();
        PushedMovieDownLoadInfo info;
        while (cr.moveToNext()) {
        	
        	int _id = cr.getInt(cr.getColumnIndex(DBConstant.KEY_ID));
        	String name = cr.getString(cr.getColumnIndex(DBConstant.KEY_MOVIE_DOWNLOAD_INFO_NAME));
        	int push_id = cr.getInt(cr.getColumnIndex(DBConstant.KEY_MOVIE_DOWNLOAD_INFO_PUSH_ID));
        	String file_path = cr.getString(cr.getColumnIndex(DBConstant.KEY_MOVIE_DOWNLOAD_INFO_FILE_PATH));
        	int download_statue = cr.getInt(cr.getColumnIndex(DBConstant.KEY_MOVIE_DOWNLOAD_INFO_DOWNLOAD_STATE));
        	String download_uuid = cr.getString(cr.getColumnIndex(DBConstant.KEY_MOVIE_DOWNLOAD_INFO_DOWNLOADUUID));
        	String push_url = cr.getString(cr.getColumnIndex(DBConstant.KEY_MOVIE_DOWNLOAD_INFO_PUSH_URL));
        	
        	info = new PushedMovieDownLoadInfo();
        	info.set_id(_id);
        	info.setName(name);
        	info.setPush_id(push_id);
        	info.setDownload_state(download_statue);
        	info.setFile_path(file_path);
        	info.setPush_url(push_url);
        	info.setTast(dmg.findTaksByUUID(download_uuid));
        	taskes.add(info);
        }
        cr.close();
        db.close();
        return taskes;
    }

	public synchronized long insertMoviePlayHistory(MoviePlayHistoryInfo info){
		SQLiteDatabase db = getConnection();
		ContentValues values = new ContentValues();
		values.put(DBConstant.KEY_PLAY_INFO_NAME, info.getName());
		values.put(DBConstant.KEY_PLAY_INFO_PUSH_ID, info.getPush_id());
		values.put(DBConstant.KEY_PLAY_INFO_FILE_PATH, info.getLocal_url());
		values.put(DBConstant.KEY_PLAY_INFO_PUSH_URL, info.getPush_url());
		values.put(DBConstant.KEY_PLAY_INFO_TYPE, info.getPlay_type());
		values.put(DBConstant.KEY_PLAY_INFO_PLAY_BACK_TIME, info.getPlayback_time());
		values.put(DBConstant.KEY_SYN1, info.getDuration());
		long _id = db.insert(DBConstant.TABLE_PLAY_INFO, null, values);
		db.close();
        return _id;
	}
	
	public synchronized void updateMoviePlayHistory(MoviePlayHistoryInfo info){
        SQLiteDatabase db = getConnection();
        ContentValues values = new ContentValues();
		values.put(DBConstant.KEY_PLAY_INFO_NAME, info.getName());
		values.put(DBConstant.KEY_PLAY_INFO_PUSH_ID, info.getPush_id());
		values.put(DBConstant.KEY_PLAY_INFO_FILE_PATH, info.getLocal_url());
		values.put(DBConstant.KEY_PLAY_INFO_PUSH_URL, info.getPush_url());
		values.put(DBConstant.KEY_PLAY_INFO_TYPE, info.getPlay_type());
		values.put(DBConstant.KEY_PLAY_INFO_PLAY_BACK_TIME, info.getPlayback_time());
		values.put(DBConstant.KEY_SYN1, info.getDuration());
//
        int rows = db.update(DBConstant.TABLE_PLAY_INFO, values,
        		DBConstant.KEY_ID + " = ? ", new String[] {
        		info.getId()+ ""
                });
        db.close();
        Log.d(TAG, rows + "--->update");
    }
	
	public synchronized void deleteMoviePlayHistory(MoviePlayHistoryInfo info){
		SQLiteDatabase db = getConnection();
		int rows = db.delete(DBConstant.TABLE_PLAY_INFO,
				DBConstant.KEY_ID + " = ? ", new String[] {
				String.valueOf(info.getId())
		});
		Log.i(TAG, rows + "rows deleted");
		db.close();
	}
	
	public synchronized List<MoviePlayHistoryInfo> queryMoviePlayHistoryList(){
		SQLiteDatabase db = getConnection();
        Cursor cr = db.query(DBConstant.TABLE_PLAY_INFO, null,
        		null, null, null, null, null);
        ArrayList<MoviePlayHistoryInfo> taskes = new ArrayList<MoviePlayHistoryInfo>();
        
        MoviePlayHistoryInfo info;
        while (cr.moveToNext()) {
    		info = new MoviePlayHistoryInfo();
    		
    		info.setId(cr.getInt(cr.getColumnIndex(DBConstant.KEY_ID)));
    		info.setPlay_type(cr.getInt(cr.getColumnIndex(DBConstant.KEY_PLAY_INFO_TYPE)));
    		info.setPush_id(cr.getInt(cr.getColumnIndex(DBConstant.KEY_PLAY_INFO_PUSH_ID)));
    		info.setPlayback_time(cr.getInt(cr.getColumnIndex(DBConstant.KEY_PLAY_INFO_PLAY_BACK_TIME)));
    		info.setDuration(cr.getInt(cr.getColumnIndex(DBConstant.KEY_SYN1)));
    		info.setName(cr.getString(cr.getColumnIndex(DBConstant.KEY_PLAY_INFO_NAME)));
    		info.setPush_url(cr.getString(cr.getColumnIndex(DBConstant.KEY_PLAY_INFO_PUSH_URL)));
    		info.setLocal_url(cr.getString(cr.getColumnIndex(DBConstant.KEY_PLAY_INFO_FILE_PATH)));
    		
        	taskes.add(info);
        }
        cr.close();
        db.close();
        return taskes;
	}
	
//	public synchronized boolean hasMoviePlayHistory(MoviePlayHistoryInfo info){
//		SQLiteDatabase db = getConnection();
//		Cursor cr = db.query(DBConstant.TABLE_PLAY_INFO, null,
//				DBConstant.KEY_ + " =? ", new String[] {
//        		PushedMovieDownLoadInfo.STATUE_DOWNLOAD_COMPLETE + ""}, null, null, null);
//	}
}