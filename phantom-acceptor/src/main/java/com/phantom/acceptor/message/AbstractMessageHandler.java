package com.phantom.acceptor.message;

import com.google.protobuf.InvalidProtocolBufferException;
import com.phantom.acceptor.dispatcher.DispatcherInstance;
import com.phantom.acceptor.dispatcher.DispatcherManager;
import com.phantom.acceptor.session.SessionManagerFacade;
import com.phantom.common.Constants;
import com.phantom.common.Message;
import io.netty.channel.socket.SocketChannel;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 公共消息处理器
 *
 * @author Jianfeng Wang
 * @since 2019/11/8 16:02
 */
@Slf4j
public abstract class AbstractMessageHandler implements MessageHandler {

    private DispatcherManager dispatcherManager;

    protected SessionManagerFacade sessionManagerFacade;

    private ThreadPoolExecutor threadPoolExecutor;


    AbstractMessageHandler(DispatcherManager dispatcherManager, SessionManagerFacade sessionManagerFacade,
                           ThreadPoolExecutor threadPoolExecutor) {
        this.dispatcherManager = dispatcherManager;
        this.sessionManagerFacade = sessionManagerFacade;
        this.threadPoolExecutor = threadPoolExecutor;

    }

    @Override
    public void handleMessage(Message message, SocketChannel channel) {
        threadPoolExecutor.execute(() -> {
            try {
                switch (message.getMessageType()) {
                    case Constants.MESSAGE_TYPE_REQUEST:
                        handleRequestMessage(message, channel);
                        break;
                    case Constants.MESSAGE_TYPE_RESPONSE:
                        handleResponseMessage(message);
                        break;
                    default:
                        break; // no-op
                }
            } catch (InvalidProtocolBufferException e) {
                log.error("序列化异常：", e);
            }
        });

    }

    /**
     * 获取这条消息是发送给谁的。
     *
     * @param message 消息
     * @return 用户Id
     * @throws InvalidProtocolBufferException Protobuf序列化失败
     */
    protected abstract String getReceiverId(Message message) throws InvalidProtocolBufferException;

    /**
     * 获取这条消息是响应给谁的。
     *
     * @param message 消息
     * @return 用户Id
     * @throws InvalidProtocolBufferException Protobuf序列化失败
     */
    protected abstract String getResponseUid(Message message) throws InvalidProtocolBufferException;


    /**
     * 处理响应消息,返回给客户端
     *
     * @param message 消息
     */
    private void handleResponseMessage(Message message) throws InvalidProtocolBufferException {
        String uid = getResponseUid(message);
        SocketChannel session = sessionManagerFacade.getSession(uid);
        if (session != null) {
            log.info("将响应推送给客户端：uid = {} , requestType = {}", uid, Constants.requestTypeName(message.getRequestType()));
            session.writeAndFlush(message.getBuffer());
        } else {
            log.info("将响应推送给客户端失败，找不到session");
        }
    }

    /**
     * 处理请求消息，公共逻辑，提供一个钩子，直接把消息转发给分发系统
     *
     * @param message 消息
     * @param channel 客户端连接的channel
     */
    private void handleRequestMessage(Message message, SocketChannel channel) throws InvalidProtocolBufferException {
        String uid = getReceiverId(message);
        beforeDispatchMessage(uid, message, channel);
        if (!sendMessage(uid, message)) {
            log.info("将错误信息写回给客户端");
            channel.writeAndFlush(getErrorResponse(message).getBuffer());
        }
    }

    /**
     * 获取错误响应
     *
     * @param message 请求消息
     * @return 响应
     */
    protected abstract Message getErrorResponse(Message message) throws InvalidProtocolBufferException;

    /**
     * 钩子，转发到分发系统的逻辑，子类需要可以重写
     *
     * @param uid     用户ID
     * @param message 消息
     * @param channel 客户端连接的channel
     */
    protected void beforeDispatchMessage(String uid, Message message, SocketChannel channel) {
        // default no-op
    }


    /**
     * 发送消息到分发系统
     *
     * @param uid     用户ID
     * @param message 消息
     */
    private boolean sendMessage(String uid, Message message) {
        log.info("将请求转发到分发系统, uid = {} , requestType = {}", uid, Constants.requestTypeName(message.getRequestType()));
        DispatcherInstance dispatcherInstance = dispatcherManager.chooseDispatcher(uid);
        if (dispatcherInstance == null) {
            log.error("无法找到接入系统，发送消息失败....");
            return false;
        }
        dispatcherInstance.sendMessage(message.getBuffer());
        return true;
    }

}
