package tech.boshkov.lab03.Data;

import com.android.volley.Cache;
import com.android.volley.Network;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HurlStack;

/**
 * Created by user on 12/7/16.
 */
public class RequestManager {
    private static RequestManager ourInstance = new RequestManager();

    public static RequestManager getInstance() {
        return ourInstance;
    }
    RequestQueue mRequestQueue;

    private RequestManager() {

    }
}
