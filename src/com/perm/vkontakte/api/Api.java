package com.perm.vkontakte.api;

import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.zip.GZIPInputStream;

public class Api {
    static final String TAG="Kate.Api";
    
    public static final String BASE_URL="https://api.vk.com/method/";
    
    public Api(String access_token, String api_id){
        this.access_token=access_token;
        this.api_id=api_id;
    }
    
    String access_token;
    String api_id;
    
    //TODO: it's not faster, even slower on slow devices. Maybe we should add an option to disable it. It's only good for paid internet connection.
    static boolean enable_compression=true;
    
    /*** utils methods***/
    private void checkError(JSONObject root, String url) throws JSONException,KException {
        if(!root.isNull("error")){
            JSONObject error=root.getJSONObject("error");
            int code=error.getInt("error_code");
            String message=error.getString("error_msg");
            KException e = new KException(code, message, url); 
            if (code==14) {
                e.captcha_img = error.optString("captcha_img");
                e.captcha_sid = error.optString("captcha_sid");
            }
            throw e;
        }
        if(!root.isNull("execute_errors")){
            JSONArray errors=root.getJSONArray("execute_errors");
            if(errors.length()==0)
                return;
            //only first error is processed if there are multiple
            JSONObject error=errors.getJSONObject(0);
            int code=error.getInt("error_code");
            String message=error.getString("error_msg");
            KException e = new KException(code, message, url); 
            if (code==14) {
                e.captcha_img = error.optString("captcha_img");
                e.captcha_sid = error.optString("captcha_sid");
            }
            throw e;
        }
    }
    
    private JSONObject sendRequest(Params params) throws IOException, MalformedURLException, JSONException, KException {
        return sendRequest(params, false);
    }
    
    private final static int MAX_TRIES=3;
    private JSONObject sendRequest(Params params, boolean is_post) throws IOException, MalformedURLException, JSONException, KException {
        String url = getSignedUrl(params, is_post);
        String body="";
        if(is_post)
            body=params.getParamsString();

        String response="";
        for(int i=1;i<=MAX_TRIES;++i){
            try{

                response = sendRequestInternal(url, body, is_post);
                break;
            }catch(javax.net.ssl.SSLException ex){
                processNetworkException(i, ex);
            }catch(java.net.SocketException ex){
                processNetworkException(i, ex);
            }
        }
        Log.i(TAG, "response="+response);
        JSONObject root=new JSONObject(response);
        checkError(root, url);
        return root;
    }

    private void processNetworkException(int i, IOException ex) throws IOException {
        ex.printStackTrace();
        if(i==MAX_TRIES)
            throw ex;
    }

    private String sendRequestInternal(String url, String body, boolean is_post) throws IOException, MalformedURLException, WrongResponseCodeException {
        HttpURLConnection connection=null;
        try{
            connection = (HttpURLConnection)new URL(url).openConnection();
            connection.setConnectTimeout(30000);
            connection.setReadTimeout(30000);
            connection.setUseCaches(false);
            connection.setDoOutput(is_post);
            connection.setDoInput(true);
            connection.setRequestMethod(is_post?"POST":"GET");
            if(enable_compression)
                connection.setRequestProperty("Accept-Encoding", "gzip");
            if(is_post)
                connection.getOutputStream().write(body.getBytes("UTF-8"));
            int code=connection.getResponseCode();
            Log.i(TAG, "code="+code);
            //It may happen due to keep-alive problem http://stackoverflow.com/questions/1440957/httpurlconnection-getresponsecode-returns-1-on-second-invocation
            if (code==-1)
                throw new WrongResponseCodeException("Network error");
            //может стоит проверить на код 200
            //on error can also read error stream from connection.
            InputStream is = new BufferedInputStream(connection.getInputStream(), 8192);
            String enc=connection.getHeaderField("Content-Encoding");
            if(enc!=null && enc.equalsIgnoreCase("gzip"))
                is = new GZIPInputStream(is);
            String response=Utils.convertStreamToString(is);
            return response;
        }
        finally{
            if(connection!=null)
                connection.disconnect();
        }
    }
    
    private String getSignedUrl(Params params, boolean is_post) {
        params.put("access_token", access_token);
        
        String args = "";
        if(!is_post)
            args=params.getParamsString();

        /*
        for(int i = 0 ; i < 10 ; i ++)
            Log.d("TAGGGGG", BASE_URL+params.method_name+"?"+args );
        */

        return BASE_URL+params.method_name+"?"+args;
    }
    
    public static String unescape(String text){
        if(text==null)
            return null;
        return text.replace("&amp;", "&").replace("&quot;", "\"").replace("<br>", "\n").replace("&gt;", ">").replace("&lt;", "<")
        .replace("&#39;", "'").replace("<br/>", "\n").replace("&ndash;","-").replace("&#33;", "!").trim();
        //возможно тут могут быть любые коды после &#, например были: 092 - backslash \
    }
    
    public static String unescapeWithSmiles(String text){
        return unescape(text)
                //May be useful to someone
                //.replace("\uD83D\uDE0A", ":-)")
                //.replace("\uD83D\uDE03", ":D")
                //.replace("\uD83D\uDE09", ";-)")
                //.replace("\uD83D\uDE06", "xD")
                //.replace("\uD83D\uDE1C", ";P")
                //.replace("\uD83D\uDE0B", ":p")
                //.replace("\uD83D\uDE0D", "8)")
                //.replace("\uD83D\uDE0E", "B)")
                //
                //.replace("\ud83d\ude12", ":(")  //F0 9F 98 92
                //.replace("\ud83d\ude0f", ":]")  //F0 9F 98 8F
                //.replace("\ud83d\ude14", "3(")  //F0 9F 98 94
                //.replace("\ud83d\ude22", ":'(")  //F0 9F 98 A2
                //.replace("\ud83d\ude2d", ":_(")  //F0 9F 98 AD
                //.replace("\ud83d\ude29", ":((")  //F0 9F 98 A9
                //.replace("\ud83d\ude28", ":o")  //F0 9F 98 A8
                //.replace("\ud83d\ude10", ":|")  //F0 9F 98 90
                //                           
                //.replace("\ud83d\ude0c", "3)")  //F0 9F 98 8C
                //.replace("\ud83d\ude20", ">(")  //F0 9F 98 A0
                //.replace("\ud83d\ude21", ">((")  //F0 9F 98 A1
                //.replace("\ud83d\ude07", "O:)")  //F0 9F 98 87
                //.replace("\ud83d\ude30", ";o")  //F0 9F 98 B0
                //.replace("\ud83d\ude32", "8o")  //F0 9F 98 B2
                //.replace("\ud83d\ude33", "8|")  //F0 9F 98 B3
                //.replace("\ud83d\ude37", ":X")  //F0 9F 98 B7
                //                           
                //.replace("\ud83d\ude1a", ":*")  //F0 9F 98 9A
                //.replace("\ud83d\ude08", "}:)")  //F0 9F 98 88
                //.replace("\u2764", "<3")  //E2 9D A4   
                //.replace("\ud83d\udc4d", ":like:")  //F0 9F 91 8D
                //.replace("\ud83d\udc4e", ":dislike:")  //F0 9F 91 8E
                //.replace("\u261d", ":up:")  //E2 98 9D   
                //.replace("\u270c", ":v:")  //E2 9C 8C   
                //.replace("\ud83d\udc4c", ":ok:")  //F0 9F 91 8C
                ;
    }

    /*** API methods ***/
    //http://vk.com/dev/database.getCities


    <T> String arrayToString(Collection<T> items) {
        if(items==null)
            return null;
        String str_cids = "";
        for (Object item:items){
            if(str_cids.length()!=0)
                str_cids+=',';
            str_cids+=item;
        }
        return str_cids;
    }
    

    public ArrayList<User> getProfiles(Collection<Long> uids, Collection<String> domains, String fields, String name_case, String captcha_key, String captcha_sid) throws MalformedURLException, IOException, JSONException, KException{
        if (uids == null && domains == null)
            return null;
        if ((uids != null && uids.size() == 0) || (domains != null && domains.size() == 0))
            return null;
        Params params = new Params("users.get");
        if (uids != null && uids.size() > 0)
            params.put("uids",arrayToString(uids));
        if (domains != null && domains.size() > 0)
            params.put("uids",arrayToString(domains));
        if (fields == null)
            params.put("fields","uid,first_name,last_name,nickname,domain,sex,bdate,city,country,timezone,photo,photo_medium_rec,photo_big,has_mobile,rate,contacts,education,online");
        else
            params.put("fields",fields);
        params.put("name_case",name_case);
        addCaptchaParams(captcha_key, captcha_sid, params);
        JSONObject root = sendRequest(params);
        JSONArray array=root.optJSONArray("response");
        return User.parseUsers(array);
    }

    /*** methods for friends ***/
    //http://vk.com/dev/friends.get
    public ArrayList<User> getFriends(Long user_id) throws MalformedURLException, IOException, JSONException, KException{
        Params params = new Params("friends.get");
        params.put("fields","first_name,last_name,photo_medium");
        params.put("uid",user_id);
        params.put("order","hints");
        
        //addCaptchaParams(captcha_key, captcha_sid, params);
        JSONObject root = sendRequest(params);
        ArrayList<User> users=new ArrayList<User>();
        JSONArray array=root.optJSONArray("response");
        //if there are no friends "response" will not be array
        if(array==null)
            return users;
        int category_count=array.length();
        for(int i=0; i<category_count; ++i){
            JSONObject o = (JSONObject)array.get(i);
            User u = User.parse(o);
            users.add(u);
        }
        return users;
    }
    
    //http://vk.com/dev/photos.get
    public ArrayList<Photo> getPhotos(Long uid, Long aid, Integer offset, Integer count) throws MalformedURLException, IOException, JSONException, KException{
        Params params = new Params("photos.get");
        if(uid>0)
            params.put("uid", uid);
        else
            params.put("gid", -uid);
        params.put("aid", aid);
        params.put("extended", "1");
        params.put("offset",offset);
        params.put("limit",count);
        params.put("v","4.1");
        JSONObject root = sendRequest(params);
        JSONArray array = root.optJSONArray("response");
        if (array == null)
            return new ArrayList<Photo>(); 
        ArrayList<Photo> photos = parsePhotos(array);
        return photos;
    }
    
    //http://vk.com/dev/photos.getUserPhotos
    public ArrayList<Photo> getUserPhotos(Long uid, Integer offset, Integer count) throws MalformedURLException, IOException, JSONException, KException{
        Params params = new Params("photos.getUserPhotos");
        params.put("uid", uid);
        params.put("sort","0");
        params.put("count",count);
        params.put("offset",offset);
        params.put("extended",1);
        JSONObject root = sendRequest(params);
        JSONArray array = root.optJSONArray("response");
        if (array == null)
            return new ArrayList<Photo>(); 
        ArrayList<Photo> photos = parsePhotos(array);
        return photos;
    }
    private ArrayList<Photo> parsePhotos(JSONArray array) throws JSONException {
        ArrayList<Photo> photos=new ArrayList<Photo>();
        int category_count=array.length();
        for(int i=0; i<category_count; ++i){
            //in getUserPhotos first element is integer
            if(array.get(i) instanceof JSONObject == false)
                continue;
            JSONObject o = (JSONObject)array.get(i);
            Photo p = Photo.parse(o);
            photos.add(p);
        }
        return photos;
    }

    //http://vk.com/dev/photos.getAll
    public ArrayList<Photo> getAllPhotos(Long owner_id, Integer offset, Integer count, boolean extended) throws MalformedURLException, IOException, JSONException, KException{
        Params params = new Params("photos.getAll");
        params.put("owner_id", owner_id);
        params.put("offset", offset);
        params.put("count",count);
        params.put("extended",extended?1:0);
        JSONObject root = sendRequest(params);
        JSONArray array = root.optJSONArray("response");
        if (array == null)
            return new ArrayList<Photo>(); 
        ArrayList<Photo> photos = parsePhotos(array);
        return photos;
    }

    
    private void addCaptchaParams(String captcha_key, String captcha_sid, Params params) {
        params.put("captcha_sid",captcha_sid);
        params.put("captcha_key",captcha_key);
    }


    /*** for status***/
    //http://vk.com/dev/status.get
    public VkStatus getStatus(Long uid) throws MalformedURLException, IOException, JSONException, KException{
        Params params = new Params("status.get");
        params.put("uid", uid);
        JSONObject root = sendRequest(params);
        JSONObject obj = root.optJSONObject("response");
        VkStatus status = new VkStatus();
        if (obj != null) {
            status.text = unescape(obj.getString("text"));
            JSONObject jaudio = obj.optJSONObject("audio");
            if (jaudio != null) 
                status.audio = Audio.parse(jaudio);
        }
        return status;
    }

    //http://vk.com/dev/status.set
    public String setStatus(String status_text, String audio) throws MalformedURLException, IOException, JSONException, KException{
        Params params = new Params("status.set");
        params.put("text", status_text);
        params.put("audio", audio); //oid_aid
        JSONObject root = sendRequest(params);
        Object response_id = root.opt("response");
        if (response_id != null)
            return String.valueOf(response_id);
        return null;
    }



    /*** for audio ***/
    //http://vk.com/dev/audio.get
    public ArrayList<Audio> getAudio(Long uid, Long gid, Long album_id,int count) throws MalformedURLException, IOException, JSONException, KException{
        Params params = new Params("audio.get");
        params.put("uid", uid);
        params.put("gid", gid);
        params.put("album_id", album_id);
        params.put("count", count);
        //addCaptchaParams(captcha_key, captcha_sid, params);
        JSONObject root = sendRequest(params);
        JSONArray array = root.optJSONArray("response");
        return parseAudioList(array, 0);
    }


    //http://vk.com/dev/audio.getById
    public ArrayList<Audio> getAudioById(String audios, String captcha_key, String captcha_sid) throws MalformedURLException, IOException, JSONException, KException{
        Params params = new Params("audio.getById");
        params.put("audios", audios);
        addCaptchaParams(captcha_key, captcha_sid, params);
        JSONObject root = sendRequest(params);
        JSONArray array = root.optJSONArray("response");
        return parseAudioList(array, 0);
    }
    
    //http://vk.com/dev/audio.getLyrics
    public String getLyrics(Long id) throws MalformedURLException, IOException, JSONException, KException{
        Params params = new Params("audio.getLyrics");
        params.put("lyrics_id", id);
        JSONObject root = sendRequest(params);
        JSONObject response = root.optJSONObject("response");
        return response.optString("text");
    }
    
    /*** for crate album ***/
    //http://vk.com/dev/photos.createAlbum
    public Album createAlbum(String title, Long gid, String privacy, String comment_privacy, String description) throws MalformedURLException, IOException, JSONException, KException {
        Params params = new Params("photos.createAlbum");
        params.put("title", title);
        params.put("gid", gid);
        params.put("privacy", privacy);
        params.put("comment_privacy", comment_privacy);
        params.put("description", description);
        JSONObject root = sendRequest(params);
        JSONObject o = root.optJSONObject("response");
        if (o == null)
            return null; 
        return Album.parse(o);
    }
    
    //http://vk.com/dev/photos.editAlbum
    public String editAlbum(long aid, Long oid, String title, String privacy, String comment_privacy, String description) throws MalformedURLException, IOException, JSONException, KException {
        Params params = new Params("photos.editAlbum");
        params.put("aid", String.valueOf(aid));
        params.put("oid", oid);
        params.put("title", title);
        params.put("privacy", privacy);
        params.put("comment_privacy", comment_privacy);
        params.put("description", description);
        JSONObject root = sendRequest(params);
        Object response_code = root.opt("response");
        if (response_code != null)
            return String.valueOf(response_code);
        return null;
    }

    
    //http://vk.com/dev/audio.getUploadServer
    public String getAudioUploadServer() throws MalformedURLException, IOException, JSONException, KException {
        Params params = new Params("audio.getUploadServer");
        JSONObject root = sendRequest(params);
        JSONObject response = root.getJSONObject("response");
        return response.getString("upload_url");
    }

    //http://vk.com/dev/audio.save
    public Audio saveAudio(String server, String audio, String hash, String artist, String title) throws MalformedURLException, IOException, JSONException, KException {
        Params params = new Params("audio.save");
        params.put("server",server);
        params.put("audio",audio);
        params.put("hash",hash);
        params.put("artist",artist);
        params.put("title",title);
        JSONObject root = sendRequest(params);
        JSONObject response=root.getJSONObject("response");
        return Audio.parse(response);
    }

    
    //http://vk.com/dev/audio.search
    public ArrayList<Audio> searchAudio(String query, int count) throws MalformedURLException, IOException, JSONException, KException{
        Params params = new Params("audio.search");
        params.put("q", query);
        params.put("sort", "2");
        params.put("lyrics", "0");
        params.put("count", count);
        params.put("offset", "0");
        params.put("auto_complete", "1");
        //addCaptchaParams(captcha_key, captcha_sid, params);
        JSONObject root = sendRequest(params);
        JSONArray array = root.optJSONArray("response");
        return parseAudioList(array, 1);
    }

    private ArrayList<Audio> parseAudioList(JSONArray array, int type_array) //type_array must be 0 or 1
            throws JSONException {
        ArrayList<Audio> audios = new ArrayList<Audio>();
        if (array != null) {
            for(int i = type_array; i<array.length(); ++i) { //get(0) is integer, it is audio count
                JSONObject o = (JSONObject)array.get(i);
                audios.add(Audio.parse(o));
            }
        }
        return audios;
    }
    
    //http://vk.com/dev/audio.delete
    public String deleteAudio(Long aid, Long oid) throws MalformedURLException, IOException, JSONException, KException{
        Params params = new Params("audio.delete");
        params.put("aid", aid);
        params.put("oid", oid);
        JSONObject root = sendRequest(params);
        Object response_code = root.opt("response");
        if (response_code != null)
            return String.valueOf(response_code);
        return null;
    }

    //http://vk.com/dev/audio.add
    public String addAudio(Long aid, Long oid) throws MalformedURLException, IOException, JSONException, KException
    {
        Params params = new Params("audio.add");
        params.put("aid", aid);
        params.put("oid", oid);
        //addCaptchaParams(captcha_key, captcha_sid, params);
        JSONObject root = sendRequest(params);
        Object response_code = root.opt("response");
        if (response_code != null)
            return String.valueOf(response_code);
        return null;
    }
    

    
    //http://vk.com/dev/likes.add
    public Long addLike(Long owner_id, Long item_id, String type, String access_key, String captcha_key, String captcha_sid) throws MalformedURLException, IOException, JSONException, KException{
        Params params = new Params("likes.add");
        params.put("owner_id", owner_id);
        params.put("item_id", item_id);
        params.put("type", type);
        params.put("access_key", access_key);
        addCaptchaParams(captcha_key, captcha_sid, params);
        JSONObject root = sendRequest(params);
        JSONObject response = root.optJSONObject("response");
        long likes=response.optLong("likes", -1);
        return likes;
    }
    
    //http://vk.com/dev/likes.delete
    public Long deleteLike(Long owner_id, String type, Long item_id, String captcha_key, String captcha_sid) throws MalformedURLException, IOException, JSONException, KException{
        Params params = new Params("likes.delete");
        params.put("owner_id", owner_id);
        params.put("type", type);
        params.put("item_id", item_id);
        addCaptchaParams(captcha_key, captcha_sid, params);
        JSONObject root = sendRequest(params);
        JSONObject response = root.optJSONObject("response");
        return response.optLong("likes", -1);
    }
    
    //http://vk.com/dev/photos.getById
    public ArrayList<Photo> getPhotosById(String photos, Integer extended, Integer photo_sizes) throws MalformedURLException, IOException, JSONException, KException{
        Params params = new Params("photos.getById");
        params.put("photos", photos);
        params.put("extended", extended);
        params.put("photo_sizes", photo_sizes);
        JSONObject root = sendRequest(params);
        JSONArray array = root.optJSONArray("response");
        if (array == null)
            return new ArrayList<Photo>(); 
        ArrayList<Photo> photos1 = parsePhotos(array);
        return photos1;
    }
    
    //http://vk.com/dev/users.getSubscriptions
    public ArrayList<Long> getSubscriptions(Long uid, int offset, int count, Integer extended) throws MalformedURLException, IOException, JSONException, KException{
        Params params = new Params("users.getSubscriptions");
        params.put("uid", uid);
        //params.put("extended", extended); //TODO
        if (offset>0)
            params.put("offset", offset);
        if (count>0)
            params.put("count", count);
        JSONObject root = sendRequest(params);
        JSONObject response = root.getJSONObject("response");
        JSONObject jusers = response.optJSONObject("users");
        JSONArray array=jusers.optJSONArray("items");
        ArrayList<Long> users = new ArrayList<Long>();
        if (array != null) {
            int category_count=array.length();
            for(int i=0; i<category_count; ++i) {
                Long id = array.optLong(i, -1);
                if(id!=-1)
                    users.add(id);
            }
        }
        return users;
    }
    //http://vk.com/dev/execute
    public void execute(String code) throws MalformedURLException, IOException, JSONException, KException {
        Params params = new Params("execute");
        params.put("code", code);
        sendRequest(params);
    }

    
    //http://vk.com/dev/friends.getLists


    
    private ArrayList<PhotoTag> parsePhotoTags(JSONArray array, Long pid, Long owner_id) throws JSONException {
        ArrayList<PhotoTag> photo_tags=new ArrayList<PhotoTag>(); 
        int category_count=array.length(); 
        for(int i=0; i<category_count; ++i){
            //in getUserPhotos first element is integer
            if(array.get(i) instanceof JSONObject == false)
                continue;
            JSONObject o = (JSONObject)array.get(i);
            PhotoTag p = PhotoTag.parse(o);
            photo_tags.add(p);
            if (pid != null)
                p.pid = pid;
            if (owner_id != null)
                p.owner_id = owner_id;
        }
        return photo_tags;
    }

    //http://vk.com/dev/groups.getById
    public ArrayList<Group> getGroups(Long uid, Integer count) throws MalformedURLException, IOException, JSONException, KException{

        if(count == null)
            count =  Integer.valueOf(600);

        Params params = new Params("groups.get");
        params.put("uid", uid);
        params.put("extended",1);
        params.put("count", count);

        //params.put("fields", fields); //Possible values: place,wiki_page,city,country,description,start_date,finish_date,site,fixed_post
        JSONObject root = sendRequest(params);
        JSONArray array=root.optJSONArray("response");
        return Group.parseGroups(array);
    }


    //http://vk.com/dev/groups.search
    public ArrayList<Group> searchGroup(String q, Long count, Long offset) throws MalformedURLException, IOException, JSONException, KException {
        Params params = new Params("groups.search");
        params.put("q", q);
        params.put("count", count);
        params.put("offset", offset);
        JSONObject root = sendRequest(params);
        JSONArray array=root.optJSONArray("response");
        ArrayList<Group> groups = new ArrayList<Group>();  
        //if there are no groups "response" will not be array
        if (array==null)
            return groups;
        groups = Group.parseGroups(array);
        return groups;
    }
    
    //http://vk.com/dev/account.registerDevice
    public String registerDevice(String token, String device_model, String system_version, Integer no_text, String subscribe)
            throws MalformedURLException, IOException, JSONException, KException {
        Params params = new Params("account.registerDevice");
        params.put("token", token);
        params.put("device_model", device_model);
        params.put("system_version", system_version);
        params.put("no_text", no_text);
        params.put("subscribe", subscribe);
        JSONObject root = sendRequest(params);
        return root.getString("response");
    }
    
    //http://vk.com/dev/account.unregisterDevice
    public String unregisterDevice(String token) throws MalformedURLException, IOException, JSONException, KException {
        Params params = new Params("account.unregisterDevice");
        params.put("token", token);
        JSONObject root = sendRequest(params);
        return root.getString("response");
    }

    
    /*** faves ***/
    //http://vk.com/dev/fave.getUsers
    public ArrayList<User> getFaveUsers(String fields, Integer count, Integer offset) throws MalformedURLException, IOException, JSONException, KException{
        Params params = new Params("fave.getUsers");
        if(fields==null)
            fields="photo_medium,online";
        params.put("fields",fields);
        params.put("count", count);
        params.put("offset", offset);
        JSONObject root = sendRequest(params);
        ArrayList<User> users=new ArrayList<User>();
        JSONArray array=root.optJSONArray("response");
        //if there are no friends "response" will not be array
        if(array==null)
            return users;
        int category_count=array.length();
        for(int i=0; i<category_count; ++i) {
            if(array.get(i)==null || ((array.get(i) instanceof JSONObject)==false))
                continue;
            JSONObject o = (JSONObject)array.get(i);
            User u = User.parseFromFave(o);
            users.add(u);
        }
        return users;
    }
    
    //http://vk.com/dev/fave.getPhotos
    public ArrayList<Photo> getFavePhotos(Integer count, Integer offset) throws MalformedURLException, IOException, JSONException, KException {
        Params params = new Params("fave.getPhotos");
        params.put("count", count);
        params.put("offset", offset);
        JSONObject root = sendRequest(params);
        JSONArray array = root.optJSONArray("response");
        if (array == null)
            return new ArrayList<Photo>(); 
        ArrayList<Photo> photos = parsePhotos(array);
        return photos;
    }


    
    //http://vk.com/dev/groups.getMembers
    public ArrayList<Long> getGroupsMembers(long gid, Integer count, Integer offset, String sort) throws MalformedURLException, IOException, JSONException, KException {
        Params params = new Params("groups.getMembers");
        params.put("gid", gid);
        params.put("count", count);
        params.put("offset", offset);
        params.put("sort", sort); //id_asc, id_desc, time_asc, time_desc
        JSONObject root = sendRequest(params);
        JSONObject response=root.getJSONObject("response");
        JSONArray array=response.optJSONArray("users");
        ArrayList<Long> users=new ArrayList<Long>();
        if (array != null) {
            int category_count=array.length();
            for(int i=0; i<category_count; ++i){
                Long id = array.optLong(i, -1);
                if(id!=-1)
                    users.add(id);
            }
        }
        return users;
    }
    
    //http://vk.com/dev/groups.getMembers
    public Long getGroupsMembersCount(long gid) throws MalformedURLException, IOException, JSONException, KException {
        Params params = new Params("groups.getMembers");
        params.put("gid", gid);
        params.put("count", 10);
        JSONObject root = sendRequest(params);
        JSONObject response=root.getJSONObject("response");
        return response.optLong("count");
    }
    
    public ArrayList<User> getGroupsMembersWithExecute(long gid, Integer count, Integer offset, String sort, String fields) throws MalformedURLException, IOException, JSONException, KException {
        //String code = "return API.getProfiles({\"uids\":API.groups.getMembers({\"gid\":" + String.valueOf(gid) + ",\"count\":" + String.valueOf(count) + ",\"offset\":" + String.valueOf(offset) + ",\"sort\":\"id_asc\"}),\"fields\":\"" + fields + "\"});";
        String code = "var members=API.groups.getMembers({\"gid\":" + gid + "}); var u=members[1]; return API.getProfiles({\"uids\":u,\"fields\":\"" + fields + "\"});";
        Params params = new Params("execute");
        params.put("code", code);
        JSONObject root = sendRequest(params);
        JSONArray array=root.optJSONArray("response");
        return User.parseUsers(array);
    }
    
    //http://vk.com/dev/utils.getServerTime
    public long getServerTime() throws MalformedURLException, IOException, JSONException, KException{
        Params params = new Params("utils.getServerTime");
        JSONObject root = sendRequest(params);
        return root.getLong("response");
    }
    
    //http://vk.com/dev/audio.getAlbums
    public ArrayList<AudioAlbum> getAudioAlbums(Long uid, Long gid, Integer count) throws MalformedURLException, IOException, JSONException, KException{
        Params params = new Params("audio.getAlbums");
        params.put("uid", uid);
        params.put("gid", gid);
        params.put("count", count);
        JSONObject root = sendRequest(params);
        JSONArray array = root.optJSONArray("response");
        ArrayList<AudioAlbum> albums = AudioAlbum.parseAlbums(array);
        return albums;
    }
    
    //http://vk.com/dev/audio.getRecommendations
    public ArrayList<Audio> getAudioRecommendations(int count,long userId) throws MalformedURLException, IOException, JSONException, KException{
        Params params = new Params("audio.getRecommendations");
        params.put("user_id",userId);
        params.put("count",count);
        JSONObject root = sendRequest(params);
        JSONArray array = root.optJSONArray("response");
        return parseAudioList(array, 0);
    }
    
    //http://vk.com/dev/audio.getPopular
    public ArrayList<Audio> getAudioPopular(int genre_id,int count) throws MalformedURLException, IOException, JSONException, KException{
        Params params = new Params("audio.getPopular");
        params.put("genre_id", genre_id);
        params.put("count", count);
        JSONObject root = sendRequest(params);
        JSONArray array = root.optJSONArray("response");
        return parseAudioList(array, 0);
    }

    //gets status of broadcasting user current audio to his page
    public boolean audioGetBroadcast() throws MalformedURLException, IOException, JSONException, KException {
        Params params = new Params("audio.getBroadcast");
        JSONObject root = sendRequest(params);
        JSONObject response = root.optJSONObject("response");
        return response.optInt("enabled")==1;
    }

    //http://vk.com/dev/audio.setBroadcast
    public boolean audioSetBroadcast(boolean enabled) throws MalformedURLException, IOException, JSONException, KException {
        Params params = new Params("audio.setBroadcast");
        params.put("enabled",enabled?"1":"0");
        JSONObject root = sendRequest(params);
        JSONObject response = root.optJSONObject("response");
        return response.optInt("enabled")==1;
    }
    
    //http://vk.com/dev/audio.addAlbum
    public Long addAudioAlbum(String title) throws MalformedURLException, IOException, JSONException, KException {
        Params params = new Params("audio.addAlbum");
        params.put("title", title);
        JSONObject root = sendRequest(params);
        JSONObject obj = root.getJSONObject("response");
        return obj.optLong("album_id");
    }
    
    //http://vk.com/dev/audio.editAlbum
    public Integer editAudioAlbum(String title, long album_id) throws MalformedURLException, IOException, JSONException, KException {
        Params params = new Params("audio.editAlbum");
        params.put("title", title);
        params.put("album_id", album_id);
        JSONObject root = sendRequest(params);
        return root.optInt("response");
    }
    
    //http://vk.com/dev/audio.deleteAlbum
    public Integer deleteAudioAlbum(long album_id) throws MalformedURLException, IOException, JSONException, KException {
        Params params = new Params("audio.deleteAlbum");
        params.put("album_id", album_id);
        JSONObject root = sendRequest(params);
        return root.optInt("response");
    }
    
    //http://vk.com/dev/audio.moveToAlbum
    public Integer moveToAudioAlbum(long album_id,long audioAids) throws MalformedURLException, IOException, JSONException, KException {
        Params params = new Params("audio.moveToAlbum");
        params.put("aid", String.valueOf(album_id));
        params.put("aids",String.valueOf(audioAids));
        JSONObject root = sendRequest(params);
        return root.optInt("response");
    }
}