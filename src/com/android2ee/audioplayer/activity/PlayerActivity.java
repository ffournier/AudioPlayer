package com.android2ee.audioplayer.activity;


import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;

import com.android2ee.audioplayer.R;
import com.android2ee.audioplayer.pojo.POJOAudio;
import com.android2ee.audioplayer.service.MediaService;
import com.android2ee.audioplayer.view.VisualizerFFTView;
import com.android2ee.audioplayer.view.VisualizerView;

public class PlayerActivity extends ABoundActivity {
	
	public static final String KEY_AUDIO_PLAY = "com.android2ee.audioplayer.audio_player";
	
	public static final String KEY_AUDIO_PLAYING = "com.android2ee.audioplayer.audio_playing";
	public static final String KEY_AUDIO_DURATION = "com.android2ee.audioplayer.audio_duration";
	public static final String KEY_AUDIO_CURRENT = "com.android2ee.audioplayer.audio_current";
	
	private POJOAudio audio = null;
	private ImageButton play;
	
	private boolean isPlayingBefore = false;
	
    private VisualizerView myView;
    private VisualizerFFTView myViewFFT;
	
    private class MyHandler extends Handler {
		
		@Override
	    public void handleMessage(Message message) {
			switch (message.what) {
			case MediaService.PLAY_START:
				myView.startVisualizer();
				updateResource(true);
				break;
			case MediaService.PLAY_END:
				myView.endVisualizer();
				updateResource(false);
			break;
	        case MediaService.PLAY_FFT:
	        	byte[] fft = message.getData().getByteArray(MediaService.KEY_PLAY_FFT);
	        	int rateFFT = message.getData().getInt(MediaService.KEY_PLAY_RATE);
        		addDataFFT(fft, rateFFT);
	            break;    
	         case MediaService.PLAY_WAVEFROM:
	        	byte[] waveFrom = message.getData().getByteArray(MediaService.KEY_PLAY_WAVEFROM);
	        	int rateWaveFrom = message.getData().getInt(MediaService.KEY_PLAY_RATE);
	    		addDataWaveFrom(waveFrom, rateWaveFrom);
	            break;    
	        }
		}
	}
	
	private Handler myHandler = new MyHandler();
	
	@Override
	protected void serviceConnected(MediaService service) {
		mService.setHandler(myHandler);
		
		if (!mService.isPlayerCreate()) {
			mService.startPlayer(audio.getPath());
			updateResource(true);
		} else if (isPlayingBefore) {
			Log.i("PlayerActivity", "playing before ");
			mService.prPlayer();
			updateResource(isPlayingBefore);
		} else {
			Log.i("PlayerActivity", "not playing before ");
			updateResource(isPlayingBefore);
		}
	}
	
	/**
	 * 
	 */
	private void disconnect() {
		if (mBound) {
			
			isPlayingBefore = mService.isPlayerPlay();
			if (isFinishing()) {
				Log.i("PlayerActivity", "disconnect stop Player ");
				mService.stopPlayer();
				updateResource(false);
			} else {
				if (mService.isPlayerPlay()) {
					mService.prPlayer();
					updateResource(false);
				}
			}
			mService.setHandler(null);
		} else {
			isPlayingBefore = false;
		}
	}
	
	@Override
	protected void serviceDisconnected(MediaService service) {
		disconnect();
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_player);
		
		Bundle bundle = getIntent().getExtras();
		if (bundle != null) {
			audio = bundle.getParcelable(KEY_AUDIO_PLAY);
		} else {
			// TODO Error
		}
		
		play = (ImageButton) findViewById(R.id.player_play);
		play.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO 
				if (mBound) {
					if (!mService.isPlayerCreate()) {
						mService.startPlayer(audio.getPath());
						updateResource(true);
					} else {
						mService.prPlayer();
						updateResource(mService.isPlayerPlay());
					}
				}
			}
		});
		
		myView = (VisualizerView) findViewById(R.id.player_view);
		myViewFFT = (VisualizerFFTView) findViewById(R.id.player_viewfft);
	}
	
	private void updateResource(boolean isPlaying) {
		play.setImageResource(isPlaying ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		
		outState.putBoolean(KEY_AUDIO_PLAYING, mService != null && mService.isPlayerCreate() && mService.isPlayerPlay());
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		isPlayingBefore = savedInstanceState.getBoolean(KEY_AUDIO_PLAYING);
		Log.i("PlayerActivity", "onRestoreInstanceState");
		Log.i("PlayerActivity", "Isplayingbefore" + isPlayingBefore);
		if (isPlayingBefore && mService != null && !mService.isPlayerPlay()) {
			mService.prPlayer();
		} 
	}

	
	/**
	 * 
	 */
    private void addDataFFT(byte[] fft, int rate) {
    	if (mBound && mService.isPlayerPlay()) {
    		myViewFFT.updateVisualizer(fft);
    	}
    }
    
    /**
	 * 
	 */
    private void addDataWaveFrom(byte[] waveFrom, int rate) {
    	if (mBound && mService.isPlayerPlay()) {
    		myView.updateVisualizer(waveFrom);
    	}
    }
	
	
	@Override
	protected void onStop() {
		disconnect();
		super.onStop();
		
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		
	}
	
}
