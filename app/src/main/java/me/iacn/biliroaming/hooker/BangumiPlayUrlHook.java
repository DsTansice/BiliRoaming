package me.iacn.biliroaming.hooker;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;

import de.robv.android.xposed.XC_MethodHook;
import me.iacn.biliroaming.network.BiliRoamingApi;
import me.iacn.biliroaming.network.StreamUtils;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

/**
 * Created by iAcn on 2019/3/29
 * Email i@iacn.me
 */
public class BangumiPlayUrlHook extends BaseHook {

    private static final String TAG = "BiliRoaming";

    public BangumiPlayUrlHook(ClassLoader classLoader) {
        super(classLoader);
    }

    @Override
    public void startHook() {
        Log.d(TAG, "startHook: BangumiPlayUrl");
        findAndHookMethod("com.bilibili.lib.okhttp.huc.OkHttpURLConnection", mClassLoader,
                "getInputStream", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        // Found from "b.ecy" in version 5.39.1
                        HttpURLConnection connection = (HttpURLConnection) param.thisObject;
                        String urlString = connection.getURL().toString();

                        if (urlString.startsWith("https://api.bilibili.com/pgc/player/api/playurl")) {
                            String queryString = urlString.substring(urlString.indexOf("?") + 1);
                            if (queryString.contains("module=bangumi")) {
                                InputStream inputStream = (InputStream) param.getResult();
                                String encoding = connection.getContentEncoding();
                                String content = StreamUtils.getContent(inputStream, encoding);

                                if (isLimitWatchingArea(content)) {
                                    Log.d(TAG, "Limited Play Url: queryString = " + queryString);
                                    content = BiliRoamingApi.getPlayUrl(queryString);
                                    Log.d(TAG, "Has replaced play url with proxy server");
                                }

                                param.setResult(new ByteArrayInputStream(content.getBytes()));
                            }
                        }
                    }
                });
    }

    private boolean isLimitWatchingArea(String jsonText) {
        try {
            JSONObject json = new JSONObject(jsonText);
            int code = json.optInt("code");
            Log.d(TAG, "PlayUrlInformation: code = " + code);

            return code == -10403;
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
    }
}