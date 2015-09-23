package com.chaoyang805.sharelocation.model;

import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.model.LatLng;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by chaoyang805 on 2015/9/19.
 */
public class User {
    /**
     * 每个用户的userId,区别用户的唯一性
     */
    private String mImei;
    /**
     * 用户的名字
     */
    private String mUserName;
    /**
     * 用户的头像
     */
    private BitmapDescriptor mUserIcon;

    /**
     * 用户当前的位置
     */
    private LatLng mLatLng;

    public User(){
    }

    /**
     * 通过传回来的json字符串构造一个user对象
     * @param jsonMessage
     */
    public User(String jsonMessage) {
        try {
            JSONObject jsonObject = new JSONObject(jsonMessage);
            mUserName = jsonObject.getString("user_name");
            mImei = jsonObject.getString("user_id");
            Double lat = jsonObject.getJSONObject("latLng").getDouble("lat");
            Double lng = jsonObject.getJSONObject("latLng").getDouble("lng");
            mLatLng = new LatLng(lat, lng);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void setUid(String imei) {
        mImei = imei;
    }

    public String getUid() {
        return mImei;
    }

    public String getUserName() {
        return mUserName;
    }

    public void setUserName(String userName) {
        mUserName = userName;
    }

    public BitmapDescriptor getUserIcon() {
        return mUserIcon;
    }

    public void setUserIcon(BitmapDescriptor userIcon) {
        mUserIcon = userIcon;
    }

    public LatLng getLatLng() {
        return mLatLng;
    }

    public void setLatLng(LatLng latLng) {
        mLatLng = latLng;
    }

    public String toJsonString() {
        //{"userid":1,"username":"chaoyang805","LatLng":{"Lat":"139.123456","Lng":"25.458712"}}
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("user_id", mImei);
            jsonObject.put("user_name", mUserName);
            JSONObject latLngJson = new JSONObject();
            latLngJson.put("lat",mLatLng.latitude);
            latLngJson.put("lng", mLatLng.longitude);
            jsonObject.put("latLng", latLngJson);
            return jsonObject.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
}
