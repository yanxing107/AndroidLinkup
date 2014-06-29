package com.znv.linkup.rest;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Message;

import com.znv.linkup.ViewSettings;
import com.znv.linkup.util.IconCacheUtil;
import com.znv.linkup.util.RestUtil;

/**
 * 第三方平台用户关卡分数或时间管理
 * 
 * @author yzb
 * 
 */
public class UserScore {
    public static int topN = 3;

    private static String USER_ADD_URI = "http://xxllk.aliapp.com/webapi/user/add";
    private static String SCORE_ADD_URI = "http://xxllk.aliapp.com/webapi/score/add";
    private static String SCORE_GET_URI = "http://xxllk.aliapp.com/webapi/score/get";
    private static String TIME_ADD_URI = "http://xxllk.aliapp.com/webapi/time/add";
    private static String TIME_GET_URI = "http://xxllk.aliapp.com/webapi/time/get";

    /**
     * 记录用户登录
     * 
     * @param userInfo
     *            用户登录信息
     */
    public static void login(final UserInfo userInfo, final Handler handler) {
        final List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("userid", userInfo.getUserId()));
        params.add(new BasicNameValuePair("username", userInfo.getUserName()));
        params.add(new BasicNameValuePair("usergender", userInfo.getUserGender()));
        params.add(new BasicNameValuePair("usericon", userInfo.getUserIcon()));
        params.add(new BasicNameValuePair("plat", userInfo.getPlat()));
        params.add(new BasicNameValuePair("platver", String.valueOf(userInfo.getPlatVersion())));
        new Thread(new Runnable() {

            @Override
            public void run() {
                String result = RestUtil.post(USER_ADD_URI, params);
                if (result != null) {
                    Message msg = new Message();
                    msg.what = ViewSettings.MSG_LOGIN;
                    msg.obj = userInfo;
                    handler.sendMessage(msg);
                } else {
                    // 网络或其它问题
                    handler.sendEmptyMessage(ViewSettings.MSG_NETWORK_EXCEPTION);
                }
            }
        }).start();
    }

    /**
     * 新增用户关卡分数，用于排名
     * 
     * @param userId
     *            user的id信息,缓存于客户端
     * @param level
     *            关卡id
     * @param score
     *            最高分数
     */
    public static void addScore(String userId, int level, int score, final Handler handler) {
        final List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("userid", userId));
        params.add(new BasicNameValuePair("level", String.valueOf(level)));
        params.add(new BasicNameValuePair("score", String.valueOf(score)));
        new Thread(new Runnable() {

            @Override
            public void run() {
                String result = RestUtil.post(SCORE_ADD_URI, params);
                if (result != null) {
                    // 成功post用户关卡分数
                    handler.sendEmptyMessage(ViewSettings.MSG_SCORE_ADD);
                } else {
                    // 网络或其它问题
                    handler.sendEmptyMessage(ViewSettings.MSG_NETWORK_EXCEPTION);
                }
            }
        }).start();
    }

    /**
     * 按关卡获取排名信息
     * 
     * @param level
     *            关卡id
     */
    public static void getTopScores(int level, final Handler handler) {
        final String uri = SCORE_GET_URI + "?level=" + String.valueOf(level) + "&top=" + String.valueOf(topN);
        new Thread(new Runnable() {

            @Override
            public void run() {
                String result = RestUtil.get(uri);
                if (result != null) {
                    // 成功获取排名信息
                    Message msg = new Message();
                    msg.what = ViewSettings.MSG_SCORE_GET;
                    msg.obj = result;
                    handler.sendMessage(msg);
                } else {
                    // 网络或其它问题
                    handler.sendEmptyMessage(ViewSettings.MSG_NETWORK_EXCEPTION);
                }
            }
        }).start();
    }

    /**
     * 新增用户关卡时间，用于排名
     * 
     * @param userId
     *            user的id信息,缓存于客户端
     * @param level
     *            关卡id
     * @param time
     *            最小时间
     */
    public static void addTime(String userId, int level, int time, final Handler handler) {
        final List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("userid", userId));
        params.add(new BasicNameValuePair("level", String.valueOf(level)));
        params.add(new BasicNameValuePair("time", String.valueOf(time)));
        new Thread(new Runnable() {

            @Override
            public void run() {
                String result = RestUtil.post(TIME_ADD_URI, params);
                if (result != null) {
                    // 成功post用户关卡时间
                    handler.sendEmptyMessage(ViewSettings.MSG_TIME_ADD);
                } else {
                    // 网络或其它问题
                    handler.sendEmptyMessage(ViewSettings.MSG_NETWORK_EXCEPTION);
                }
            }
        }).start();
    }

    /**
     * 按关卡获取排名信息
     * 
     * @param level
     *            关卡id
     */
    public static void getTopTimes(int level, final Handler handler) {
        final String uri = TIME_GET_URI + "?level=" + String.valueOf(level) + "&top=" + String.valueOf(topN);
        new Thread(new Runnable() {

            @Override
            public void run() {
                String result = RestUtil.get(uri);
                if (result != null) {
                    // 成功获取排名信息
                    Message msg = new Message();
                    msg.what = ViewSettings.MSG_TIME_GET;
                    msg.obj = result;
                    handler.sendMessage(msg);
                } else {
                    // 网络或其它问题
                    handler.sendEmptyMessage(ViewSettings.MSG_NETWORK_EXCEPTION);
                }
            }
        }).start();
    }

    /**
     * 获取用户icon
     * 
     * @param url
     *            url地址
     * @param callback
     *            回调
     */
    public static void getUserImage(final String url, final Handler handler) {
        new Thread(new Runnable() {

            @Override
            public void run() {
                String newUrl = url;
                // 获取清晰的QQ图像
                // if(newUrl.endsWith("/30")){
                // newUrl = newUrl.substring(0, newUrl.length() - 3) + "/100";
                // }
                Bitmap bm = IconCacheUtil.getIcon(newUrl);
                if (bm == null) {
                    try {
                        bm = BitmapFactory.decodeStream((new URL(newUrl)).openStream());
                    } catch (Exception e) {
                        e.printStackTrace();
                        bm = null;
                    }
                }
                if (bm != null) {
                    IconCacheUtil.putIcon(newUrl, bm);
                    // 成功获取排名信息
                    Message msg = new Message();
                    msg.what = ViewSettings.MSG_IMAGE_GET;
                    msg.obj = bm;
                    handler.sendMessage(msg);
                } else {
                    // 网络或其它问题
                    handler.sendEmptyMessage(ViewSettings.MSG_NETWORK_EXCEPTION);
                }
            }
        }).start();
    }

    /**
     * 批量获取icon信息
     * 
     * @param urls
     *            url地址集合
     * @param callback
     *            回调
     */
    public static void getTopImages(final List<String> urls, final Handler handler) {
        new Thread(new Runnable() {

            @Override
            public void run() {
                boolean hasImage = false;
                List<Bitmap> images = new ArrayList<Bitmap>();
                Bitmap bm = null;
                for (int i = 0; i < urls.size(); i++) {
                    bm = IconCacheUtil.getIcon(urls.get(i));
                    if (bm == null) {
                        try {
                            bm = BitmapFactory.decodeStream((new URL(urls.get(i))).openStream());
                            hasImage = true;
                            IconCacheUtil.putIcon(urls.get(i), bm);
                        } catch (Exception e) {
                            e.printStackTrace();
                            bm = null;
                        }
                    } else {
                        hasImage = true;
                    }
                    images.add(bm);
                }
                if (hasImage) {
                    // 成功获取排名信息
                    Message msg = new Message();
                    msg.what = ViewSettings.MSG_TOPIMAGES_GET;
                    msg.obj = images;
                    handler.sendMessage(msg);
                } else {
                    // 网络或其它问题
                    handler.sendEmptyMessage(ViewSettings.MSG_NETWORK_EXCEPTION);
                }
            }
        }).start();
    }
}