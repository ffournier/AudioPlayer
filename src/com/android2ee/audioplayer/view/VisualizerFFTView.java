package com.android2ee.audioplayer.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class VisualizerFFTView extends View {
	
	  private byte[] mBytes;
	  private float[] mPoints;
	  private Rect mRect = new Rect();

	  private Paint mForePaint = new Paint();
	  
	  private int mDivisions;

	  /**
	   * Renders the FFT data as a series of lines, in histogram form
	   */
	  public VisualizerFFTView(Context context) {
		  super(context);
		  init();
	  }
	    
	  public VisualizerFFTView(Context context, AttributeSet attrs,
				int defStyleAttr, int defStyleRes) {
		  super(context, attrs, defStyleAttr, defStyleRes);
		  init();
	  }



	  public VisualizerFFTView(Context context, AttributeSet attrs, int defStyle) {
		  super(context, attrs, defStyle);
		  init();
	  }



	  public VisualizerFFTView(Context context, AttributeSet attrs) {
		  super(context, attrs);
		  init();
	  }



	  private void init() {
		  mBytes = null;

		  mForePaint.setAntiAlias(true);
	      mForePaint.setColor(Color.rgb(255, 128, 0));
	      //1024 lenght
		  mDivisions = 20;
	  }
	  
	  public void updateVisualizer(byte[] bytes) {
		  mBytes = bytes;
	      invalidate();
	  }
	  
	  public void startVisualizer() {
		  mBytes = null;
		  mPoints = null;
		  invalidate();
	  }

	  public void endVisualizer() {
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

	    return ss;
	  }

	  @Override
	  public void onDraw(Canvas canvas)
	  {
		  
		  if (mBytes == null) {
			  return;
		  }
		  
		  if (mPoints == null || mPoints.length < (mBytes.length * 4 / mDivisions)) {
	          mPoints = new float[(mBytes.length * 4 / mDivisions)];
	      } 
		  
		  mRect.set(0, 0, getWidth(), getHeight());
		  
		  int width_stroke = (getWidth() * mDivisions) / mBytes.length - 5;
		  mForePaint.setStrokeWidth(width_stroke); // 5 margin 
		  
		  for (int i = 0; i < mBytes.length / mDivisions; i++) {
			  mPoints[i * 4] =  (mRect.width() * i) / (mBytes.length / mDivisions - 1) + width_stroke / 2;
			  mPoints[i * 4 + 2] =  mPoints[i * 4];
			  
			  byte rfk = mBytes[mDivisions * i];
			  byte ifk = mBytes[mDivisions * i + 1];
			  float magnitude = (rfk * rfk + ifk * ifk);
			  int dbValue = (int) (10 * Math.log10(magnitude));
			
			  mPoints[i * 4 + 1] = mRect.height();
			  // max 102.3502
			  mPoints[i * 4 + 3] = (float) (mRect.height() - (((dbValue * 2) * mRect.height()) / 102.3502)) + 3;  // +3 to draw the line
			  
		  }
		  canvas.drawLines(mPoints, mForePaint);
	  }
	  
	  private static class SavedState extends BaseSavedState {
		  
		    protected byte[] mBytes;
		    protected float[] mPoints;

		    SavedState(Parcelable superState) {
		    	super(superState);
		    }

		    private SavedState(Parcel in) {
		    	super(in);
		    	this.mBytes = in.createByteArray();
		    	this.mPoints = in.createFloatArray();
		    }

		    @Override
		    public void writeToParcel(Parcel out, int flags) {
		    	super.writeToParcel(out, flags);
		    	out.writeByteArray(mBytes);
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
