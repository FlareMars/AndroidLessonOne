package flaremars.com.somethingdemo.utils;

import android.content.Context;
import android.graphics.Point;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.util.TypedValue;
import android.view.WindowManager;

public enum DisplayUtils {
	INSTANCE;
	
	public int dp2px(Context context,float dpValue) {		
		final float scale = context.getResources().getDisplayMetrics().density; 
	    return (int) (dpValue * scale + 0.5f); 
	}
	
	public Point getScreenWidth(Context context) {
		WindowManager wm = (WindowManager) context
				.getSystemService(Context.WINDOW_SERVICE);

		Point screenSize = new Point();
		if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN_MR1) {
			wm.getDefaultDisplay().getRealSize(screenSize);
		} else {
			screenSize.x = wm.getDefaultDisplay().getWidth();
			screenSize.y = wm.getDefaultDisplay().getHeight();
		}
		return screenSize;
	}

	public int getStatusBarHeight(Context context) {
		int result = 0;
		int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
		if (resourceId > 0) {
			result = context.getResources().getDimensionPixelSize(resourceId);
		}
		return result;
	}

	public int getActionBarHeight(Context context) {
		TypedValue tv = new TypedValue();
		int actionBarHeight = 0;
		if (context.getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
			actionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data, context.getResources().getDisplayMetrics());
		}
		return actionBarHeight;
	}

    public int getWindowContentHeight(Context context) {
        int screenHeight = getScreenWidth(context).y;
        return screenHeight - getStatusBarHeight(context) - getActionBarHeight(context);
    }
}
