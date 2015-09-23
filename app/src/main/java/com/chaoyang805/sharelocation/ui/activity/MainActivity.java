package com.chaoyang805.sharelocation.ui.activity;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;

import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.model.LatLng;
import com.chaoyang805.sharelocation.R;
import com.chaoyang805.sharelocation.baidumap.BaiduMapManager;
import com.chaoyang805.sharelocation.model.User;
import com.chaoyang805.sharelocation.netclient.LocationClient;
import com.chaoyang805.sharelocation.netclient.LocationUpdateHandler;
import com.chaoyang805.sharelocation.utils.ToastUtils;

import de.hdodenhof.circleimageview.CircleImageView;

public class MainActivity extends AppCompatActivity implements BaiduMapManager.OnLocationUpdateListener {

    private static final String TAG = "MainActivity";


    /**
     * 百度地图控件
     */
    private MapView mMapView;
    /**
     * 百度地图manager对象
     */
    private BaiduMapManager mBaiduMapManager;

    private HorizontalScrollView mSvUsers;
    /**
     * 显示所有user的view
     */
    private LinearLayout mAllUsersView;

    private String mDeviceId;

    private Handler mHanler = new Handler();

    private LocationClient mClient;
    private LocationUpdateHandler.OnMessageReceivedListener
            mMessageReceivedListener = new LocationUpdateHandler.OnMessageReceivedListener() {
        @Override
        public void onMessageReceived(String message) {
            final User user = new User(message);
            Message msg = mHanler.obtainMessage();
            msg.obj = user;
            mHanler.post(new Runnable() {
                @Override
                public void run() {
                    if (!mBaiduMapManager.containsUser(user)) {
                        addNewUser(user);
                    }
                    mBaiduMapManager.showUserOnMap(user);
                }
            });
        }

        @Override
        public void onSessionClosed(final String closedDeviceId,final String userName) {
            mHanler.post(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "onSessionClosed device = " + closedDeviceId);
                    //移除掉用户头像
                    removeUserIcon(closedDeviceId);
                    //从地图中移除掉用户的marker
                    mBaiduMapManager.removeMarker(closedDeviceId);
                    ToastUtils.showToast(MainActivity.this, getString(R.string.user_removed, userName));
                }
            });
        }
    };

    /**
     * 用户取消位置共享时，移除用户的头像
     */
    private void removeUserIcon(String deviceId) {
        Log.d(TAG, "remove user's deviceId = " + deviceId + ",hashCode = " + deviceId.hashCode());
        View v = findViewById(Math.abs(deviceId.hashCode()));
        Log.d(TAG, "view == null ? " + (v == null));
        mAllUsersView.removeView(v);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();
        prepareMapManager();
        TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        mDeviceId = tm.getDeviceId();
        //TODO 记得判断网络是否可用
    }

    private void initViews() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startShare();
            }
        });

        mMapView = (MapView) findViewById(R.id.bmap);

        mSvUsers = (HorizontalScrollView) findViewById(R.id.sv_users);
        mAllUsersView = (LinearLayout) findViewById(R.id.ll_users);
//        mCivMe = (CircleImageView) findViewById(R.id.civ_me);

        mSvUsers.setVisibility(View.GONE);
    }

    /**
     * 开始共享位置的方法
     */
    private void startShare() {
        mBaiduMapManager.requestLocation();
        mBaiduMapManager.registerLocationListener(this);
        //实例化Mina客户端
        if (mClient == null) {
            mClient = new LocationClient(mDeviceId);
            mClient.init();
            //添加收到消息时的回调
            mClient.getHandler().setOnMessageReceivedListener(mMessageReceivedListener);
            ToastUtils.showToast(this, getString(R.string.start_sharing_location));
        }
        mSvUsers.setVisibility(View.VISIBLE);
    }

    /**
     * 添加新的用户到共享位置的用户列表中
     *
     * @param user
     */
    private void addNewUser(User user) {
        CircleImageView civUser = new CircleImageView(this);

        String userName = user.getUserName();
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
        //将userId的hashCode设置为view的id.
        Log.d(TAG, "add new user view deviceId = " + user.getUid() + ",hashCode = " + user.getUid().hashCode());
        civUser.setId(Math.abs(user.getUid().hashCode()));
        mAllUsersView.addView(civUser);
    }

    /**
     * 初始化baidumapmanager
     */
    private void prepareMapManager() {
        mBaiduMapManager = new BaiduMapManager(this, mMapView);
        mBaiduMapManager.init(false);
    }

    //在生命周期的各个方法中管理地图的生命周期
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
        mClient.disconnect();
        mBaiduMapManager.onDestroy();
        super.onDestroy();
    }

    private User me = null;

    @Override
    public void onLocationUpdate(LatLng latLng) {
        if (me == null) {
            //初始化本机用户的详细信息
            me = new User();
            me.setUid(mDeviceId);
            me.setUserName("user" + (int) (Math.random() * 5 + 1));
        }
        me.setLatLng(latLng);
        //将我的最新位置信息发送给服务器
        mClient.updateMyLocation(me);
    }
}
