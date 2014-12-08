package com.android2ee.audioplayer.pojo;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import android.media.MediaPlayer;
import android.os.Parcel;
import android.os.Parcelable;

public class POJOAudio implements Parcelable, Comparable<POJOAudio> {

	
	private String path;
	private String name;
	private Date date;

	
	public POJOAudio(String path, String name, Date date) {
		super();
		this.path = path;
		this.name = name;
		this.date = date;

	}
	
	@Override
	public int compareTo(POJOAudio another) {
		return this.date.compareTo(another.date);
	}

	public POJOAudio(Parcel in) {
		super();
		this.path = in.readString();
		this.name = in.readString();
		this.date = (Date) in.readValue(Date.class.getClassLoader());
	}

	public String getPath() {
		return path;
	}

	public String getName() {
		return name;
	}

	public Date getDate() {
		return date;
	}

	
	@Override
	public String toString() {
		return name;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(path);
		dest.writeString(name);
		dest.writeValue(date);
	}
	
	public static final Parcelable.Creator<POJOAudio> CREATOR = new Parcelable.Creator<POJOAudio>()
	{
	    @Override
	    public POJOAudio createFromParcel(Parcel source)
	    {
	        return new POJOAudio(source);
	    }

	    @Override
	    public POJOAudio[] newArray(int size)
	    {
		return new POJOAudio[size];
	    }
	};

	

}
