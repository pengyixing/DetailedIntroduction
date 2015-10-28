package com.example.uitl;  
  
import android.content.Context;  
import android.util.AttributeSet;  
import android.view.MotionEvent;  
import android.view.VelocityTracker;  
import android.view.View;  
import android.view.ViewConfiguration;  
import android.view.ViewGroup;  
import android.widget.Scroller;  
  
public class ScrollLayout extends ViewGroup {  
  
    private Scroller mScroller;  
    private VelocityTracker mVelocityTracker;  
    public static int mCurScreen;  
    private int mDefaultScreen = 0;  
    private static final int TOUCH_STATE_REST = 0;  
    private static final int TOUCH_STATE_SCROLLING = 1;  
    private static final int SNAP_VELOCITY = 600;  
    private int mTouchState = TOUCH_STATE_REST;  
    private int mTouchSlop;  
    private float mLastMotionX;  
    private int currentScreenIndex = 0;  
    public int getCurrentScreenIndex() {  
        return currentScreenIndex;  
    }  
  
    public void setCurrentScreenIndex(int currentScreenIndex) {  
        this.currentScreenIndex = currentScreenIndex;  
    }  
  
    public ScrollLayout(Context context, AttributeSet attrs) {  
        this(context, attrs, 0);  
    }  
  
    public ScrollLayout(Context context, AttributeSet attrs, int defStyle) {  
        super(context, attrs, defStyle);  
        mScroller = new Scroller(context);  
        mCurScreen = mDefaultScreen;  
        mTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();  
    }  
  
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int childLeft = 0;
        final int childCount = getChildCount();  
//        System.out.println("childCount=" + childCount);
        for (int i = 0; i < childCount; i++) {  
            final View childView = getChildAt(i);  
            if (childView.getVisibility() != View.GONE) {  
                final int childWidth = childView.getMeasuredWidth();  
                childView.layout(childLeft, 0, childLeft + childWidth,  
                        childView.getMeasuredHeight());  
                
                childLeft += childWidth;  
            }  
        }
    }  
  
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {  
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);  
  
        final int width = MeasureSpec.getSize(widthMeasureSpec);  
        final int widthMode = MeasureSpec.getMode(widthMeasureSpec);  
        if (widthMode != MeasureSpec.EXACTLY) {  
            throw new IllegalStateException(  
                    "ScrollLayout only canmCurScreen run at EXACTLY mode!");  
        }  
  
        final int heightMode = MeasureSpec.getMode(heightMeasureSpec);  
        if (heightMode != MeasureSpec.EXACTLY) {  
            throw new IllegalStateException(  
                    "ScrollLayout only can run at EXACTLY mode!");  
        }  
  
        final int count = getChildCount();  
        for (int i = 0; i < count; i++) {  
            getChildAt(i).measure(widthMeasureSpec, heightMeasureSpec);  
        }  
        System.out.println("moving to screen " + mCurScreen);  
        scrollTo(mCurScreen * width, 0);  
    }  
  
    public void snapToDestination() {  
        final int screenWidth = getWidth();  
        final int destScreen = (getScrollX() + screenWidth / 2) / screenWidth;  
        snapToScreen(destScreen);  
    }  
  
    public void snapToScreen(int whichScreen) {
        whichScreen = Math.max(0, Math.min(whichScreen, getChildCount() - 1));  
        if (getScrollX() != (whichScreen * getWidth())) {  
  
            final int delta = whichScreen * getWidth() - getScrollX();  
            mScroller.startScroll(getScrollX(), 0, delta, 0,  
                    Math.abs(delta) * 2);  
            mCurScreen = whichScreen;  
            invalidate();
        }  
    }  
  
    public void setToScreen(int whichScreen) {  
        whichScreen = Math.max(0, Math.min(whichScreen, getChildCount() - 1));  
        mCurScreen = whichScreen;  
        scrollTo(whichScreen * getWidth(), 0);  
    }  
  
    public int getCurScreen() {  
        return mCurScreen;  
    }  
  
    public void computeScroll() {  
        if (mScroller.computeScrollOffset()) {  
            scrollTo(mScroller.getCurrX(), mScroller.getCurrY());  
            postInvalidate();  
        }  
    }  
  
    public boolean onTouchEvent(MotionEvent event) {
  
        if (mVelocityTracker == null) {  
            mVelocityTracker = VelocityTracker.obtain();  
        }  
        mVelocityTracker.addMovement(event);  
        
        final int action = event.getAction();  
        final float x = event.getX();  
  
        switch (action) {  
        case MotionEvent.ACTION_DOWN:  
            if (!mScroller.isFinished()) {  
                mScroller.abortAnimation();  
            }  
            mLastMotionX = x;  
            break;  
  
        case MotionEvent.ACTION_MOVE:  
            int deltaX = (int) (mLastMotionX - x);  
            mLastMotionX = x;  
            scrollBy(deltaX, 0);  
            break;  
  
        case MotionEvent.ACTION_UP:  
            final VelocityTracker velocityTracker = mVelocityTracker;  
            velocityTracker.computeCurrentVelocity(1000);  
            int velocityX = (int) velocityTracker.getXVelocity();  
            if (velocityX > SNAP_VELOCITY && mCurScreen > 0) {  
                onScreenChangeListener.onScreenChange(mCurScreen - 1);  
                snapToScreen(mCurScreen - 1);  
            } else if (velocityX < -SNAP_VELOCITY  
                    && mCurScreen < getChildCount() - 1) {  
                onScreenChangeListener.onScreenChange(mCurScreen + 1);  
                //只往右移动才加载数据  
                onScreenChangeListenerDataLoad.onScreenChange(mCurScreen+1);  
                snapToScreen(mCurScreen + 1);  
            } else {  
                snapToDestination();  
            }  
  
            if (mVelocityTracker != null) {  
                mVelocityTracker.recycle();  
                mVelocityTracker = null;  
            }  
            mTouchState = TOUCH_STATE_REST; 
            
            System.out.println("mCurScreen=" + mCurScreen);
            break;  
        case MotionEvent.ACTION_CANCEL:  
            mTouchState = TOUCH_STATE_REST;  
            break;  
        }  
  
        return true;  
    }
  
    public boolean onInterceptTouchEvent(MotionEvent ev) {  
        final int action = ev.getAction();  
        if ((action == MotionEvent.ACTION_MOVE)  
                && (mTouchState != TOUCH_STATE_REST)) {  
            return true;  
        }  
  
        final float x = ev.getX();  
        switch (action) {  
        case MotionEvent.ACTION_MOVE:  
            final int xDiff = (int) Math.abs(mLastMotionX - x);  
            if (xDiff > mTouchSlop) {  
                mTouchState = TOUCH_STATE_SCROLLING;  
            }  
            break;  
  
        case MotionEvent.ACTION_DOWN:  
            mLastMotionX = x;  
            mTouchState = mScroller.isFinished() ? TOUCH_STATE_REST  
                    : TOUCH_STATE_SCROLLING;
            break;  
  
        case MotionEvent.ACTION_CANCEL:  
        case MotionEvent.ACTION_UP:  
            mTouchState = TOUCH_STATE_REST;  
            break;  
        }  
  
        return mTouchState != TOUCH_STATE_REST;  
    }  
  
    //分页监听  
    public interface OnScreenChangeListener {  
        void onScreenChange(int currentIndex);  
    }  
  
    private OnScreenChangeListener onScreenChangeListener;  
  
    public void setOnScreenChangeListener(  
            OnScreenChangeListener onScreenChangeListener) {  
        this.onScreenChangeListener = onScreenChangeListener;  
    }  
      
      
    //动态数据监听  
    public interface OnScreenChangeListenerDataLoad {  
        void onScreenChange(int currentIndex);  
    }  
    private OnScreenChangeListenerDataLoad onScreenChangeListenerDataLoad;  
  
    public void setOnScreenChangeListenerDataLoad(OnScreenChangeListenerDataLoad onScreenChangeListenerDataLoad) {  
        this.onScreenChangeListenerDataLoad = onScreenChangeListenerDataLoad;  
    }  
  
}  