package com.chaoyang805.sharelocation.ui.activity;

import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.TelephonyManager;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.model.LatLng;
import com.chaoyang805.sharelocation.R;
import com.chaoyang805.sharelocation.baidumap.BaiduMapManager;
import com.chaoyang805.sharelocation.model.User;
import com.chaoyang805.sharelocation.netclient.LocationClient;
import com.chaoyang805.sharelocation.netclient.LocationUpdateHandler;
import com.chaoyang805.sharelocation.utils.ToastUtils;

import de.hdodenhof.circleimageview.CircleImageView;

public class MainActivity extends AppCompatActivity implements BaiduMapManager.OnLocationUpdateListener, View.OnClickListener {

    private static final String TAG = "MainActivity";
    /**
     * 百度地图控件
     */
    private MapView mMapView;
    /**
     * 开启和关闭定位的按钮
     */
    private FloatingActionButton mFab;
    /**
     * 百度地图manager对象
     */
    private BaiduMapManager mBaiduMapManager;
    /**
     * 显示共享位置用户详细信息的布局
     */
    private RelativeLayout mDetailLayout;
    /**
     * 显示所有user的view
     */
    private LinearLayout mAllUsersView;
    /**
     * 显示在线用户数量的textview
     */
    private TextView mTvUserCount;
    /**
     * 手机的IMEI信息
     */
    private String mDeviceId;
    /**
     * 在主线程处理网络回调中的内容
     */
    private Handler mHanler = new Handler();
    /**
     * 是否在共享位置的标志位
     */
    private boolean isSharing = false;
    /**
     * 位置共享的网络客户端对象
     */
    private LocationClient mClient;
    /**
     * 连接服务器超时的回调
     */
    private LocationClient.OnTimeOutCallback mTimeOutCallback;
    /**
     * 网络客户端接收到服务器数据时的回调
     */
    private LocationUpdateHandler.OnMessageReceivedListener
            mMessageReceivedListener = new LocationUpdateHandler.OnMessageReceivedListener() {
        @Override
        public void onMessageReceived(String message) {
            final User user = new User(message);
            mHanler.post(new Runnable() {
                @Override
                public void run() {
                    if (!mBaiduMapManager.containsUser(user)) {
                        addNewUserToView(user);
                    }
                    mBaiduMapManager.showUserOnMap(user);
                }
            });
        }

        @Override
        public void onSessionClosed(final String closedDeviceId, final String userName) {
            mHanler.post(new Runnable() {
                @Override
                public void run() {
                    //移除掉用户头像
                    removeUserFromView(closedDeviceId);
                    //从地图中移除掉用户的marker
                    mBaiduMapManager.removeMarker(closedDeviceId);
                    ToastUtils.showToast(MainActivity.this, getString(R.string.user_removed, userName));
                }
            });
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();
        prepareMapManager();
        TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        mDeviceId = tm.getDeviceId();
    }

    /**
     * 初始化view
     */
    private void initViews() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mFab = (FloatingActionButton) findViewById(R.id.fab);
        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isSharing) {
                    ToastUtils.showToast(MainActivity.this, getString(R.string.cancel_location_share));
                    stopShare();
                } else {
                    //先判断是否有网络，再开启位置分享
                    if (!fineNetwork()) {
                        ToastUtils.showToast(MainActivity.this, R.string.network_is_unavailable);
                        return;
                    }
                    startShare();
                    ToastUtils.showToast(MainActivity.this, R.string.start_sharing_location);
                }

            }
        });

        mMapView = (MapView) findViewById(R.id.bmap);

        mDetailLayout = (RelativeLayout) findViewById(R.id.rl_detail);
        mAllUsersView = (LinearLayout) findViewById(R.id.ll_users);
        mTvUserCount = (TextView) findViewById(R.id.tv_hint);
        mTvUserCount.setText(R.string.no_user);
        mDetailLayout.setVisibility(View.GONE);
    }

    /**
     * 初始化baidumapmanager
     */
    private void prepareMapManager() {
        mBaiduMapManager = new BaiduMapManager(this, mMapView);
        mBaiduMapManager.init();
    }

    /**
     * 是否有网络连接
     *
     * @return
     */
    private boolean fineNetwork() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        if (networkInfo != null) {
            return networkInfo.isAvailable();
        }
        return false;
    }

    /**
     * 开始共享位置的方法
     */
    private void startShare() {
        mFab.setImageResource(R.drawable.ic_cancel_white_48dp);
        mBaiduMapManager.requestLocation();
        mBaiduMapManager.registerLocationListener(this);
        //实例化Mina客户端
        if (mClient == null) {
            mTimeOutCallback = new LocationClient.OnTimeOutCallback() {
                @Override
                public void onTimeOut() {
                    mHanler.post(new Runnable() {
                        @Override
                        public void run() {
                            //连接服务器失败后，提示用户，并且重置mClient对象
                            ToastUtils.showToast(MainActivity.this, R.string.unable_to_connect_to_server);
                            stopShare();
                        }
                    });
                }
            };
            mClient = new LocationClient(mDeviceId, mTimeOutCallback);
            mClient.init();
            //添加收到消息时的回调
            mClient.getHandler().setOnMessageReceivedListener(mMessageReceivedListener);
            isSharing = true;
        }
        mDetailLayout.setVisibility(View.VISIBLE);

    }

    /**
     * 停止共享位置的方法
     */
    private void stopShare() {

        mFab.setImageResource(R.drawable.ic_share_white_48dp);
        mAllUsersView.removeAllViews();
        mDetailLayout.setVisibility(View.GONE);

        mClient.disconnect();
        mClient.getHandler().setOnMessageReceivedListener(null);
        mClient = null;

        mBaiduMapManager.removeAllMarkers();
        mBaiduMapManager.stopRequest();

        isSharing = false;
    }

    /**
     * 添加新的用户到共享位置的用户列表中
     *
     * @param user
     */
    private void addNewUserToView(User user) {
        CircleImageView civUser = new CircleImageView(this);

        String userName = user.getUserName();
        //根据username最后一个数字来为用户指定头像
        String imageResName = "image_" + userName.charAt(userName.length() - 1);

        //配置用户头像的布局参数
        civUser.setImageResource(getResources().getIdentifier(imageResName, "drawable", getPackageName()));
        civUser.setBorderColor(Color.WHITE);
        civUser.setBorderWidth(getResources().getDimensionPixelSize(R.dimen.border_width));
        int imageDimension = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                54f, getResources().getDisplayMetrics());
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(imageDimension, imageDimension);
        int margin = getResources().getDimensionPixelSize(R.dimen.user_head_margin);
        layoutParams.setMargins(margin, margin, margin, margin);
        civUser.setLayoutParams(layoutParams);
        civUser.setTag(user);
        //将userId的hashCode设置为view的id.
        civUser.setId(Math.abs(user.getUid().hashCode()));
        mAllUsersView.addView(civUser);
        civUser.setOnClickListener(this);
        mTvUserCount.setText(getString(R.string.sharing_user_num_text, mAllUsersView.getChildCount()));
    }

    /**
     * 用户取消位置共享时，移除用户的头像
     */
    private void removeUserFromView(String deviceId) {
        //根据deviceId来移除相应的view
        mAllUsersView.removeView(findViewById(Math.abs(deviceId.hashCode())));
        if (mAllUsersView.getChildCount() == 0) {
            mTvUserCount.setText(getString(R.string.no_user));
        } else {
            mTvUserCount.setText(getString(R.string.sharing_user_num_text, mAllUsersView.getChildCount()));
        }
    }

    /**
     * 保存本机用户的信息
     */
    private User me = null;

    /**
     * 位置更新时，将我的最新位置发送到服务器
     * @param latLng
     */
    @Override
    public void onLocationUpdate(LatLng latLng) {
        if (me == null) {
            //初始化本机用户的详细信息
            me = new User();
            me.setUid(mDeviceId);
            //调试时随机为用户指定userName
            me.setUserName("user" + (int) (Math.random() * 5 + 1));
        }
        me.setLatLng(latLng);
        //将我的最新位置信息发送给服务器
        if (isSharing) {
            mClient.updateMyLocation(me);
        }
    }
    /**
     * 用户头像被点击的监听事件
     * @param v
     */
    @Override
    public void onClick(View v) {
        User user = (User) v.getTag();
        if (user != null) {
            //移动当前用户到地图中心
            mBaiduMapManager.locateUser(user);
            ToastUtils.showToast(this, getString(R.string.current_user, user.getUserName()));
        }
    }

    /*在生命周期的各个方法中管理地图的生命周期*/
    @Override
    protected void onPause() {
        mBaiduMapManager.onPause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        mBaiduMapManager.onResume();
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        if (isSharing) {
            stopShare();
        }
        mBaiduMapManager.onDestroy();
        super.onDestroy();
    }
}
