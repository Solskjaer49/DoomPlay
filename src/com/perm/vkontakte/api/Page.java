package com.perm.vkontakte.api;

import java.io.Serializable;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Wiki page
 *
 */
public class Page implements Serializable{
    private static final long serialVersionUID = 1L;
    public long id;
    public long group_id;
    public String title;
    
    public static Page parseFromAttachment(JSONObject o) throws NumberFormatException, JSONException{
        Page page = new Page();
        page.title = Api.unescape(o.optString("title"));
        page.id = o.optLong("pid");
        page.group_id = o.optLong("gid");
        return page;
    }
}