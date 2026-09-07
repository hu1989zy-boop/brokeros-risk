package com.brokeros.risk.tradingdata.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import com.brokeros.risk.tradingdata.infrastructure.configuration.TradingDataKafkaConfiguration;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.autoconfigure.ssl.SslAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;

class TradingDataKafkaConfigurationTests {
    @Test
    void nativeSecuritySettingsSurviveWhileSafeDeliverySettingsCannotBeWeakened() {
        String syntheticSecret = UUID.randomUUID().toString();
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(KafkaAutoConfiguration.class, SslAutoConfiguration.class))
                .withUserConfiguration(TradingDataKafkaConfiguration.class)
                .withPropertyValues("spring.kafka.consumer.group-id=q015-config-test",
                        "spring.kafka.properties.security.protocol=SASL_SSL",
                        "spring.kafka.properties.sasl.jaas.config=" + syntheticSecret,
                        "spring.kafka.consumer.enable-auto-commit=true",
                        "spring.kafka.consumer.max-poll-records=9999",
                        "spring.kafka.listener.ack-mode=batch",
                        "spring.kafka.listener.concurrency=9",
                        "spring.kafka.listener.auto-startup=false")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    ConcurrentKafkaListenerContainerFactory<?, ?> factory = context.getBean("tradingDataKafkaListenerContainerFactory",
                            ConcurrentKafkaListenerContainerFactory.class);
                    var consumer = (DefaultKafkaConsumerFactory<?, ?>) factory.getConsumerFactory();
                    assertThat(consumer.getConfigurationProperties())
                            .containsEntry("security.protocol", "SASL_SSL")
                            .containsEntry("sasl.jaas.config", syntheticSecret)
                            .containsEntry(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false)
                            .containsEntry(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100)
                            .containsEntry(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class)
                            .containsEntry(ConsumerConfig.ALLOW_AUTO_CREATE_TOPICS_CONFIG, false);
                    var container = (ConcurrentMessageListenerContainer<?, ?>) factory.createContainer("trading-data.canonical");
                    assertThat(container.getConcurrency()).isEqualTo(1);
                    assertThat(container.getContainerProperties().getAckMode()).isEqualTo(ContainerProperties.AckMode.RECORD);
                    assertThat(container.getContainerProperties().isAsyncAcks()).isFalse();
                    assertThat(container.getContainerProperties().isSyncCommits()).isTrue();
                    DefaultKafkaProducerFactory<?, ?> producer = context.getBean("tradingDataProducerFactory", DefaultKafkaProducerFactory.class);
                    assertThat(producer.getConfigurationProperties())
                            .containsEntry("security.protocol", "SASL_SSL")
                            .containsEntry("sasl.jaas.config", syntheticSecret)
                            .containsEntry("enable.idempotence", true)
                            .containsEntry("acks", "all")
                            .containsEntry("retries", 3);
                });
    }
}
