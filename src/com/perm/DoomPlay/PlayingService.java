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


import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.audiofx.AudioEffect;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

public class PlayingService extends Service implements MediaPlayer.OnCompletionListener , SharedPreferences.OnSharedPreferenceChangeListener ,MediaPlayer.OnErrorListener
{
    public final static String actionTrackChanged = "DoomedTrackChanged";
    public final static String actionIconPlay = "DoomedPlayPlay";
    public final static String actionIconPause = "DoomedPlaPause";
    private volatile static MediaPlayer mediaPlayer;
    private static boolean isPrepared ;
    public static int indexCurrentTrack = 0;
    public static boolean isShuffle;
    public static boolean isPlaying;
    public static boolean isLoop;
    final static int nextTrack = 1;
    final static int previousTrack = -1;
    public final static int valueIncredible = 519815;
    final MyBinder binder = new MyBinder();
    public final static int idForeground = 931;
    public final static String actionPlay = "DoomePlay";
    public final static String actionClose = "DoomClose";
    public final static String actionNext = "DoomNext";
    public final static String actionPrevious = "DoomPrevious";
    public final static String actionShuffle = "DoomShuffle";
    public final static String actionLoop = "DoomLooping";
    public static boolean serviceAlive ;
    private static  int trackCountTotal = valueIncredible;
    private static  int trackCountCurrent = 0;
    public final static String actionOffline = "FromlPlayback";
    public static boolean isOnline = false ;
    public static final String actionOnline = "vkOnline";
    private AudioManager audioManager ;
    public static ArrayList<Audio> audios ;
    public static boolean isLoadingTrack;
    private AFListener afListener ;
    private CallListener callListener = new CallListener();
    private OnLoadingTrackListener loadingListener;


    @Override
    public boolean onError(MediaPlayer mp, int what, int extra)
    {
        if(isOnline && !Utils.isOnline(this))
        {
            Toast.makeText(this, "check internet connection", Toast.LENGTH_SHORT).show();
        }
        else
        {
            Toast.makeText(this, "sorry, error", Toast.LENGTH_SHORT).show();
        }
        Log.e("TAG AUDIO","ERROR IN MEDIA PLAYER ,what = "+ what +" ,extra = " + extra);

        handleError();
        return true;
    }





    private void handleError()
    {
        dispose();
        isPlaying = false;
        startNotif();
        sendBroadcast(new Intent(SimpleSWidget.actionUpdateWidget));
        sendBroadcast(new Intent(actionIconPlay));
    }


    interface OnLoadingTrackListener
    {
        void onLoadingTrackStarted();
        void onLoadingTrackEnded();
    }
    void setOnLoadingTrackListener(OnLoadingTrackListener loadingListener)
    {
        this.loadingListener = loadingListener;
    }



    @Override
    public void onCreate()
    {
        super.onCreate();
        isLoadingTrack = false;
        isShuffle = false;
        isPlaying = true;
        isLoop = false;
        afListener = new AFListener();
        ((TelephonyManager)getSystemService(TELEPHONY_SERVICE)).listen(callListener,CallListener.LISTEN_CALL_STATE);
        audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        audioManager.requestAudioFocus(afListener,AudioManager.STREAM_MUSIC,AudioManager.AUDIOFOCUS_GAIN );
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);

        serviceAlive = true;
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        dispose();
        stopForeground(true);
        serviceAlive = false;
        isPlaying = false;
        sendBroadcast(new Intent(actionIconPlay));
        sendBroadcast(new Intent(SimpleSWidget.actionUpdateWidget));
        audioManager.abandonAudioFocus(afListener);
    }
    private void downloadAlbumArt(Audio audio)
    {
        if(SettingActivity.getPreferences("downloadart")&& !AlbumArtGetter.isLoadById(audio.getAid())
                && audio.getTitle() != null && audio.getArtist() != null)
        {
            if((isOnline && SettingActivity.getPreferences("artonline")) || !isOnline)
            {
                new AlbumArtGetter(audio.getAid(), audio.getArtist(), audio.getTitle())
                {
                    @Override
                    protected void onGetBitmap(Bitmap bitmap)
                    {

                    }
                    @Override
                    protected void onBitmapSaved(long albumId)
                    {
                        ArtCacheUtils.add(albumId);

                        sendBroadcast(new Intent(SimpleSWidget.actionUpdateWidget));
                        startNotif();

                        sendBroadcast(new Intent(FullPlaybackActivity.actionDataChanged));
                    }
                }.execute();
            }
        }
    }


    private RemoteViews getNotifViews(int layoutId)
    {
        RemoteViews views = new RemoteViews(getPackageName(), layoutId);

        Audio audio = audios.get(indexCurrentTrack);

        views.setTextViewText(R.id.notifTitle, audio.getTitle());
        views.setTextViewText(R.id.notifArtist, audio.getArtist());

        Bitmap cover = ArtCacheUtils.get(audio.getAid());
        if (cover == null)
        {
            downloadAlbumArt(audio);
            views.setImageViewBitmap(R.id.notifAlbum, BitmapFactory.decodeResource(getResources(), R.drawable.fallback_cover));
        }
        else
        {
            views.setImageViewBitmap(R.id.notifAlbum, cover);
        }


        views.setImageViewResource(R.id.notifPlay, isPlaying ? R.drawable.widget_pause : R.drawable.widget_play);

        ComponentName componentName = new ComponentName(this,PlayingService.class);

        Intent intentPlay = new Intent(actionPlay);
        intentPlay.setComponent(componentName);
        views.setOnClickPendingIntent(R.id.notifPlay, PendingIntent.getService(this, 0, intentPlay, 0));

        Intent intentNext = new Intent(actionNext);
        intentNext.setComponent(componentName);
        views.setOnClickPendingIntent(R.id.notifNext, PendingIntent.getService(this, 0, intentNext, 0));

        Intent intentPrevious = new Intent(actionPrevious);
        intentPrevious.setComponent(componentName);
        views.setOnClickPendingIntent(R.id.notifPrevious, PendingIntent.getService(this, 0, intentPrevious, 0));

        Intent intentClose = new Intent(actionClose);
        intentClose.setComponent(componentName);
        views.setOnClickPendingIntent(R.id.notifClose, PendingIntent.getService(this, 0, intentClose, 0));

        return views;
    }

    private Notification createNotification()
    {

        Intent intentActivity;

        if(SettingActivity.getPreferences(SettingActivity.keyOnClickNotif))
        {
            intentActivity = new Intent(FullPlaybackActivity.actionReturnFull);
            intentActivity.setClass(this,FullPlaybackActivity.class);
            intentActivity.putExtra(FileSystemActivity.keyMusic,audios);
        }
        else
        {
            intentActivity = FullPlaybackActivity.returnSmall(this);
        }
        intentActivity.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        Notification notification = new Notification();
        notification.contentView = getNotifViews(R.layout.notif);
        notification.flags |= Notification.FLAG_FOREGROUND_SERVICE;
        notification.contentIntent = PendingIntent.getActivity(this,0,intentActivity,PendingIntent.FLAG_UPDATE_CURRENT);
        notification.icon =  isPlaying ?  R.drawable.status_icon_pause : R.drawable.status_icon_play;

        return notification;
    }
    private Notification createJellyBeanNotif()
    {
        RemoteViews views = getNotifViews(R.layout.notif_jelly);
        Notification notification = createNotification();
        views.setTextViewText(R.id.textNotifCount,String.valueOf(indexCurrentTrack + 1)+ "/" +String.valueOf(audios.size()));

        notification.bigContentView = views;
        notification.priority = Notification.PRIORITY_MAX;
        return notification;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        String action = intent.getAction();

        if(action.equals(actionOffline) || action.equals(actionOnline))
        {
            isOnline = action.equals(actionOnline);

            audios = intent.getParcelableArrayListExtra(FullPlaybackActivity.keyService);

            if(intent.getIntExtra(FullPlaybackActivity.keyIndex,0) != valueIncredible)
                indexCurrentTrack = intent.getIntExtra(FullPlaybackActivity.keyIndex,0);

            loadMusic();
        }
        else
            handleNotifControll(action);

        return START_NOT_STICKY;
    }

    private void startNotif()
    {
        if(!MainScreenActivity.isJellyBean )
            startForeground(idForeground, createNotification());
        else
            startForeground(idForeground,createJellyBeanNotif());
    }

    private void handleNotifControll(String action)
    {
        if(!PlayingService.isLoadingTrack)
        {
            if(mediaPlayer == null)
            {
                if(audios != null)
                {
                    Intent intent = new Intent(this,PlayingService.class);
                    intent.putExtra(FullPlaybackActivity.keyIndex,indexCurrentTrack);
                    intent.setAction(isOnline ? actionOnline : actionOffline);
                    intent.putExtra(FullPlaybackActivity.keyService,audios);

                    startService(intent);
                }

            }
            else if (action.equals(actionPlay))
            {
                playPause();

            }
            else if (action.equals(actionClose))
            {
                stopSelf();
            }
            else if (action.equals(actionPrevious))
            {
                previousSong();
            }
            else if (action.equals(actionNext))
            {
                nextSong();
            }
            else if(action.equals(actionShuffle))
            {
                setShuffle();
            }
            else if(action.equals(actionLoop))
            {
                setLoop();
            }
        }
    }



    private void loadMusic()
    {

        dispose();

        ArtCacheUtils.add(audios.get(indexCurrentTrack).getAid());

        sendBroadcast(new Intent(actionTrackChanged));
        startNotif();
        sendBroadcast(new Intent(SimpleSWidget.actionUpdateWidget));
        if(isPlaying)
            sendBroadcast(new Intent(actionIconPause));
        else
            sendBroadcast(new Intent(actionIconPlay));

        mediaPlayer = new MediaPlayer();

        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnErrorListener(this);
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        new AsyncTask<Void,Void,Void>()
        {
            @Override
            protected void onPreExecute()
            {
                super.onPreExecute();
                isLoadingTrack = true;

                if(loadingListener != null)
                    loadingListener.onLoadingTrackStarted();
            }

            @Override
            protected Void doInBackground(Void... params)
            {

                try
                {
                    mediaPlayer.setDataSource(audios.get(indexCurrentTrack).getUrl());
                    mediaPlayer.prepare();

                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid)
            {
                super.onPostExecute(aVoid);
                isLoadingTrack = false;
                if(loadingListener != null)
                    loadingListener.onLoadingTrackEnded();

                isPrepared = true;
                if(isPlaying)
                {
                    mediaPlayer.start();
                }
                if(!MainScreenActivity.isOldSDK)
                    notifyEqualizer();
            }
        }.execute();


    }
   private void notifyEqualizer()
    {
        Intent intentEqualizer = new Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION);
        intentEqualizer.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, getAudioSessionId());
        intentEqualizer.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, getPackageName());
        sendBroadcast(intentEqualizer);
    }

    public void setLoop()
    {
        isLoop = !isLoop;
        sendBroadcast(new Intent(SimpleSWidget.actionUpdateWidget));
    }

    void nextSong()
    {
        setTrack(nextTrack);
        loadMusic();
    }

    void previousSong()
    {
        setTrack(previousTrack);
        loadMusic();
    }
    void playTrackFromList(int position)
    {
        indexCurrentTrack = position;
        loadMusic();
    }

    public static void setTrack(int direction)
    {

        if(isShuffle)
        {
            Random random = new Random();
            indexCurrentTrack = random.nextInt(audios.size()-1);

        }
        else if(!isLoop)
            changeTrack(direction);

    }
    public static void changeTrack(int direction)
    {
        switch (direction)
        {
            case previousTrack:
            {
                indexCurrentTrack--;
                if(indexCurrentTrack  == -1 )
                    indexCurrentTrack = audios.size()-1;
                break;
            }
            case nextTrack:
            {
                indexCurrentTrack++;
                if(indexCurrentTrack > audios.size()-1)
                    indexCurrentTrack = 0;

                break;
            }
        }
    }

    public void setShuffle()
    {
        isShuffle = !isShuffle;
        sendBroadcast(new Intent(SimpleSWidget.actionUpdateWidget));
    }
    int getDuration()
    {
        if(isPrepared)
        {
            return mediaPlayer.getDuration();
        }
        else
            return 0;
    }
    boolean isNull()
    {
        return mediaPlayer == null;
    }

    int getCurrentPosition()
    {
        if(isPrepared)
        {
             return mediaPlayer.getCurrentPosition();
        }
        else
            return 0;
    }
    void playPause()
    {
        if(isPlaying)
        {
            isPlaying = false;
            mediaPlayer.pause();
            sendBroadcast(new Intent(actionIconPlay));
        }
        else
        {
            isPlaying = true;
            mediaPlayer.start();
            sendBroadcast(new Intent(actionIconPause));
        }
        startNotif();
        sendBroadcast(new Intent(SimpleSWidget.actionUpdateWidget));
    }
    void setCurrentPosition(int positionMillis)
    {
        mediaPlayer.seekTo(positionMillis);
    }
    @Override
    public void onCompletion(MediaPlayer mp)
    {
        countTimer();
        nextSong();
    }
    private void dispose()
    {
        if(mediaPlayer!= null)
        {
            mediaPlayer.stop();
            isPrepared = false;
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
    private void countTimer()
    {
        if(trackCountTotal != valueIncredible)
        {
            trackCountCurrent++;
            if(trackCountCurrent == trackCountTotal)
                sendBroadcast(new Intent(AbstractReceiver.actionKill));
            trackCountTotal = PlayingService.valueIncredible;
        }
    }


    public static void setSleepTrack(int tracksCount)
    {
        trackCountCurrent = 0;
        trackCountTotal = tracksCount;
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return binder;
    }

    public int getAudioSessionId()
    {
        if(mediaPlayer != null)
            return mediaPlayer.getAudioSessionId();
        else
            return 0;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
    {
         if(PlayingService.serviceAlive && key.equals(SettingActivity.keyOnClickNotif) && audios != null)
             startNotif();
    }

    class MyBinder extends Binder
    {
        PlayingService getService()
        {
            return PlayingService.this;
        }
    }

    private class CallListener extends PhoneStateListener
    {
        boolean wasPlaying = false;

        @Override
        public void onCallStateChanged(int state, String incomingNumber)
        {
            if((TelephonyManager.CALL_STATE_RINGING == state && isPlaying) ||
                    (TelephonyManager.CALL_STATE_OFFHOOK == state && isPlaying))
            {
                if(SettingActivity.getPreferences(SettingActivity.keyOnCall))
                    playPause();
                wasPlaying = true;
            }
            else if(TelephonyManager.CALL_STATE_IDLE == state && wasPlaying && !isPlaying)
            {
                if(SettingActivity.getPreferences(SettingActivity.keyAfterCall))
                    playPause();
                wasPlaying = false;
            }
        }
    }
    private class AFListener implements AudioManager.OnAudioFocusChangeListener
    {
        boolean wasPlaying = false;

        @Override
        public void onAudioFocusChange(int focusChange)
        {
            switch (focusChange)
            {
                case AudioManager.AUDIOFOCUS_LOSS:
                    if(SettingActivity.getPreferences(SettingActivity.keyLongFocus)&& isPlaying)
                        playPause();
                    wasPlaying = true;

                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:

                    if(SettingActivity.getPreferences(SettingActivity.keyShortFocus) && isPlaying)
                        playPause();
                    wasPlaying = true;

                    break;
                case AudioManager.AUDIOFOCUS_GAIN:

                    if(SettingActivity.getPreferences(SettingActivity.keyOnGain) && wasPlaying && !isPlaying)
                        playPause();
                    wasPlaying = false;

                    break;
            }
        }
    }
}
