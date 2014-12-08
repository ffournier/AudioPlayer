package com.android2ee.audioplayer.activity;


import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;

import com.android2ee.audioplayer.service.MediaService;
import com.android2ee.audioplayer.service.MediaService.LocalBinder;

public abstract class ABoundActivity extends ActionBarActivity {
	
	protected boolean mBound = false;
	protected MediaService mService;
	
	 /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            LocalBinder binder = (LocalBinder) service;
            mService = binder.getService();
            mBound = true;
            serviceConnected(mService);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
            mService = null;
        }
    };
    
    protected abstract void serviceConnected(MediaService service);
    
    protected abstract void serviceDisconnected(MediaService service);
	
	@Override
	protected void onStart() {
		super.onStart();
		Intent intent = new Intent(this, MediaService.class);
        bindService(intent, mConnection, BIND_AUTO_CREATE);
	}
	
	
	@Override
	protected void onStop() {
		super.onStop();
		// Unbind from the service
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
        
    }

}
