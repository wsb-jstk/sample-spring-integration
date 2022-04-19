package com.capgemini.sample.integration.si;

import lombok.RequiredArgsConstructor;
import org.springframework.integration.handler.GenericHandler;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.MessageHeaders;

@RequiredArgsConstructor
public class MultiplicationHandler implements GenericHandler<Integer> {

    private final int multiplier;
    private final int maxNumber;

    @Override
    public Object handle(Integer payload, MessageHeaders headers) {
        if (payload < maxNumber) {
            return payload * multiplier;
        }
        return MessageBuilder.withPayload(Integer.MAX_VALUE)
                             .setHeader("msg", "Reached limit")
                             .setHeader("limit", maxNumber)
                             .build();
    }

}
