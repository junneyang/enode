package com.enodeframework.queue;

public interface IMessageHandler {
    void handle(QueueMessage queueMessage, IMessageContext context);
}
