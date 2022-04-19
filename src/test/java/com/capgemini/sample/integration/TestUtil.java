package com.capgemini.sample.integration;

import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

/**
 * Utils to reduce boiler-plate code
 */
public class TestUtil {

    public static <T> Message<T> createMessage(T payload) {
        return MessageBuilder.withPayload(payload)
                             .build();
    }

}
