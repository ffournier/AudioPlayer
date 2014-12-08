package com.android2ee.audioplayer.activity;


import java.io.File;
import java.io.FilenameFilter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Pattern;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;

import com.android2ee.audioplayer.R;
import com.android2ee.audioplayer.pojo.POJOAudio;
import com.android2ee.audioplayer.service.MediaService;
import com.android2ee.audioplayer.view.VisualizerView;

public class MainActivity extends ABoundActivity {
	
	private static final String KEY_RECORD_DATAS = "com.android2ee.audioplayer.datas_record";
	private static final String SUB_PATH = "/android2ee_audioplayer/";
	
	// declaration
	private ImageButton btRecorder;
	private EditText etRecorder;
	private ListView listView;
	private ArrayList<POJOAudio> audios = new ArrayList<POJOAudio>();
	private ArrayAdapter<POJOAudio> mAdapter;
	
	private File pathFile = null;
	private static final String fileNamePattern = "android2ee_%s_%04d-%02d-%02d-%02d-%02d-%02d.pcm";
	private VisualizerView myView;
	
	private class MyHandler extends Handler {
		
		@Override
	    public void handleMessage(Message message) {
			switch (message.what) {
			case MediaService.RECORD_WAVEFROM:
				byte[] values = message.getData().getByteArray(MediaService.KEY_RECORD_WAVEFROM);
	        	addData(values);
	            break;    
	        }
		}
	}

	private Handler myHandler = new MyHandler();
	
	@Override
	protected void serviceConnected(MediaService service) {
		mService.setHandler(myHandler);
		updateResource(service.isRecorderCreate());
	}
	
	@Override
	protected void serviceDisconnected(MediaService service) {
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		
		etRecorder = (EditText) findViewById(R.id.edittext_recorder);
		
		btRecorder = (ImageButton) findViewById(R.id.button_recorder);
		btRecorder.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if (mBound) {
					updateRecord(!mService.isRecorderCreate());
				} else {
					// TODO
				}
			}
		});
		
		listView = (ListView) findViewById(R.id.list);
		mAdapter = new ArrayAdapter<POJOAudio>(this, android.R.layout.simple_list_item_1,audios);
		listView.setAdapter(mAdapter);
		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int position,
					long id) {
				if (mBound && !mService.isRecorderCreate()) {
					POJOAudio audio = mAdapter.getItem(position);
					// show details 
					Intent intent = new Intent(getApplicationContext(), PlayerActivity.class);
					intent.putExtra(PlayerActivity.KEY_AUDIO_PLAY, audio);
					startActivity(intent);
				}
			}
		});
		
		myView = (VisualizerView) findViewById(R.id.record_view_data);
		
		//File dir = getExternalFilesDir(Environment.DIRECTORY_MUSIC);
		// Sometimes this fucking External return null don't know why ?
		File dir = Environment.getExternalStorageDirectory();
		if (dir != null) {
			File subdir = new File(dir.getAbsolutePath() + SUB_PATH);
			if (!subdir.exists()) {
				subdir.mkdir();
			}
			pathFile = subdir;
		} else {
			// TODO default ? finish ?
			Log.e(getClass().getSimpleName(), "Error when get the filePath, getExternalFilesDir return null");
			finish();
		}
		Intent intent = new Intent(this, MediaService.class);
        startService(intent);
	}
	
	
	@Override
	protected void onResume() {
		super.onResume();
		if (audios.size() <= 0) {
			ArrayList<POJOAudio> audiostemp = loadFile();
			for (POJOAudio audio: audiostemp) {
				audios.add(audio);
			}
			mAdapter.notifyDataSetChanged();
		}
	}
	
	
	
	@Override
	protected void onPause() {
		super.onPause();
		// finish service when quit the application
        if (isFinishing()) {
        	Intent intent = new Intent(this, MediaService.class);
        	stopService(intent);
        }
	}


	/**
	 * 
	 * @param recorded
	 */
	private void updateRecord(boolean recorded) {
		// change image source
		if (recorded) {
			// Launch MediaPlayer Recorder
			Calendar calendar = Calendar.getInstance();
			String name = etRecorder.getText().toString();
			if (name == null || name.length() <= 0) {
				name = "unknown";
			}
			
			String fileName = String.format(fileNamePattern, name, calendar.get(Calendar.YEAR),
					calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH), calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE),
					calendar.get(Calendar.SECOND));
			String fileFullPath = pathFile.getAbsolutePath() + "/" + fileName;
			Date date = calendar.getTime();
			recorded = mService.startRecorder(fileFullPath);
			if (recorded) {
				myView.startVisualizer();
				POJOAudio object = new POJOAudio(fileFullPath, name, date);
				mService.setAudioRecord(object);
			}
		} else {
			// Stop Media Player Recorder
			POJOAudio object = (POJOAudio) mService.getAudioRecord();
			mService.stopRecorder();
			myView.endVisualizer();
			if (object != null) {
				// add in list
				audios.add(object);
				mAdapter.notifyDataSetChanged();
			}
		}
		updateResource(recorded);
	}
	
	private void updateResource(boolean recorded) {
		btRecorder.setImageResource(recorded ? android.R.drawable.btn_star : android.R.drawable.btn_radio);
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putParcelableArrayList(KEY_RECORD_DATAS, audios);
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		ArrayList<POJOAudio> audiostemp = savedInstanceState.getParcelableArrayList(KEY_RECORD_DATAS);
		audios.clear();
		for (POJOAudio audio: audiostemp) {
			audios.add(audio);
		}
		mAdapter.notifyDataSetChanged();
	}
	
	/**
	 * 
	 * @return
	 */
	private ArrayList<POJOAudio> loadFile() {
		ArrayList<POJOAudio> result = new ArrayList<POJOAudio>();
		// TODO
		
		final Pattern p = Pattern.compile("android2ee_[a-zA-Z0-9]*_[0-9]{4}-[0-9]{2}-[0-9]{2}-[0-9]{2}-[0-9]{2}-[0-9]{2}.pcm");
		File[] files = pathFile.listFiles(new FilenameFilter() {
			
			@Override
			public boolean accept(File dir, String filename) {
				return p.matcher(filename).matches();
			}
		});
		
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
		
		for (File file : files) {
			String fileName = file.getName();
			//if (fileName.split("_").length >=2) {
				String name = fileName.split("_")[1];
				String dateName = (fileName.split("_")[2]).split("\\.")[0];
				Date date;
				try {
					date = format.parse(dateName);
					POJOAudio object = new POJOAudio(file.getAbsolutePath(), name, date);
					result.add(object);
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			//}
		}
		return result;
	}
	
	 /**
	 * 
	 */
    private void addData(byte[] values) {
		if (mBound && mService.isRecorderPlay()) {
    		myView.updateVisualizer(values);
    	}
		
    }

	@Override
	protected void onDestroy() {
		super.onDestroy();
		
	}


}
