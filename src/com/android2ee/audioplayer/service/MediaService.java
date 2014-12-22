package com.android2ee.audioplayer.service;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Service;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.media.audiofx.Visualizer;
import android.media.audiofx.Visualizer.OnDataCaptureListener;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

/**
 * 
 * @author florian
 *
 */
public class MediaService extends Service implements OnAudioFocusChangeListener {
	
	public static final int PLAY_START = 1;
	public static final int PLAY_END = 2;
	public static final int PLAY_FFT = 3;
	public static final int PLAY_WAVEFROM = 4;
	public static final int RECORD_WAVEFROM = 5;
	
	public static final int ACTION_STOP = 1;
	
	public static final String KEY_RECORD_WAVEFROM = "com.android2ee.audioplayer.record.wavefrom_media";
	public static final String KEY_PLAY_FFT = "com.android2ee.audioplayer.fft_media";
	public static final String KEY_PLAY_WAVEFROM = "com.android2ee.audioplayer.wavefrom_media";
	public static final String KEY_PLAY_RATE = "com.android2ee.audioplayer.rate_media";
	public static final String KEY_ACTION_PLAY = "com.android2ee.audioplayer.action_play";
	
	private static final int RECORDER_SAMPLERATE = 8000;
	 
	private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
	 
	private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
	
	private static final int PLAYER_SAMPLERATE = 8000;
	private static final int PLAYER_CHANNELS = AudioFormat.CHANNEL_OUT_MONO;
	private static final int PLAYER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

	int BufferElements2Rec = 1024; // want to play 2048 (2K) since 2 bytes we use only 1024
	int BytesPerElement = 2; // 2 bytes in 16bit format
	
	//declaration
	private AudioRecord mRecorder;
	private Thread recordingThread = null;
	private Thread listenningThread = null;
	
	private AudioTrack mPlayer;
	private StatePlayer isRecord;
	private StatePlayer isPlay;
	
	private Timer mTimer;
	
	private Handler handler;
	
	private AudioFocusHelper audioFocusHelper;
	
	private Visualizer mVisualizer;

	public enum StatePlayer {
		STATE_DEFAULT,
		STATE_PLAY,
		STATE_PAUSE,
		STATE_END
	}
	
	private Object objectRecord = null;
	
	private LocalBinder mBinder = new LocalBinder();
	
	/**
	 * Binder
	 * @author florian
	 *
	 */
	public class LocalBinder extends Binder {

		public MediaService getService() {
			return MediaService.this;
		}
		
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		
		int bufferSize = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE,
			    RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING);
		
		isRecord = StatePlayer.STATE_DEFAULT;
		isPlay = StatePlayer.STATE_DEFAULT;
		handler = null;
		
		mTimer = new Timer();
		
		audioFocusHelper = new AudioFocusHelper(this);
		audioFocusHelper.requestFocus(this);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent != null && intent.getExtras() != null && intent.getExtras().containsKey(KEY_ACTION_PLAY)) {
			int action = intent.getIntExtra(KEY_ACTION_PLAY, -1);
			switch(action) {
			case ACTION_STOP :
				Log.i("MediaService", "ACTION STOP");
				if (isPlayerCreate() && isPlayerPlay()) {
					Log.i("MediaService", "ACTION STOP execute");
					prPlayer();
				}
				break;
			}
		}
		return super.onStartCommand(intent, flags, startId);
	}
	
	public void setHandler(Handler handler) {
		this.handler = handler;
		
	}
	
	public boolean isRecorderCreate() {
		return isRecord != StatePlayer.STATE_DEFAULT;
	}
	
	public boolean isRecorderPlay() {
		return isRecord == StatePlayer.STATE_PLAY;
	}
	
	
	/**
	 * 
	 * @return
	 */
	public boolean startRecorder(final String path) {
		if (isRecord !=  StatePlayer.STATE_DEFAULT) {
			stopRecorder();
		}
		mRecorder = null;
		isRecord =  StatePlayer.STATE_PLAY;
		mRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
			    RECORDER_SAMPLERATE, RECORDER_CHANNELS,
			    RECORDER_AUDIO_ENCODING, BufferElements2Rec * BytesPerElement);

		mRecorder.startRecording();
			   
			 
		recordingThread = new Thread(new Runnable() {

			   public void run() {

				   writeAudioDataToFile(path);

			   }
		}, "AudioRecorder Thread");
		recordingThread.start();
		return isRecord !=  StatePlayer.STATE_DEFAULT;
	}
	
	private void writeAudioDataToFile(String path) {
		short sData[] = new short[BufferElements2Rec];

	    FileOutputStream os = null;
	    try {
	    	Log.w("TAG", "wirting to file" + path);
	        
	    	os = new FileOutputStream(path);
	        
	    } catch (FileNotFoundException e) {
	        e.printStackTrace();
	    }

	    while (isRecord ==  StatePlayer.STATE_PLAY) {
	        // gets the voice output from microphone to byte format

	        mRecorder.read(sData, 0, BufferElements2Rec);
	        
	        try {
	            // // writes the data to file from buffer
	            // // stores the voice buffer
	            byte bData[] = short2byte(sData);
	            sendRecordWaveFrom(bData, 0);
	            os.write(bData, 0, BufferElements2Rec * BytesPerElement);
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
	    }
	    try {
	        os.close();
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	}

	
    //convert short to byte
	private byte[] short2byte(short[] sData) {
	    int shortArrsize = sData.length;
	    byte[] bytes = new byte[shortArrsize * 2];
	    for (int i = 0; i < shortArrsize; i++) {
	        bytes[i * 2] = (byte) (sData[i] & 0x00FF);
	        bytes[(i * 2) + 1] = (byte) (sData[i] >> 8);
	        sData[i] = 0;
	    }
	    return bytes;
	
	}
	
	
	/**
	 * 
	 * @return
	 */
	public boolean stopRecorder() {
		if (mRecorder != null) {
			if (isRecord != StatePlayer.STATE_DEFAULT) {
				isRecord = StatePlayer.STATE_DEFAULT;
				mRecorder.stop();
				recordingThread = null;
				
				Log.w("TAG", "stopRecorder");
				purgeTimer();
			}
			mRecorder = null;
		}
		return isRecord == StatePlayer.STATE_DEFAULT;
	}
	
	/**
	 * 
	 */
	public void releaseRecorder() {
		if (mRecorder != null) {
			stopRecorder();
			mRecorder.release();
			isRecord = StatePlayer.STATE_DEFAULT; 
		}
	}
	
	/**
	 * 
	 * @param object
	 */
	public void setAudioRecord(Object object) {
		objectRecord = object;
	}
	
	/**
	 * 
	 * @return
	 */
	public Object getAudioRecord() {
		return objectRecord;
	}
	
	/**
	 * 
	 * @return
	 */
	public boolean isPlayerCreate() {
		return isPlay != StatePlayer.STATE_DEFAULT;
	}
	
	/**
	 * 
	 * @return
	 */
	public boolean isPlayerPlay() {
		return isPlay == StatePlayer.STATE_PLAY ;
	}
	
	/**
	 * 
	 * @return
	 */
	public boolean startPlayer(final String path) {
		if (isPlay != StatePlayer.STATE_DEFAULT) {
			stopPlayer();
		}
		try {
			mPlayer = null;
			mPlayer = new AudioTrack(AudioManager.STREAM_MUSIC,PLAYER_SAMPLERATE,PLAYER_CHANNELS,PLAYER_AUDIO_ENCODING,
					BufferElements2Rec * BytesPerElement,AudioTrack.MODE_STREAM);
			mPlayer.getAudioSessionId();
			
			Log.i("MediaService", "audio id " + mPlayer.getAudioSessionId());
			if (mVisualizer != null) {
				mVisualizer.setEnabled(false);
				mVisualizer.release();
				mVisualizer = null;
			}
			mVisualizer = new Visualizer(mPlayer.getAudioSessionId());
			mVisualizer.setCaptureSize(Visualizer.getCaptureSizeRange()[1]);
			Log.i("MediaService", "audio size " + Visualizer.getCaptureSizeRange()[1]);
			mVisualizer.setDataCaptureListener(new OnDataCaptureListener() {
				
				@Override
				public void onWaveFormDataCapture(Visualizer visualizer, byte[] waveform,
						int samplingRate) {
					sendPlayWaveFrom(waveform, samplingRate);
				}
				
				@Override
				public void onFftDataCapture(Visualizer visualizer, byte[] fft,
						int samplingRate) {
					sendPlayFFT(fft, samplingRate);
				}
			}, Visualizer.getMaxCaptureRate() / 2, true, true);
			
			if (listenningThread != null) {
				listenningThread.interrupt();
				listenningThread = null;
			}
			listenningThread = new Thread(new Runnable() {
		        public void run() {
		            readAudioDataToFile(path);
		        }
		    }, "AudioTrack Thread");
			listenningThread.start();
		    
			mPlayer.play();
			
			isPlay = StatePlayer.STATE_PLAY;
			sendPlayStart();
			mVisualizer.setEnabled(true);
			
		} catch (IllegalArgumentException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (SecurityException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return isPlay != StatePlayer.STATE_DEFAULT;
	}
	
	private void readAudioDataToFile(String path) {
		byte sData[] = new byte[BufferElements2Rec * BytesPerElement];

		try {
			DataInputStream  dis = new DataInputStream(new BufferedInputStream(new FileInputStream(path)));
			Log.w("TAG", "reading to file" + path);
			
			synchronized (this) {
				try {
					wait(50);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		
		    while (isPlay == StatePlayer.STATE_PLAY  && dis.available() > 0) {
		    	int i=0;
		    	Log.w("TAG", "reading one sample buf");
		        while (dis.available() > 0 && i < sData.length) {
		        	sData[i] = dis.readByte();
		        	i++;
		        }
		        mPlayer.write(sData,0,sData.length);
		        Log.w("TAG", "write data");
		      }
		      dis.close();
		      sendPlayEnding();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		isPlay = StatePlayer.STATE_DEFAULT;
	}
	
	
	/**
	 * 
	 * @return
	 */
	public void prPlayer(){
		Log.i("MediaService", "prPlayer");
		if(isPlay != StatePlayer.STATE_DEFAULT){
			if (isPlay == StatePlayer.STATE_PLAY) {
				Log.i("MediaService", "prPlayer Pause");
				mPlayer.pause();
				mVisualizer.setEnabled(false);
				isPlay = StatePlayer.STATE_PAUSE;
				purgeTimer();
			} else {
				Log.i("MediaService", "prPlayer Play");
				mPlayer.play();
				mVisualizer.setEnabled(true);
				if (isPlay == StatePlayer.STATE_END) {
					sendPlayStart();
				}
				isPlay = StatePlayer.STATE_PLAY;
			}
		}
	}
	
	/**
	 * 
	 */
	private void sendPlayStart() {
		if (handler != null && isPlayerCreate()) {
			Message msg = handler.obtainMessage();
			msg.what = PLAY_START;
			Bundle bundle = new Bundle();
			msg.setData(bundle);
			handler.sendMessage(msg);
		}
	}
	
	/**
	 * 
	 */
	private void sendPlayFFT(byte[] fft, int rate) {
		if (handler != null && isPlayerCreate()) {
			Message msg = handler.obtainMessage();
			msg.what = PLAY_FFT;
			Bundle bundle = new Bundle();
			bundle.putByteArray(KEY_PLAY_FFT, fft);
			bundle.putInt(KEY_PLAY_RATE, rate);
			msg.setData(bundle);
			handler.sendMessage(msg);
		}
	}
	
	/**
	 * 
	 */
	private void sendPlayWaveFrom(byte[] waveFrom, int rate) {
		if (handler != null && isPlayerCreate()) {
			Message msg = handler.obtainMessage();
			msg.what = PLAY_WAVEFROM;
			Bundle bundle = new Bundle();
			bundle.putByteArray(KEY_PLAY_WAVEFROM, waveFrom);
			bundle.putInt(KEY_PLAY_RATE, rate);
			msg.setData(bundle);
			handler.sendMessage(msg);
		}
	}
	
	/**
	 * 
	 */
	private void sendRecordWaveFrom(byte[] waveFrom, int rate) {
		if (handler != null && isRecorderCreate()) {
			Message msg = handler.obtainMessage();
			msg.what = RECORD_WAVEFROM;
			Bundle bundle = new Bundle();
			bundle.putByteArray(KEY_RECORD_WAVEFROM, waveFrom);
			bundle.putInt(KEY_PLAY_RATE, rate);
			msg.setData(bundle);
			handler.sendMessage(msg);
		}
	}
	
	/**
	 * 
	 */
	private void sendRecordWaveFrom(int amp) {
		if (handler != null && isRecorderPlay()) {
			Message msg = handler.obtainMessage();
			msg.what = RECORD_WAVEFROM;
			Bundle bundle = new Bundle();
			bundle.putInt(KEY_RECORD_WAVEFROM, amp);
			msg.setData(bundle);
			handler.sendMessage(msg);
		}
	}
	
	/**
	 * 
	 */
	private void sendPlayEnding() {
		if (handler != null && isPlayerCreate()) {
			Message msg = handler.obtainMessage();
			msg.what = PLAY_END;
			Bundle bundle = new Bundle();
			msg.setData(bundle);
			handler.sendMessage(msg);
		}
	}
	
	/**
	 * 
	 */
	private void purgeTimer() {
		if (mTimer != null) {
			mTimer.purge();
		}
	}
	
	/**
	 * 
	 */
	private void cancelTimer() {
		if (mTimer != null) {
			mTimer.cancel();
		}
	}
	
	/**
	 * 
	 * @return
	 */
	public boolean stopPlayer() {
		if (mPlayer != null) {
			if (isPlay != StatePlayer.STATE_DEFAULT) {
				mPlayer.stop();
				purgeTimer();
				
				isPlay = StatePlayer.STATE_DEFAULT;
			}
			mPlayer = null;
		}
		return isPlay == StatePlayer.STATE_DEFAULT;
	}
	
	/**
	 * 
	 */
	public void releasePlayer() {
		if (mPlayer != null) {
			stopPlayer();
			mPlayer.release();
			isPlay = StatePlayer.STATE_DEFAULT;
		}
	}

	@Override
	public void onDestroy() {
		audioFocusHelper.abandonFocus(this);
		
		releaseRecorder();
		releasePlayer();
		
		cancelTimer();
		
		if (recordingThread != null) {
			recordingThread.interrupt();
		}
		
		if (mVisualizer != null) {
			mVisualizer.release();
		}
		
		super.onDestroy();
		
	}

	@Override
	public void onAudioFocusChange(int focusChange) {
		switch(focusChange) {
			case AudioManager.AUDIOFOCUS_GAIN:
			case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT:
			case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE:
				if (isPlayerCreate() && isPlayerPlay()) {
					mPlayer.setVolume(1.0f);
				}
				break;
			
			case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK:
				if (isPlayerCreate() && isPlayerPlay()) {
					mPlayer.setVolume(0.8f);
				}
			break;
			case AudioManager.AUDIOFOCUS_LOSS:
				if (isPlayerCreate() && isPlayerPlay()) {
					mPlayer.setVolume(1.0f);
				}
				break;
			case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
				Log.i("MediaService", "AUDIOFOCUS_LOSS_TRANSIENT");
				if (isPlayerCreate() && isPlayerPlay()) {
					Log.i("MediaService", "AUDIOFOCUS_LOSS_TRANSIENT execute");
					
					prPlayer();
				}
				break;
			case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
				if (isPlayerCreate() && isPlayerPlay()) {
					mPlayer.setVolume(0.2f);
				}
				break;
		}
	}
	
}
