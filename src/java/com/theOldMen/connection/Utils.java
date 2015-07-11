package com.theOldMen.connection;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

public class Utils extends Application{
	private static Utils instance ;
	
	public static Utils getInstance(){
		return instance;
	}
	
	@Override
	public void onCreate(){
		super.onCreate();
		instance = this;
	}

	String getSharedPreferences(String root , String data){
		SharedPreferences sharedPreference= instance.getSharedPreferences("Category", Context.MODE_APPEND);
		return sharedPreference.getString(root,data);
	}


}
