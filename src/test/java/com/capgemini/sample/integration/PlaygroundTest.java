package com.capgemini.sample.integration;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.exceptions.base.MockitoException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.handler.GenericHandler;
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.integration.test.mock.MockIntegration;
import org.springframework.integration.test.mock.MockMessageHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @see MessageHandler
 * @see GenericHandler
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = PlaygroundTest.TestConfig.class)
class PlaygroundTest {

    @Autowired
    @Qualifier(TestConfig.ENTRY_CHANNEL)
    private DirectChannel entryChannel;
    @Autowired
    @Qualifier(TestConfig.EXIT_CHANNEL)
    private PublishSubscribeChannel exitChannel;

    @Test
    void test() {
        // given
        // prepare fake message handler
        final ArgumentCaptor<Message<?>> messageArgumentCaptor = ArgumentCaptor.forClass(Message.class);
        final MockMessageHandler mh = MockIntegration.mockMessageHandler(messageArgumentCaptor)
                                                     .handleNext(m -> {});
        this.exitChannel.subscribe(mh);
        // prepare message
        final Message<Integer> msg = TestUtil.createMessage(1);
        // when
        try {
            this.entryChannel.send(msg);
            // then
            Awaitility.await()
                      .atMost(Duration.ofSeconds(10))
                      .ignoreException(MockitoException.class)
                      .untilAsserted(() -> {
                          final Message<?> value = messageArgumentCaptor.getValue();
                          assertThat(value).isNotNull();
                      });
        } finally {
            this.exitChannel.unsubscribe(mh);
        }
    }

    @TestConfiguration
    @EnableIntegration
    static class TestConfig {

        public static final String ENTRY_CHANNEL = "entryChannel";
        public static final String EXIT_CHANNEL = "exitChannel";

        @Bean(ENTRY_CHANNEL)
        MessageChannel entryChannel() {
            return MessageChannels.direct()
                                  .get();
        }

        @Bean(EXIT_CHANNEL)
        MessageChannel exitChannel() {
            return MessageChannels.publishSubscribe()
                                  .get();
        }

        @Bean
        public IntegrationFlow flow() {
            return IntegrationFlows.from(entryChannel())
                                   .log(LoggingHandler.Level.WARN, m -> "Received: " + m.getPayload())
                                   .channel(exitChannel())
                                   .get();
        }

    }

}
