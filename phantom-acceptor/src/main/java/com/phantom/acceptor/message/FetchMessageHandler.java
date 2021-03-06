package com.phantom.acceptor.message;

import com.google.protobuf.InvalidProtocolBufferException;
import com.phantom.acceptor.dispatcher.DispatcherManager;
import com.phantom.acceptor.session.SessionManagerFacade;
import com.phantom.common.FetchMessageRequest;
import com.phantom.common.FetchMessageResponse;
import com.phantom.common.Message;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 抓取消息请求处理器
 *
 * @author Jianfeng Wang
 * @since 2019/11/18 15:13
 */
@Slf4j
public class FetchMessageHandler extends AbstractMessageHandler {

    FetchMessageHandler(DispatcherManager dispatcherManager, SessionManagerFacade sessionManagerFacade,
                        ThreadPoolExecutor threadPoolExecutor) {
        super(dispatcherManager, sessionManagerFacade, threadPoolExecutor);
    }

    @Override
    protected String getReceiverId(Message message) throws InvalidProtocolBufferException {
        FetchMessageRequest request = FetchMessageRequest.parseFrom(message.getBody());
        return request.getUid();
    }

    @Override
    protected String getResponseUid(Message message) throws InvalidProtocolBufferException {
        FetchMessageResponse fetchMessageResponse = FetchMessageResponse.parseFrom(message.getBody());
        return fetchMessageResponse.getUid();
    }

    @Override
    protected Message getErrorResponse(Message message) throws InvalidProtocolBufferException {
        FetchMessageRequest request = FetchMessageRequest.parseFrom(message.getBody());
        FetchMessageResponse response = FetchMessageResponse.newBuilder()
                .setIsEmpty(true)
                .setUid(request.getUid())
                .build();
        return Message.buildFetcherMessageResponse(response);
    }
}
