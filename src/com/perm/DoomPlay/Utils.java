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

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.widget.Toast;

import java.io.File;
import java.util.List;

class Utils
{
    private Utils(){}

    private static String[] EXTENSIONS ;

    static
    {
        if(Build.VERSION.SDK_INT > 11)
        {
             EXTENSIONS = new String[]{ ".mp3",".flac",".m4a",".aac", ".mp4",".wav",".ogg",".midi",".3gp",".ota",".imy",".rtx",".xmf",".mxmf"};
        }
        else
        {
            EXTENSIONS = new String[]{".mp3",".mp4",".wav",".m4a",".ogg",".midi",".3gp",".ota",".imy",".rtx",".xmf",".mxmf"};
        }

    }




    public static boolean trackChecker(String trackToTest)
    {
        for(String ext : EXTENSIONS)
        {
            if(trackToTest.contains(ext))
                return true;
        }
        return false;
    }
    public static String milliSecondsToTimer(long milliseconds)
    {
        String finalTimerString = "";
        String secondsString ;

        int hours = (int)( milliseconds / (1000*60*60));
        int minutes = (int)(milliseconds % (1000*60*60)) / (1000*60);
        int seconds = (int) ((milliseconds % (1000*60*60)) % (1000*60) / 1000);

        if(hours > 0)
        {
            finalTimerString = hours + ":";
        }
        if(seconds < 10)
        {
            secondsString = "0" + seconds;
        }
        else
        {
            secondsString = "" + seconds;
        }

        finalTimerString = finalTimerString + minutes + ":" + secondsString;

        return finalTimerString;
    }
    public static int getProgressPercentage(long currentDuration, long totalDuration)
    {
        Double percentage ;
        long currentSeconds = (int) (currentDuration / 1000);
        long totalSeconds = (int) (totalDuration / 1000);
        percentage =(((double)currentSeconds)/totalSeconds)*100;

        return percentage.intValue();
    }
    public static boolean checkSpecialCharacters(String fileToCheck)
    {
        String[] specialCh = {"!","@","#","$","%","^","&","*","(",")","_","+","-","=","/",">","<"," "};
        for(String c : specialCh)
        {
            if(fileToCheck.contains(c))
                return false;
        }
        return true;
    }
    public static void setRingtone(Context context,Audio audio)
    {

        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DATA, audio.getUrl());
        values.put(MediaStore.MediaColumns.TITLE, audio.getTitle());
        values.put(MediaStore.MediaColumns.SIZE, new File(audio.getUrl()).getTotalSpace());
        values.put(MediaStore.MediaColumns.MIME_TYPE, "audio/*");
        values.put(MediaStore.Audio.Media.ARTIST, audio.getArtist());
        values.put(MediaStore.Audio.Media.DURATION, 5000);
        values.put(MediaStore.Audio.Media.IS_RINGTONE, true);
        values.put(MediaStore.Audio.Media.IS_NOTIFICATION, false);
        values.put(MediaStore.Audio.Media.IS_ALARM, false);
        values.put(MediaStore.Audio.Media.IS_MUSIC, false);

        Uri uri = MediaStore.Audio.Media.getContentUriForPath(audio.getUrl());
        Uri newUri = context.getContentResolver().insert(uri, values);
        RingtoneManager.setActualDefaultRingtoneUri(context,RingtoneManager.TYPE_RINGTONE,newUri);


        Toast.makeText(context,"track was set as ringtone",Toast.LENGTH_SHORT).show();
    }
    public static boolean isOnline(Context context)
    {
        final NetworkInfo netInfo = ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();

        return (netInfo != null && netInfo.isConnected());
    }

    public static boolean isIntentAvailable(Context context, Intent intent)
    {
        PackageManager packageManager = context.getPackageManager();
        List<ResolveInfo> list = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        return list.size() > 0;
    }

}
