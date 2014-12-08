package com.android2ee.audioplayer.view;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.audiofx.Visualizer;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.View;



/**
 * A simple class that draws waveform data received from a
 * {@link Visualizer.OnDataCaptureListener#onWaveFormDataCapture }
 */
public class VisualizerView extends View {
	
    private ArrayList<Byte> mBytesArray;
    private byte[] mBytes;
    private float[] mPoints;
    private Rect mRect = new Rect();
    
    private Paint mForePaint = new Paint();

    public VisualizerView(Context context) {
        super(context);
        init();
    }
    
    

    public VisualizerView(Context context, AttributeSet attrs,
			int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
		init();
	}



	public VisualizerView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}



	public VisualizerView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}



	private void init() {
        mBytesArray = new ArrayList<Byte>();
        mBytes = null;
        mForePaint.setStrokeWidth(1f);
        mForePaint.setAntiAlias(true);
        mForePaint.setColor(Color.rgb(0, 128, 255));
    }

    public void updateVisualizer(byte[] bytes) {
    	mBytes = bytes;
    	for (byte value : bytes) {
    		mBytesArray.add(value);
    	}
    	invalidate();
    }
    
    public void endVisualizer() {
    	mBytes = null;
    	Byte[] temp = mBytesArray.toArray(new Byte[mBytesArray.size()]);
       	mBytes = new byte[temp.length];
       	for (int i = 0; i < temp.length ; i++) {
       		mBytes[i] = temp[i];
       	}
       	mBytesArray.clear();
       	invalidate();
    }
    
    public void startVisualizer() {
    	mBytes = null;
       	mBytesArray.clear();
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
			  this.mBytes = ss.mBytes;
			  this.mPoints = ss.mPoints;
			  this.mBytesArray = ss.mBytesArray;
			  invalidate();
		  }
	  }

	  @Override
	  protected Parcelable onSaveInstanceState() {
		Parcelable superState = super.onSaveInstanceState();
	    SavedState ss = new SavedState(superState);
	    //end
	    ss.mBytes = this.mBytes;
	    ss.mPoints = this.mPoints;
	    ss.mBytesArray = this.mBytesArray;

	    return ss;
	  }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mBytes == null || mBytes.length == 0) {
        	return;
        }
        
      

        if (mPoints == null || mPoints.length < mBytes.length * 4) {
            mPoints = new float[mBytes.length * 4];
        } 

        mRect.set(0, 0, getWidth(), getHeight());
        
        for (int i = 0; i < mBytes.length - 1; i++) {
            
        	mPoints[i * 4] = mRect.width() * i / (mBytes.length - 1);
            mPoints[i * 4 + 1] = mRect.height() / 2
                    + ((byte) (mBytes[i] + 128)) * (mRect.height() / 2) / 128;
            mPoints[i * 4 + 2] = mRect.width() * (i + 1) / (mBytes.length - 1);
            mPoints[i * 4 + 3] = mRect.height() / 2
                    + ((byte) (mBytes[i + 1] + 128)) * (mRect.height() / 2) / 128;
            
        }
        canvas.drawLines(mPoints, mForePaint);
    }
    
    private static class SavedState extends BaseSavedState {
		  
	    byte[] mBytes;
	    float[] mPoints;
	    ArrayList<Byte> mBytesArray;
	    
	    SavedState(Parcelable superState) {
	    	super(superState);
	    }

	    @SuppressWarnings("unchecked")
		private SavedState(Parcel in) {
	    	super(in);
	    	this.mBytes = in.createByteArray();
	    	this.mPoints = in.createFloatArray();
	    	this.mBytesArray = (ArrayList<Byte>) in.readSerializable();
	    }

	    @Override
	    public void writeToParcel(Parcel out, int flags) {
	    	super.writeToParcel(out, flags);
	    	out.writeByteArray(mBytes);
	    	out.writeFloatArray(mPoints);
	    	out.writeSerializable(mBytesArray);
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