package com.chaoyang805.sharelocation.netclient;

import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;

import java.util.ArrayList;

/**
 * Created by chaoyang805 on 2015/9/20.
 */
public class LocationUpdateHandler extends IoHandlerAdapter {
    private static final String TAG = "LocationUpdateHanlder";

    private ArrayList<IoSession> mSessionList = new ArrayList();

    @Override
    public void sessionCreated(IoSession session) throws Exception {
        super.sessionCreated(session);
    }

    @Override
    public void sessionOpened(IoSession session) throws Exception {
        super.sessionOpened(session);
    }

    @Override
    public void sessionClosed(IoSession session) throws Exception {
        super.sessionClosed(session);
    }

    @Override
    public void sessionIdle(IoSession session, IdleStatus status) throws Exception {
        super.sessionIdle(session, status);
    }

    @Override
    public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
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
            case "CREATE":
                //会话创建
                break;
            case "BROADCAST":
                //接受消息消息
                if (mListener != null) {
                    //注意，这里不是主线程
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

    @Override
    public void messageSent(IoSession session, Object message) throws Exception {
        super.messageSent(session, message);
    }

    public interface OnMessageReceivedListener {
        void onMessageReceived(String message);

        void onSessionClosed(String closedDeviceId,String userName);
    }

    private OnMessageReceivedListener mListener;

    public void setOnMessageReceivedListener(OnMessageReceivedListener listener) {
        mListener = listener;
    }
}
