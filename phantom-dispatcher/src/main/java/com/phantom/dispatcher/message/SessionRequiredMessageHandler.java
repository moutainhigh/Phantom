package com.phantom.dispatcher.message;

import com.google.protobuf.InvalidProtocolBufferException;
import com.phantom.dispatcher.message.wrapper.Identifyable;
import com.phantom.dispatcher.session.Session;
import com.phantom.dispatcher.session.SessionManager;
import com.phantom.common.Message;
import com.phantom.dispatcher.kafka.Producer;
import io.netty.channel.socket.SocketChannel;
import lombok.extern.slf4j.Slf4j;

/**
 * 需要校验Session的消息处理器
 *
 * @author Jianfeng Wang
 * @since 2019/11/19 17:14
 */
@Slf4j
public abstract class SessionRequiredMessageHandler<T extends Identifyable> extends AbstractMessageHandler {

    protected Producer producer;

    SessionRequiredMessageHandler(SessionManager sessionManager) {
        super(sessionManager);
        this.producer = Producer.getInstance(dispatcherConfig);
    }

    @Override
    public void handleMessage(Message message, SocketChannel channel) throws Exception {
        T msg = parseMessage(message);
        String uid = msg.getUid();
        Session session = sessionManager.getSession(msg.getUid());
        if (session == null) {
            log.info("找不到Session，发送消息失败");
            Message errorMessage = getErrorMessage(msg, channel);
            sendToAcceptor(uid, errorMessage);
            return;
        }
        processMessage(msg, channel);
    }

    /**
     * 真正处理消息逻辑
     *
     * @param message 消息
     * @param channel 渠道
     */
    protected abstract void processMessage(T message, SocketChannel channel);

    /**
     * 发送错误响应
     *
     * @param message 消息
     * @param channel 渠道
     * @return 消息
     */
    protected abstract Message getErrorMessage(T message, SocketChannel channel);

    /**
     * 根据消息获取用户ID
     *
     * @param message 消息
     * @return 用户ID
     * @throws InvalidProtocolBufferException Proto buf 序列化失败
     */
    protected abstract T parseMessage(Message message) throws InvalidProtocolBufferException;
}
