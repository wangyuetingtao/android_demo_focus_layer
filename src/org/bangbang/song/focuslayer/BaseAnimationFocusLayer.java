
package org.bangbang.song.focuslayer;

import org.bangbang.song.android.commonlib.Grid;
import org.bangbang.song.android.commonlib.Grid.GridDrawer;
import org.bangbang.song.android.commonlib.ViewUtil;
import org.bangbang.song.demo.focuslayer.R;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsoluteLayout;
import android.widget.ImageView;

/**
 * the animated bitmap MUST not has alpha channel.
 * 
 * @author bysong
 */
public class BaseAnimationFocusLayer extends
        AbsoluteLayout // yes we need this layout absolutely.
        implements IFocusAnimationLayer {
    private static final String TAG = BaseAnimationFocusLayer.class.getSimpleName();

    protected static final boolean DEBUG = true;
    protected static final boolean DRAW_GRIG = true && DEBUG;
    protected static final int OFFSET_X = 0;
    protected static final int OFFSET_Y = 0;
    private static final int DEFAULT_ANIMATION_DURATION = 2222;
    private static final float DEFAULT_SCALE_FACOTR = 1.3f;

    protected static final boolean DEBUG_TRANSFER_ANIMATION = true;

    protected static final boolean DEBUG_SCALE_ANIMATION = true;

    private Grid.GridDrawer mGridDrawer;

    protected Rect mTmpRect = new Rect();

    protected Rect mLastFocusRect = new Rect();
    protected Rect mLastScaledFocusRect = new Rect();
    protected Bitmap mLastFocusBitmap = null;

    protected Rect mCurrentFocusRect = new Rect();
    protected Rect mCurrentScaledFocusRect = new Rect();
    protected Bitmap mCurrentFocusBitmap = null;

    private Matrix mMatrix;
    private RectF mTmpRectF;

    /** used for transfer */
    protected FixedSizeView mFocusRectView;
    /** used for scale */
    protected FixedSizeView mLastFocusView;
    /** used for scale */
    protected FixedSizeView mCurrentFocusView;

    /** in millisec */
    protected int mDuration;
    protected float mScaleFactor;
    protected boolean mDisableScaleAnimation;

    private boolean mFirstFocus = true;

    public BaseAnimationFocusLayer(Context context) {
        this(context, null);
    }

    public BaseAnimationFocusLayer(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BaseAnimationFocusLayer(Context context, AttributeSet attrs,
            int defStyle) {
        super(context, attrs, defStyle);

        mDuration = DEFAULT_ANIMATION_DURATION;
        mScaleFactor = DEFAULT_SCALE_FACOTR;
        mDisableScaleAnimation = false;

        init();
    }

    private void init() {
        mLastFocusView = new FixedSizeView(getContext());
        mLastFocusView.setWidth(0);
        mLastFocusView.setHeight(0);
        mCurrentFocusView = new FixedSizeView(getContext());
        mCurrentFocusView.setWidth(0);
        mCurrentFocusView.setHeight(0);
        addView(mLastFocusView);
        addView(mCurrentFocusView);

        mMatrix = new Matrix();
        mFocusRectView = onInflateFocusRectView(((LayoutInflater) getContext().getSystemService(
                Context.LAYOUT_INFLATER_SERVICE)));
        addView(mFocusRectView);

        setId(Utils.FOCUS_LAYER_ID);

        setBackgroundColor(Color.TRANSPARENT);
        if (DRAW_GRIG) {
            setWillNotDraw(false);
            mGridDrawer = new GridDrawer(50, 50);
        }
    }

    public void setDuration(int duration) {
        mDuration = duration;
    }

    public int getDration() {
        return mDuration;
    }

    public void setScaleFactor(float factor) {
        mScaleFactor = factor;
    }

    public float getScaleFactor() {
        return mScaleFactor;
    }

    /**
     * inflate focus view, but NOT add to view hierarchy, caller will do this
     * for u.
     * 
     * @param layoutInflater
     * @return
     */
    protected BaseAnimationFocusLayer.FixedSizeView onInflateFocusRectView(
            LayoutInflater layoutInflater) {
        FixedSizeView v = new FixedSizeView(getContext());
        v.setBackgroundResource(R.drawable.search_button_hover);

        return v;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (DRAW_GRIG) {
            int w = getWidth();
            int h = getHeight();
            mGridDrawer.onDraw(canvas, w, h);
        }
    }

    protected void offsetAnyViewRectToMyCoord(View view, Rect rect) {
        View root = view.getRootView();
        if (root instanceof ViewGroup) {
            ((ViewGroup) root).offsetDescendantRectToMyCoords(view, rect);
            ((ViewGroup) root).offsetRectIntoDescendantCoords(this, rect);
        } else {
            Log.e(TAG, "view's parent is NOT a ViewGroup.");
        }
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if (hasFocus) {
            updateFocusView(v);
        }
    }

    protected void updateFocusView(View focus) {
        if (null == focus) {
            return;
        }

        focus.getFocusedRect(mTmpRect);
        // focus.getDrawingRect(mTmpRect);
        offsetAnyViewRectToMyCoord(focus, mTmpRect);

        mLastFocusRect = new Rect(mCurrentFocusRect);
        mCurrentFocusRect = new Rect(mTmpRect);

        int width = focus.getWidth();
        int height = focus.getHeight();
        if (width <= 0 || height <= 0) {
            Log.w(TAG, "invalid w or h. w: " + width + " h: " + height);
            return;
        }

        if (!mDisableScaleAnimation) {
            mMatrix.reset();
            // adjust rect by scale factor.
            mTmpRectF = new RectF(mCurrentFocusRect.left, mCurrentFocusRect.top,
                    mCurrentFocusRect.right, mCurrentFocusRect.bottom);
            mMatrix.setScale(mScaleFactor, mScaleFactor, mTmpRectF.centerX(), mTmpRectF.centerY());
            mMatrix.mapRect(mTmpRectF);
            mCurrentScaledFocusRect = new Rect((int) mTmpRectF.left, (int) mTmpRectF.top,
                    (int) mTmpRectF.right, (int) mTmpRectF.bottom);

            mMatrix.reset();
            mTmpRectF = new RectF(mLastFocusRect.left, mLastFocusRect.top,
                    mLastFocusRect.right, mLastFocusRect.bottom);
            mMatrix.setScale(mScaleFactor, mScaleFactor, mTmpRectF.centerX(),
                    mTmpRectF.centerY());
            mMatrix.mapRect(mTmpRectF);
            mLastScaledFocusRect = new Rect((int) mTmpRectF.left,
                    (int) mTmpRectF.top, (int) mTmpRectF.right,
                    (int) mTmpRectF.bottom);
        } else {
            mLastScaledFocusRect = new Rect(mLastFocusRect);
            mCurrentScaledFocusRect = new Rect(mCurrentFocusRect);
        }

        Log.d(TAG, "updateFocusView(). view: " + focus);
        Log.d(TAG, "mCurrentFocusRect: " + mCurrentFocusRect);
        Log.d(TAG, "mLastFocusRect: " + mLastFocusRect);
        Log.d(TAG, "mCurrentScaledFocusRect: " + mCurrentScaledFocusRect);
        Log.d(TAG, "mLastScaledFocusRect: " + mLastScaledFocusRect);

        if (!mDisableScaleAnimation) {
            mLastFocusBitmap = mCurrentFocusBitmap;
            mCurrentFocusBitmap = getBitmap(focus);
        }

        if (mFirstFocus) {
            mFirstFocus = false;
            initFocusTarget();
        }
    }

    private Bitmap getBitmap(View focus) {
        int oldWidth = focus.getWidth();
        int oldHeight = focus.getHeight();

        Bitmap bitmap = Bitmap.createBitmap(oldWidth, oldHeight, Bitmap.Config.ARGB_8888);
        focus.draw(new Canvas(bitmap));
        // bitmap = Bitmap.createScaledBitmap(bitmap,
        // mCurrentScaledFocusRect.width(), mCurrentScaledFocusRect.height(),
        // false);

        // return bitmap;

        int w = mCurrentScaledFocusRect.width();
        int h = mCurrentScaledFocusRect.height();
        Log.d(TAG, "focus bitmap W: " + w + " H: " + h);
        return ViewUtil.getBitmapX(focus, w, h);
    }

    private void deubgFocusRect() {
        int width = mFocusRectView.getWidth();
        int height = mFocusRectView.getHeight();
        int left = mFocusRectView.getLeft();
        int top = mFocusRectView.getTop();
        Log.d(TAG, "mFocusRectView left: " + left + " top: " + top + " width: " + width
                + " height: " + height);
    }
    
    @Override
    protected void onAnimationStart() {
        // TODO Auto-generated method stub
        super.onAnimationStart();
        Log.d(TAG, "onAnimationStart");
    }
    @Override
    protected void onAnimationEnd() {
        // TODO Auto-generated method stub
        super.onAnimationEnd();
        Log.d(TAG, "onAnimationEnd");
    }

    private void initFocusTarget() {
            // no animation in firstly focus on.
            mLastFocusRect = new Rect(mCurrentFocusRect);
            mLastScaledFocusRect = new Rect(mCurrentScaledFocusRect);
    
            Log.d(TAG, "first focus: mLastFocusRect: " + mLastFocusRect);
            Log.d(TAG, "first focus: mCurrentFocusRect: " + mCurrentFocusRect);
            Log.d(TAG, "first focus: mLastScaledFocusRect: " + mLastScaledFocusRect);
            Log.d(TAG, "first focus: mCurrentScaledFocusRect: " + mCurrentScaledFocusRect);
            
            int width = mCurrentScaledFocusRect.width();
            int height = mCurrentScaledFocusRect.height();
            int x = mCurrentScaledFocusRect.left;
            int y = mCurrentScaledFocusRect.top;
            
            mFocusRectView.setWidth(width);
            mFocusRectView.setHeight(height);
            updateViewLayout(mFocusRectView, new AbsoluteLayout.LayoutParams(
                    width,
                    height,
                    x,
                    y
                    ));
    //      updateViewLayout(mCurrentFocusView, new AbsoluteLayout.LayoutParams(
    //              width,
    //              height,
    //              x,
    //              y
    //              ));
    //      updateViewLayout(mLastFocusView, new AbsoluteLayout.LayoutParams(
    //              width, 
    //              height, 
    //              x,
    //              y
    //              ));
        }

    /**
     * fixed width & height.
     * <p>
     * after construct it, you must explicitly set width & height
     * 
     * @see #setWidth(float)
     * @see #setHeight(float)
     * @author bysong
     */
    public static class FixedSizeView extends
            // View ?
            ImageView
    {

        private int mWidth;
        private int mHeight;

        public FixedSizeView(Context context, AttributeSet attrs, int defStyle) {
            super(context, attrs, defStyle);

            init();
        }

        public FixedSizeView(Context context, AttributeSet attrs) {
            super(context, attrs);

            init();
        }

        public FixedSizeView(Context context) {
            super(context);

            init();
        }

        void init() {
            mWidth = 0;
            mHeight = 0;
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);

            int measuredWidth = MeasureSpec.makeMeasureSpec(MeasureSpec.EXACTLY, mWidth);
            int measuredHeight = MeasureSpec.makeMeasureSpec(MeasureSpec.EXACTLY, mHeight);

            // Log.d(TAG, "measuredWidth: " +
            // MeasureSpec.toString(measuredWidth) + " measuredHeight: " +
            // MeasureSpec.toString(measuredHeight));
            setMeasuredDimension(measuredWidth, measuredHeight);
        }

        public void setWidth(float w) {
            // Log.d(TAG, "setWidth(). w: " + w);
            mWidth = (int) w;

            // updateFocusLayoutparams();
            invalidate();
        }

        public void setHeight(float h) {
            // Log.d(TAG, "setHeight(). h: " + h);
            mHeight = (int) h;

            // updateFocusLayoutparams();
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            // Log.d(TAG, "onDraw(). view: " + this);
        }

    }
}
