package eu.nazgee.eccerobo.remote.mjpeg;

import java.io.IOException;

import eu.nazgee.eccerobo.remote.mjpeg.SimpleMjpegActivity.DoRead;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class MjpegView extends SurfaceView implements SurfaceHolder.Callback {
    private static final String TAG = "MjpegView";

    public final static int POSITION_UPPER_LEFT  = 9;
    public final static int POSITION_UPPER_RIGHT = 3;
    public final static int POSITION_LOWER_LEFT  = 12;
    public final static int POSITION_LOWER_RIGHT = 6;

    public final static int SIZE_STANDARD   = 1; 
    public final static int SIZE_BEST_FIT   = 4;
    public final static int SIZE_FULLSCREEN = 8;
    public final static int SIZE_BEST_FIT_TOP = 16;

    private MjpegViewThread thread;
    private MjpegInputStream mIn = null;    
    private boolean showFps = true;
    private boolean mRun = false;
    private boolean surfaceDone = false;    
    private Paint overlayPaint;
    private int overlayTextColor;
    private int overlayBackgroundColor;
    private int ovlPos;
    private int dispWidth;
    private int dispHeight;
    private int displayMode;

    public class MjpegViewThread extends Thread {
        private SurfaceHolder mSurfaceHolder;
        private int frameCounter = 0;
        private long start;
        private Bitmap ovl;

        public MjpegViewThread(SurfaceHolder surfaceHolder) {
            mSurfaceHolder = surfaceHolder;
        }

        private Rect destRect(int bmw, int bmh) {
            int tempx;
            int tempy;
            if (displayMode == MjpegView.SIZE_STANDARD) {
                tempx = (dispWidth / 2) - (bmw / 2);
                tempy = (dispHeight / 2) - (bmh / 2);
                return new Rect(tempx, tempy, bmw + tempx, bmh + tempy);
            }
            if (displayMode == MjpegView.SIZE_BEST_FIT) {
                float bmasp = (float) bmw / (float) bmh;
                bmw = dispWidth;
                bmh = (int) (dispWidth / bmasp);
                if (bmh > dispHeight) {
                    bmh = dispHeight;
                    bmw = (int) (dispHeight * bmasp);
                }
                tempx = (dispWidth / 2) - (bmw / 2);
                tempy = (dispHeight / 2) - (bmh / 2);
                return new Rect(tempx, tempy, bmw + tempx, bmh + tempy);
            }
            if (displayMode == MjpegView.SIZE_BEST_FIT_TOP) {
                float bmasp = (float) bmw / (float) bmh;
                bmw = dispWidth;
                bmh = (int) (dispWidth / bmasp);
                if (bmh > dispHeight) {
                    bmh = dispHeight;
                    bmw = (int) (dispHeight * bmasp);
                }
                tempx = (dispWidth / 2) - (bmw / 2);
                tempy = 0;
                return new Rect(tempx, tempy, bmw + tempx, bmh + tempy);
            }
            if (displayMode == MjpegView.SIZE_FULLSCREEN){
                return new Rect(0, 0, dispWidth, dispHeight);
            }
            return null;
        }

        public void setSurfaceSize(int width, int height) {
            synchronized(mSurfaceHolder) {
                dispWidth = width;
                dispHeight = height;
            }
        }

        private Bitmap makeFpsOverlay(Paint p, String text) {
            Rect b = new Rect();
            p.getTextBounds(text, 0, text.length(), b);
            int bwidth  = b.width()+2;
            int bheight = b.height()+2;
            Bitmap bm = Bitmap.createBitmap(bwidth, bheight, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(bm);
            p.setColor(overlayBackgroundColor);
            c.drawRect(0, 0, bwidth, bheight, p);
            p.setColor(overlayTextColor);
            c.drawText(text, -b.left+1, (bheight/2)-((p.ascent()+p.descent())/2)+1, p);
            return bm;           
        }

        public void run() {
            start = System.currentTimeMillis();
            PorterDuffXfermode mode = new PorterDuffXfermode(PorterDuff.Mode.DST_OVER);
            Bitmap bm;
            int width;
            int height;
            Rect destRect;
            Canvas c = null;
            Paint p = new Paint();
            String fps;
            while (mRun) {
                if(surfaceDone) {
                    try {
                        c = mSurfaceHolder.lockCanvas();
                        if (c == null) {
                        	Log.e(TAG, "no canvas?");
                        	continue;
                        }
                        synchronized (mSurfaceHolder) {
                            try {
                                bm = mIn.readMjpegFrame();
                                destRect = destRect(bm.getWidth(),bm.getHeight());
//                                destRect = destRect(200,200);
                                
//                                if (frameCounter % 2 == 0)
//                                	c.drawColor(Color.RED);
//                                else
//                                	c.drawColor(Color.BLUE);
                                
                                c.drawBitmap(bm, null, destRect, p);
                                if(showFps) {
                                    p.setXfermode(mode);
                                    if(ovl != null) {
                                        height = ((ovlPos & 1) == 1) ? destRect.top : destRect.bottom-ovl.getHeight();
                                        width  = ((ovlPos & 8) == 8) ? destRect.left : destRect.right -ovl.getWidth();
                                        c.drawBitmap(ovl, width, height, null);
                                    }
                                    p.setXfermode(null);
                                    frameCounter++;
                                    if((System.currentTimeMillis() - start) >= 1000) {
                                        fps = String.valueOf(frameCounter)+" fps";
                                        frameCounter = 0; 
                                        start = System.currentTimeMillis();
                                        ovl = makeFpsOverlay(overlayPaint, fps);
                                    }
                                }
                            } catch (IOException e) {
                                e.getStackTrace();
                                Log.d(TAG, "catch IOException hit in run", e);
                                return;
                            }
                        }
                    } finally { 
                        if (c != null) {
                            mSurfaceHolder.unlockCanvasAndPost(c); 
                        }
                    }
                }
            }
        }
    }

    private void init() {
        SurfaceHolder holder = getHolder();
        holder.addCallback(this);

        setFocusable(true);
        overlayPaint = new Paint();
        overlayPaint.setTextAlign(Paint.Align.LEFT);
        overlayPaint.setTextSize(12);
        overlayPaint.setTypeface(Typeface.DEFAULT);
        overlayTextColor = Color.WHITE;
        overlayBackgroundColor = Color.BLACK;
        ovlPos = MjpegView.POSITION_LOWER_RIGHT;
        displayMode = MjpegView.SIZE_STANDARD;
        dispWidth = getWidth();
        dispHeight = getHeight();
    }

	public void startPlayback() {
		if (mIn != null) {
			if (!mRun) {
				thread = new MjpegViewThread(getHolder());
				thread.setSurfaceSize(getMeasuredWidth(), getMeasuredHeight());
			}
			mRun = true;
			thread.start();
		}
	}

    public void stopPlayback() { 
        mRun = false;
        boolean retry = true;
        while(retry) {
            try {
                thread.join();
                mIn.close();
                retry = false;
            } catch (InterruptedException e) {
                e.getStackTrace();
                Log.d(TAG, "catch IOException hit in stopPlayback", e);
            } catch (IOException e) {
				e.printStackTrace();
			}
        }
    }

    public MjpegView(Context context, AttributeSet attrs) { 
        super(context, attrs); init(); 
    }

	public void surfaceChanged(SurfaceHolder holder, int f, int w, int h) {
		if (thread != null) {
			thread.setSurfaceSize(w, h);
		}
	}

    public void surfaceDestroyed(SurfaceHolder holder) { 
        surfaceDone = false; 
        stopPlayback(); 
    }

    public MjpegView(Context context) { 
        super(context);
        init(); 
    }

    public void surfaceCreated(SurfaceHolder holder) { 
        surfaceDone = true; 
    }

    public void showFps(boolean b) { 
        showFps = b; 
    }

    public void setSource(MjpegInputStream source) { 
        mIn = source;
        startPlayback();
    }

    public void setOverlayPaint(Paint p) { 
        overlayPaint = p; 
    }

    public void setOverlayTextColor(int c) { 
        overlayTextColor = c; 
    }

    public void setOverlayBackgroundColor(int c) { 
        overlayBackgroundColor = c; 
    }

    public void setOverlayPosition(int p) { 
        ovlPos = p; 
    }

    public void setDisplayMode(int s) { 
        displayMode = s; 
    }
}
