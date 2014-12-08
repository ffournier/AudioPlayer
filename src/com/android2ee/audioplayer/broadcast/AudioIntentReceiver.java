package com.android2ee.audioplayer.broadcast;

import com.android2ee.audioplayer.service.MediaService;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AudioIntentReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getAction().equals(
                android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY)) {
			// signal your service to stop playback
			// (via an Intent, for instance)
			Intent service = new Intent(context, MediaService.class);
			service.putExtra(MediaService.KEY_ACTION_PLAY, MediaService.ACTION_STOP);
			context.startService(service);
		}

	}

}
