package com.joyplus.tvhelper.entity;

public class MoviePlayHistoryInfo {

	public static final int PLAY_TYPE_ONLINE = 0;
	public static final int PLAY_TYPE_LOCAL = PLAY_TYPE_ONLINE + 1;
	
	public static final int EDITE_STATUE_NOMAL 			= 0;
	public static final int EDITE_STATUE_EDIT 			= EDITE_STATUE_NOMAL + 1;
	public static final int EDITE_STATUE_SELETED 		= EDITE_STATUE_EDIT + 1;
	
	private int id;
	private String name;
	private int play_type;
	private String push_url;
	private String local_url;
	private int duration;
	private int playback_time;
	private int push_id;
	private int edite_state = 0;
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getPush_url() {
		return push_url;
	}
	public void setPush_url(String push_url) {
		this.push_url = push_url;
	}
	public String getLocal_url() {
		return local_url;
	}
	public void setLocal_url(String local_url) {
		this.local_url = local_url;
	}
	public int getDuration() {
		return duration;
	}
	public void setDuration(int duration) {
		this.duration = duration;
	}
	public int getPlayback_time() {
		return playback_time;
	}
	public void setPlayback_time(int playback_time) {
		this.playback_time = playback_time;
	}
	public int getPush_id() {
		return push_id;
	}
	public void setPush_id(int push_id) {
		this.push_id = push_id;
	}
	public int getPlay_type() {
		return play_type;
	}
	public void setPlay_type(int play_type) {
		this.play_type = play_type;
	}
	public int getEdite_state() {
		return edite_state;
	}
	public void setEdite_state(int edite_state) {
		this.edite_state = edite_state;
	}
	
}