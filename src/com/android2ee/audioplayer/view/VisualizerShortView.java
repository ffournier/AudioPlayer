package com.android2ee.audioplayer.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.audiofx.Visualizer;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;



/**
 * A simple class that draws waveform data received from a
 * {@link Visualizer.OnDataCaptureListener#onWaveFormDataCapture }
 */
public class VisualizerShortView extends View {
	
    private Short[] mShorts;
    private float[] mPoints;
    private Rect mRect = new Rect();
    
    private Paint mForePaint = new Paint();

    public VisualizerShortView(Context context) {
        super(context);
        init();
    }
    
    

    public VisualizerShortView(Context context, AttributeSet attrs,
			int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
		init();
	}



	public VisualizerShortView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}



	public VisualizerShortView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}



	private void init() {
        mShorts = null;
        mForePaint.setStrokeWidth(1f);
        mForePaint.setAntiAlias(true);
        mForePaint.setColor(Color.rgb(0, 128, 255));
    }

    public void updateVisualizer(short[] shorts) {
    	mShorts = new Short[shorts.length];
    	for (int i = 0; i < shorts.length; i ++) {
    		mShorts[i] = shorts[i];
    	}
    	invalidate();
    }
    
    public void endVisualizer() {
    	mShorts = null;
    	invalidate();
    }
    
    public void startVisualizer() {
    	mShorts = null;
    	mPoints = null;
       	invalidate();
    }
    
    @Override
	  protected void onRestoreInstanceState(Parcelable state) {
		  if (!(state instanceof SavedState)) {
			  super.onRestoreInstanceState(state);
		  } else {
			  SavedState ss = (SavedState)state;
			  super.onRestoreInstanceState(ss.getSuperState());
			  this.mShorts = ss.mShorts;
			  this.mPoints = ss.mPoints;
			  invalidate();
		  }
	  }

	  @Override
	  protected Parcelable onSaveInstanceState() {
		Parcelable superState = super.onSaveInstanceState();
	    SavedState ss = new SavedState(superState);
	    //end
	    ss.mShorts = this.mShorts;
	    ss.mPoints = this.mPoints;
	    
	    return ss;
	  }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mShorts == null || mShorts.length == 0) {
        	return;
        }
        
        if (mPoints == null || mPoints.length < mShorts.length * 4) {
            mPoints = new float[mShorts.length * 4];
        } 

        mRect.set(0, 0, getWidth(), getHeight());
        
        for (int i = 0; i < mShorts.length - 1; i++) {
            
        	mPoints[i * 4] = mRect.width() * i / (mShorts.length - 1);
           /* mPoints[i * 4 + 1] = mRect.height() / 2
                    + ((short) (mShorts[i] + 32768)) * (mRect.height() / 2) / 32768;*/
        	mPoints[i * 4 + 1] = (float) ((mRect.height() / 65536.0) * mShorts[i]) + mRect.height() / 2;
            mPoints[i * 4 + 2] = mRect.width() * (i + 1) / (mShorts.length - 1);
            mPoints[i * 4 + 3] = (float) ((mRect.height() / 65536.0) * mShorts[i + 1])  + mRect.height() / 2;
            
            Log.w("TAG", "Data " + mShorts[i]);
            Log.w("TAG", "Height " +  mRect.height() + " Data short " + (float) ((mRect.height() / 65536.0) * mShorts[i]));;
            Log.w("TAG", "Points "  + mPoints[i * 4] + " , " + mPoints[i * 4 + 1]);
        }
        canvas.drawLines(mPoints, mForePaint);
    }
    
    private static class SavedState extends BaseSavedState {
		  
    	Short[] mShorts;
	    float[] mPoints;
	    
	    SavedState(Parcelable superState) {
	    	super(superState);
	    }

	    @SuppressWarnings("unchecked")
		private SavedState(Parcel in) {
	    	super(in);
	    	this.mShorts = (Short[]) in.readArray(Short.class.getClassLoader());
	    	this.mPoints = in.createFloatArray();
	    }

	    @Override
	    public void writeToParcel(Parcel out, int flags) {
	    	super.writeToParcel(out, flags);
	    	out.writeArray(mShorts);
	    	out.writeFloatArray(mPoints);
	    }

	    //required field that makes Parcelables from a Parcel
	    public static final Parcelable.Creator<SavedState> CREATOR =
	        new Parcelable.Creator<SavedState>() {
	          public SavedState createFromParcel(Parcel in) {
	            return new SavedState(in);
	          }
	          public SavedState[] newArray(int size) {
	            return new SavedState[size];
	          }
	    };
  }
}