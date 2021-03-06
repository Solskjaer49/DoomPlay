package com.perm.DoomPlay;


/*
 *    Copyright 2013 Vladislav Krot
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *    You can contact me <DoomPlaye@gmail.com>
 */
import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

/*
Prevent illegalStateException (doesn't response if track didn't load)
 */

public class CustomViewPager extends ViewPager
{
    private boolean isBlocked;

    public void setBlocked(boolean isBlocked) {
        this.isBlocked = isBlocked;
    }

    public CustomViewPager(Context context)
    {
        super(context);
        isBlocked = false;
    }

    public CustomViewPager(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev)
    {
        try
        {
            return !isBlocked && super.onTouchEvent(ev);
        }
        catch (IllegalArgumentException e)
        {
            return false;
        }

    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev)
    {
        return !isBlocked && super.onInterceptTouchEvent(ev);
    }
}