package com.chaoyang805.sharelocation.baidumap;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.model.LatLng;
import com.chaoyang805.sharelocation.R;
import com.chaoyang805.sharelocation.model.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by chaoyang805 on 2015/9/7.
 * 对百度地图进行管理的manager类
 */
public class BaiduMapManager implements BDLocationListener {
    private static final String TAG = "BaiduMapManager";
    /**
     * context对象
     */
    private Context mContext;
    /**
     * 百度地图对象
     */
    private BaiduMap mBaiduMap;
    /**
     * 地图控件对象
     */
    private MapView mMapView;
    /**
     * 定位client对象
     */
    private LocationClient mClient;
    /**
     * 是否是第一次定位，如果是则自动移动到屏幕中心
     */
    private boolean isFirstLocate = true;
    /**
     * 保存有关于我的用户信息
     */
    private User me = null;
    /**
     * 显示用户位置信息的view
     */
    private LinearLayout mUserMarkerView;
    /**
     * 显示用户头像的imageView
     */
    private CircleImageView mCivUserInfo;
    /**
     * 持有所有的BitmapDescriptor,程序销毁时回收。
     */
    private List<BitmapDescriptor> mMarkerViews = new ArrayList<>();
    /**
     * 保存用户id和对应的marker,每次将marker添加到地图上时，也要加入到map中
     */
    private Map<String, Marker> mMarkers = new HashMap<>();

    public BaiduMapManager(Context context, MapView mapView) {
        mContext = context;
        mMapView = mapView;
        mMapView.showScaleControl(true);
        mMapView.showZoomControls(false);
        mBaiduMap = mapView.getMap();
    }


    /**
     * 初始化地图
     */
    public void init() {
        mBaiduMap.setMyLocationEnabled(true);
        mBaiduMap.setMapStatus(MapStatusUpdateFactory.zoomTo(15f));
        MyLocationConfiguration config = new MyLocationConfiguration
                (MyLocationConfiguration.LocationMode.FOLLOWING, true, null);
        mBaiduMap.setMyLocationConfigeration(config);
        mClient = new LocationClient(mContext);
        mClient.registerLocationListener(this);
        //配置mClient对象的参数信息
        LocationClientOption option = new LocationClientOption();

        //开启GPS
        option.setOpenGps(true);
        //坐标类型
        option.setEnableSimulateGps(true);
        //是否需要地址
        option.setIsNeedAddress(true);
        //是否需要手机的方向
        option.setNeedDeviceDirect(true);
        option.setCoorType("bd09ll");
        mClient.setLocOption(option);
        //从布局文件中解析markerview
        mUserMarkerView = (LinearLayout) LayoutInflater.from(mContext).inflate(R.layout.user_profile, null, false);
        mCivUserInfo = (CircleImageView) mUserMarkerView.findViewById(R.id.user_head);

    }

    /**
     * 开始进行定位的方法
     */
    public void requestLocation() {
        mClient.start();
        mClient.requestLocation();
    }

    /**
     * 暂停地图
     */
    public void onPause() {
        mMapView.onPause();
    }

    /**
     * 恢复地图
     */
    public void onResume() {
        mMapView.onResume();
    }

    /**
     * 销毁地图
     */
    public void onDestroy() {
        // 退出时销毁定位
        mClient.unRegisterLocationListener(this);
        mClient.stop();
        // 关闭定位图层
        mBaiduMap.setMyLocationEnabled(false);
        //销毁BitmapDescriptor
        for (BitmapDescriptor markerView : mMarkerViews) {
            markerView.recycle();
        }
        mMapView.onDestroy();
        mMapView = null;
    }

    /**
     * 定位返回数据的回调
     *
     * @param bdLocation
     */
    @Override
    public void onReceiveLocation(BDLocation bdLocation) {
        if (bdLocation == null) {
            return;
        }
        //定位成功的情况
        if (bdLocation.getLocType() == 61 ||
                bdLocation.getLocType() == 65 ||
                bdLocation.getLocType() == 66 ||
                bdLocation.getLocType() == 68 ||
                bdLocation.getLocType() == 161) {
            LatLng latLng = new LatLng(bdLocation.getLatitude(), bdLocation.getLongitude());
            if (isFirstLocate) {
                MapStatusUpdate msu = MapStatusUpdateFactory.newLatLng(latLng);
                mBaiduMap.animateMapStatus(msu);
                isFirstLocate = false;
            }
            //我的位置得到了更新，将我的最新位置信息发送给服务器
            if (mListener != null) {
                mListener.onLocationUpdate(latLng);
            }
        } else {
            Toast.makeText(mContext, R.string.location_failed, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 将用户显示在地图上
     *
     * @param user
     * @return
     */
    public void showUserOnMap(User user) {
        Marker marker;
        //如果传入的用户的位置还没显示在地图上，则先显示在地图上
        if (!mMarkers.containsKey(user.getUid())) {
            //添加用户对应的marker到地图上
            addNewMarker(user);
        } else {
            //如果已在地图上显示，直接对位置进行更新
            marker = mMarkers.get(user.getUid());
            LatLng latLng = user.getLatLng();
            marker.setPosition(latLng);
        }
    }

    /**
     * 添加新的marker到地图上
     * @param user marker所对应的用户信息
     */
    private void addNewMarker(User user) {
        Marker marker;
        String userName = user.getUserName();
        //根据username最后一个数字来为用户指定头像
        String imageResName = "image_"+userName.charAt(userName.length() - 1);
        //调试阶段，随机为用户指定头像
        mCivUserInfo.setImageResource(mContext.getResources().getIdentifier(imageResName, "drawable",
                mContext.getPackageName()));
        BitmapDescriptor bd = BitmapDescriptorFactory.fromView(mUserMarkerView);
        OverlayOptions overlayOptions = new MarkerOptions().position(user.getLatLng()).title(user.getUserName()).icon(bd);
        marker = (Marker) mBaiduMap.addOverlay(overlayOptions);
        mMarkers.put(user.getUid(), marker);
    }

    /**
     * 判断用户是否已显示在地图上
     * @param user
     * @return
     */
    public boolean containsUser(User user) {
        return mMarkers.containsKey(user.getUid());
    }

    /**
     * 移除掉已经关闭位置分享的用户的marker
     * @param deviceId
     */
    public void removeMarker(String deviceId) {
        //分别从滴入和mMarkers中移除marker。
        mMarkers.remove(deviceId).remove();
    }

    /**
     * 移除地图上显示的所有的marker
     */
    public void removeAllMarkers() {
        for (String key : mMarkers.keySet()) {
            mMarkers.get(key).remove();
        }
        mMarkers.clear();
    }

    /**
     * 位置信息更新时的回调接口
     */
    public interface OnLocationUpdateListener {
        void onLocationUpdate(LatLng latLng);
    }

    private OnLocationUpdateListener mListener;

    /**
     * 注册位置更新监听的方法
     *
     * @param listener
     */
    public void registerLocationListener(OnLocationUpdateListener listener) {
        mListener = listener;
    }


}
