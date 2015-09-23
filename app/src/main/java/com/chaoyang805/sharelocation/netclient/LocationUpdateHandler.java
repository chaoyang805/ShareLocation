package com.chaoyang805.sharelocation.netclient;

import android.util.Log;

import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;

/**
 * Created by chaoyang805 on 2015/9/20.
 */
public class LocationUpdateHandler extends IoHandlerAdapter {
    private static final String TAG = "LocationUpdateHanlder";

    @Override
    public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
        Log.e(TAG, cause.getMessage(), cause);
        super.exceptionCaught(session, cause);
    }

    @Override
    public void messageReceived(IoSession session, Object message) throws Exception {
        super.messageReceived(session, message);
        //接受服务端的消息
        String theMessage = (String) message;
        String[] results = theMessage.split(" ");
        String action = results[0];
        //根据action判断消息的类型
        switch (action) {
            case "BROADCAST":
                //接受消息
                if (mListener != null) {
                    String content = results[1];
                    mListener.onMessageReceived(content);
                }
                break;
            case "SESSIONCLOSED":
                if (mListener != null) {
                    String deviceId = results[1];
                    String userName = results[2];
                    mListener.onSessionClosed(deviceId, userName);
                }
                break;
        }
    }

    /**
     * 收到服务器发送的消息的回调接口
     */
    public interface OnMessageReceivedListener {

        void onMessageReceived(String message);

        void onSessionClosed(String closedDeviceId,String userName);
    }

    private OnMessageReceivedListener mListener;

    public void setOnMessageReceivedListener(OnMessageReceivedListener listener) {
        mListener = listener;
    }

}
