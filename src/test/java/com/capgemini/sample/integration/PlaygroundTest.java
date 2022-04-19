package com.capgemini.sample.integration;

import com.capgemini.sample.integration.domain.Employee;
import com.capgemini.sample.integration.si.MultiplicationHandler;
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
import java.time.Instant;

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
        final Message<Integer> msg1 = TestUtil.createMessage(2);
        final Message<Integer> msg2 = TestUtil.createMessage(200);
        final Message<String> msg3 = TestUtil.createMessage("trzecia wiadomosc");
        // when
        try {
            this.entryChannel.send(msg1);
            this.entryChannel.send(msg2);
            this.entryChannel.send(msg3);
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
        public static final String DISCARD_FILTER_CHANNEL = "discardFilterChannel";

        @Bean(ENTRY_CHANNEL)
        MessageChannel entryChannel() {
            return MessageChannels.direct()
                                  .datatype(Integer.class, String.class)
                                  .get();
        }

        @Bean(EXIT_CHANNEL)
        MessageChannel exitChannel() {
            return MessageChannels.publishSubscribe()
                                  .get();
        }

        @Bean(DISCARD_FILTER_CHANNEL)
        MessageChannel discardFilterChannel() {
            return MessageChannels.publishSubscribe()
                                  .get();
        }

        @Bean
        public IntegrationFlow flow() {
            return IntegrationFlows.from(entryChannel())
                                   .log(LoggingHandler.Level.WARN, m -> "Received: " + m.getPayload())
                                   .enrichHeaders(spec -> spec.header("componentCreatedAt", Instant.now()))
                                   .enrichHeaders(spec -> spec.headerFunction("now", m -> Instant.now()))
                                   .enrichHeaders(spec -> spec.headerFunction("is_number", m -> TestUtil.isNumber(m.getPayload())))
                                   .filter(p -> p != null, spec -> spec.id("notNullFilter"))
                                   .filter(p -> p.toString().length() >=1 )
                                   //.filter(String.class, p -> p.length() >= 1 ) // moglbym tak zrobic gdybym spodziewal sie tylko Stringow ALE w naszym przykladzie trafiaja tutaj msg z Integer i String
                                   .filter(Message.class, m -> m.getHeaders().containsKey("is_number") && (boolean)m.getHeaders().get("is_number"), spec -> spec.discardChannel(DISCARD_FILTER_CHANNEL)) // sposob na dostanie sie do headera
                                   .handle(Integer.class, (p, h) -> p * 2)
                                   .handle(new MultiplicationHandler(2, 200))
                                   .<Integer, Employee>transform(p -> new Employee(p))
                                   .log(LoggingHandler.Level.WARN)
                                   .channel(exitChannel())
                                   .get();
        }

    }

}
