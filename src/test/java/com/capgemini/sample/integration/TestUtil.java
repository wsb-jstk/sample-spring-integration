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

    public static boolean isNumber(Object object) {
        return object.toString()
                     .chars()
                     .allMatch(Character::isDigit);
        // try {
        //     Integer.parseInt(object.toString());
        //     return true;
        // } catch (NumberFormatException e) {
        //     return false;
        // }
    }

}
