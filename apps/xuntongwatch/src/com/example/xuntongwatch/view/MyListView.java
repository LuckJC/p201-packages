package com.example.xuntongwatch.view;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.ListView;

public class MyListView extends ListView{

	public MyListView(Context context) {
		super(context);
	}

	public MyListView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public MyListView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	private boolean isScroll = true;
	
	public void setIsScroll(boolean isScroll)
	{
		this.isScroll = isScroll;
	}
	
	// 滑动距离及坐标 
	private float xDistance, yDistance, xLast, yLast;
	
	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) 
	{
		switch (ev.getAction()) 
		{ 
		case MotionEvent.ACTION_DOWN: 
			xDistance = yDistance = 0f; 
			xLast = ev.getX(); 
			yLast = ev.getY(); 
			break; 
		case MotionEvent.ACTION_MOVE: 
			final float curX = ev.getX(); 
			final float curY = ev.getY(); 
	
			xDistance += Math.abs(curX - xLast); 
			yDistance += Math.abs(curY - yLast); 
			xLast = curX; 
			yLast = curY; 

			if(xDistance > yDistance)
			{ 
				return false; 
			} 
			
			if(!isScroll)//表示 不要移动
			{
//				face.setSS(-1);
				return false;
			}
			
		}
		return super.onInterceptTouchEvent(ev);
	}
}
