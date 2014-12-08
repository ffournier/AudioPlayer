package com.android2ee.audioplayer;

import java.util.List;

import com.android2ee.audioplayer.service.MediaService;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class MyApplication extends Application {

	@Override
	public void onCreate() {
		super.onCreate();
		
		// if no activity launch we stop the service
		boolean result = isRunning(this);
		if (!result) {
			// we stop Service
			Intent intent = new Intent(this, MediaService.class);
			stopService(intent);
		}
	}
	
	/**
	 * On Lollippo pass now now by getAppTask instead, not need the permission GET TASK on 5.0 ;).
	 * @param ctx
	 * @return
	 */
	public boolean isRunning(Context ctx) {
		
		ActivityManager activityManager = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
	    List<RunningTaskInfo> tasks = activityManager.getRunningTasks(Integer.MAX_VALUE);
	
	    for (RunningTaskInfo task : tasks) {
	        if (ctx.getPackageName().equalsIgnoreCase(task.baseActivity.getPackageName())) 
	            return true;                                  
	    }
	    return false;
	}

}
