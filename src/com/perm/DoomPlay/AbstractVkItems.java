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
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;
import com.api.KException;
import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;

abstract class AbstractVkItems extends AbstractReceiver
{
    static boolean isLoading;
    LinearLayout linearLoading;
    ListView listView;
    public final static String keyLinerLoadingRestore = "keyLinerLoading";

    protected abstract void onClickRefresh();
    protected abstract ArrayList<Audio> getAudios(int position) throws KException,JSONException,IOException;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.itemRefresh:
                if(isLoading)
                    AbstractList.waitMessage(getBaseContext());
                if(!MainScreenActivity.isRegister)
                    Toast.makeText(getBaseContext(),getResources().getString(R.string.please_sign_in),Toast.LENGTH_SHORT).show();
                else
                    onClickRefresh();
                return true;
            case R.id.itemInterrupt:
                cancelLoading();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
    private TaskLoader taskLoader;

    @Override
    public void onBackPressed()
    {
        super.onBackPressed();
        cancelLoading();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        cancelLoading();
    }

    final AdapterView.OnItemClickListener onClickListener = new AdapterView.OnItemClickListener()
    {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id)
        {
            if(isLoading)
                AbstractList.waitMessage(getBaseContext());
            else if(!MainScreenActivity.isRegister)
                Toast.makeText(getBaseContext(),getResources().getString(R.string.please_sign_in),Toast.LENGTH_SHORT).show();
            else  if(!Utils.isOnline(getBaseContext()))
                Toast.makeText(getBaseContext(), getResources().getString(R.string.check_internet), Toast.LENGTH_SHORT).show();
            else
            {
                taskLoader = new TaskLoader();
                taskLoader.execute(position);
            }
        }

    };

    void cancelLoading()
    {
        if(isLoading && taskLoader != null)
        {
            taskLoader.cancel(true);
        }
    }
    void startListVkActivity(ArrayList<Audio> audios)
    {
        Intent intent = new Intent(this,ListVkActivity.class);
        intent.setAction(ListVkActivity.actionJust);
        intent.putExtra(MainScreenActivity.keyOpenInListTrack,audios);
        startActivity(intent);
    }
    void setLoading()
    {
         if(isLoading)
         {
             isLoading = false;
             linearLoading.setVisibility(View.GONE);
         }
         else
         {
             linearLoading.setVisibility(View.VISIBLE);
             isLoading = true;
         }
    }



    private class TaskLoader extends AsyncTask<Integer,Void,ArrayList<Audio>>
    {
        @Override
        protected void onPreExecute()
        {
            super.onPreExecute();
            setLoading();
        }

        @Override
        protected ArrayList<Audio> doInBackground(Integer... params)
        {
            try
            {
                return getAudios(params[0]);
            }
            catch (KException e)
            {
                handleKException(e);
                cancel(false);
            } catch (JSONException e)
            {
                showException(e);
                cancel(false);
            } catch (IOException e)
            {
                showException(e);
                cancel(false);
            }
            return null;
        }

        @Override
        protected void onCancelled()
        {
            super.onCancelled();
            isLoading = false;
            linearLoading.setVisibility(View.GONE);
        }

        @Override
        protected void onPostExecute(ArrayList<Audio> audios)
        {
            super.onPostExecute(audios);
            setLoading();

            if(audios == null)
                Toast.makeText(getBaseContext(),getResources().getString(R.string.cant_get_audio),Toast.LENGTH_SHORT).show();
            else
                startListVkActivity(audios);
        }
    }
}
