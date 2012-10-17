package eu.nazgee.eccerobo.remote.mjpeg;


import org.andengine.opengl.view.RenderSurfaceView;
import org.andengine.ui.activity.BaseGameActivity;

import android.graphics.PixelFormat;

/**
 * (c) 2010 Nicolas Gramlich
 * (c) 2011 Zynga Inc.
 * 
 * @author Nicolas Gramlich
 * @since 21:38:32 - 24.05.2010
 */
public abstract class BaseMjpegActivity extends BaseGameActivity {
	// ===========================================================
	// Constants
	// ===========================================================

	// ===========================================================
	// Fields
	// ===========================================================

	private MjpegView mMjpegSurfaceView;

	// ===========================================================
	// Constructors
	// ===========================================================

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================

	@Override
	protected void onSetContentView() {
		this.mRenderSurfaceView = new RenderSurfaceView(this);

		this.mRenderSurfaceView.setEGLConfigChooser(4, 4, 4, 4, 16, 0);
		this.mRenderSurfaceView.getHolder().setFormat(PixelFormat.TRANSLUCENT);

		this.mRenderSurfaceView.setRenderer(this.mEngine, this);

		this.setContentView(this.mRenderSurfaceView, BaseGameActivity.createSurfaceViewLayoutParams());

		this.setMjpegSurfaceView(new MjpegView(this));
		this.addContentView(this.getMjpegSurfaceView(), BaseGameActivity.createSurfaceViewLayoutParams());

		this.mRenderSurfaceView.setZOrderMediaOverlay(true);
		this.mRenderSurfaceView.bringToFront();
	}

	@Override
	protected void onPause() {
		super.onPause();
//		finish();
	}

	public MjpegView getMjpegSurfaceView() {
		return mMjpegSurfaceView;
	}

	public void setMjpegSurfaceView(MjpegView mMjpegSurfaceView) {
		this.mMjpegSurfaceView = mMjpegSurfaceView;
	}

	// ===========================================================
	// Methods
	// ===========================================================

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}