package com.chaoyang805.sharelocation.netclient;

import android.util.Log;

import com.chaoyang805.sharelocation.model.User;

import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.textline.TextLineCodecFactory;
import org.apache.mina.transport.socket.nio.NioSocketConnector;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;

/**
 * Created by chaoyang805 on 2015/9/20.
 */
public class LocationClient {

    private static final String TAG = "LocationClient";
    /**
     * socketConnector对象
     */
    private NioSocketConnector mConnector;
    /**
     * 处理消息的Handler对象
     */
    private LocationUpdateHandler mHandler;
    /**
     * 客户端连接成功后取得的回话对象
     */
    private IoSession mSession;

    private boolean isConnected = false;
    /**
     * 是否已经初始化完成
     */
    private boolean initiated = false;
    /**
     * 用户手机的IMEI信息
     */
    private String mDeviceId;
    /**
     * 会话是否创建的标志位，用来在第一次的时候给服务端发送客户端的用户信息
     */
    private boolean mSessionCreated = false;

    public LocationClient(String deviceId) {
        mDeviceId = deviceId;
        mHandler = new LocationUpdateHandler();
    }

    public void init() {
        if (initiated) {
            return;
        }
        new Thread() {
            @Override
            public void run() {
                mConnector = new NioSocketConnector();
                mConnector.setHandler(mHandler);
                mConnector.getFilterChain().addLast("codec",
                        new ProtocolCodecFilter(new TextLineCodecFactory(Charset.forName("UTF-8"))));
                ConnectFuture future = mConnector.connect(new InetSocketAddress("192.168.0.109", 9988));
                future.awaitUninterruptibly();
                mSession = future.getSession();
//                if (mSession != null) {
//                    mSession.write("CREATE" + " " + mDeviceId);
//                }
                isConnected = mSession.isConnected();
                Log.d(TAG, "locationClient initiated ");
            }
        }.start();
        initiated = true;
    }

    public LocationUpdateHandler getHandler() {
        return mHandler;
    }

    /**
     * 发送我的最新位置信息到服务器
     *
     * @param user
     */
    public void updateMyLocation(User user) {
        if (isConnected) {
            String message = user.toJsonString();
            if (mSession != null && mSession.isConnected()) {
                if (!mSessionCreated) {
                    mSession.write("CREATE" + " " + mDeviceId + " " + user.getUserName());
                    mSessionCreated = true;
                }
                mSession.write("BROADCAST" + " " + message);
            }
        }
    }

    /**
     * 关闭客户端连接的方法
     */
    public void disconnect() {
        if (mSession != null && isConnected) {
            mSession.close(true);
            mConnector.dispose();
        }
    }

}
