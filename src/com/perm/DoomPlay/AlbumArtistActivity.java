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

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.view.ActionMode;
import android.view.*;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

public class AlbumArtistActivity extends AbstractReceiver
{

    private static String[] albumArtist;

    public final static String actionPlayArtist ="action.list.playArtist";
    public final static String actionPlayAlbum = "action.list.playAlbum";
    private static String currentAction = null;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        currentAction = getIntent().getAction();

        setContentView(R.layout.list_album_artist);
        ListView listView = (ListView) findViewById(R.id.listAlbumArtist);


        if(currentAction.equals(actionPlayArtist))
            albumArtist = TracksHolder.allArtist;
        else
            albumArtist = TracksHolder.allAlbums;

        if(albumArtist == null)
            albumArtist = new String[0];

        AlbumArtistAdapter adapter = new AlbumArtistAdapter(albumArtist);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(onClickAlbumArtist);
        listView.setOnItemLongClickListener(onLongClickAlbumArtist);
    }

    private final AdapterView.OnItemClickListener onClickAlbumArtist = new AdapterView.OnItemClickListener()
    {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id)
        {
            if(currentAction.equals(actionPlayArtist))
            {
                onClickOpen(position,false);
            }
            else
            {
                onClickOpen(position, true);
            }
        }
    };
    private final AdapterView.OnItemLongClickListener onLongClickAlbumArtist = new AdapterView.OnItemLongClickListener()
    {
        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id)
        {

            startSupportActionMode(callback).setTag(position);
            return true;
        }
    };

    void onClickOpen(int position,boolean isAlbum)
    {
        Intent intent = new Intent(getBaseContext(),ListTracksActivity.class);
        intent.setAction(ListTracksActivity.actionJust);
        intent.putExtra(MainScreenActivity.keyOpenInListTrack, getTracksFromAlbumArtist(position, isAlbum));
        startActivity(intent);
    }
    ArrayList<Audio> getTracksFromAlbumArtist(int position ,boolean fromAlbum)
    {
        //TODO:java.lang.IllegalArgumentException: the bind value at index 1 is null
        Cursor cursor;

        if(fromAlbum)
            cursor = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, TracksHolder.projection,
                MediaStore.Audio.Media.ALBUM + " = ?",new String[]{albumArtist[position]}, null);
        else
            cursor = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,  TracksHolder.projection,
                    MediaStore.Audio.Media.ARTIST+ " = ?", new String[]{albumArtist[position]}, null);


        ArrayList<Audio> result = Audio.parseAudiosCursor(cursor);

        cursor.close();

        return result;
    }

    private final ActionMode.Callback  callback = new ActionMode.Callback()
    {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu)
        {
            getMenuInflater().inflate(R.menu.action_album_artist,menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu){return false; }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item)
        {
            int position =(Integer) mode.getTag();

            switch(item.getItemId())
            {
                case R.id.itemPlayAll:
                {
                    if(currentAction.equals(actionPlayAlbum))
                    {
                        startActivity(FileSystemActivity.getToFullIntent(getBaseContext(),getTracksFromAlbumArtist(position,true)));
                    }
                    else
                    {
                        startActivity(FileSystemActivity.getToFullIntent(getBaseContext(),getTracksFromAlbumArtist(position,false)));
                    }

                    break;
                }
                case R.id.itemToPlaylist:
                {
                    if(currentAction.equals(actionPlayAlbum))
                        showPlaybackDialog(getTracksFromAlbumArtist(position, true));
                    else
                        showPlaybackDialog(getTracksFromAlbumArtist(position, false));

                    break;
                }
            }
            mode.finish();
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode){}
    };
    class AlbumArtistAdapter extends BaseAdapter
    {
        final String[] artistAlbums;
        final LayoutInflater inflater;
        public AlbumArtistAdapter(String[] artistAlbums)
        {
            this.artistAlbums = artistAlbums;
            inflater = (LayoutInflater)getBaseContext().getSystemService(LAYOUT_INFLATER_SERVICE);
        }
        @Override
        public int getCount()
        {
            return artistAlbums.length;
        }

        @Override
        public Object getItem(int position)
        {
            return artistAlbums[position];
        }

        @Override
        public long getItemId(int position)
        {
            return position;
        }


        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            View view;
            if(!currentAction.equals(actionPlayArtist))
            {
                view = inflater.inflate(R.layout.item_album,parent,false);
                ((TextView)view.findViewById(R.id.textOnlyArtist)).setText(TracksHolder.getArtistFromAlbum(position));
            }
            else
            {
                view = inflater.inflate(R.layout.item_artist,parent,false);
            }

            TextView textAlbumArtist = (TextView)view.findViewById(R.id.textAlbumArtist);
            textAlbumArtist.setText(artistAlbums[position]);

            return view;
        }
    }
}