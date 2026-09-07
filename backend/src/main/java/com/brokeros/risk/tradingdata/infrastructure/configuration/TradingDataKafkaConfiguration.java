package com.brokeros.risk.tradingdata.infrastructure.configuration;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.kafka.ConcurrentKafkaListenerContainerFactoryConfigurer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonContainerStoppingErrorHandler;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.ProducerListener;

@Configuration(proxyBeanMethods = false)
public class TradingDataKafkaConfiguration {

    @Bean
    ConcurrentKafkaListenerContainerFactory<Object, Object> tradingDataKafkaListenerContainerFactory(
            KafkaProperties properties, SslBundles sslBundles,
            ConcurrentKafkaListenerContainerFactoryConfigurer configurer) {
        Map<String, Object> config = new HashMap<>(properties.buildConsumerProperties(sslBundles));
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        config.put(ConsumerConfig.ALLOW_AUTO_CREATE_TOPICS_CONFIG, false);
        config.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);
        ConcurrentKafkaListenerContainerFactory<Object, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        configurer.configure(factory, new DefaultKafkaConsumerFactory<>(config));
        // Preserve the accepted Phase A serialized-ingress assumption within this process.
        factory.setConcurrency(1);
        factory.setBatchListener(false);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
        factory.getContainerProperties().setAsyncAcks(false);
        factory.getContainerProperties().setSyncCommits(true);
        factory.getContainerProperties().setStopImmediate(true);
        // Infrastructure failure stops consumption with its offset uncommitted.
        // No catch-all retry loop or exhaustion policy may silently skip a valid record.
        factory.setCommonErrorHandler(new CommonContainerStoppingErrorHandler());
        return factory;
    }

    @Bean(destroyMethod = "destroy")
    DefaultKafkaProducerFactory<String, byte[]> tradingDataProducerFactory(
            KafkaProperties properties, SslBundles sslBundles) {
        Map<String, Object> config = new HashMap<>(properties.buildProducerProperties(sslBundles));
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        config.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 1);
        config.put(ProducerConfig.RETRIES_CONFIG, 3);
        config.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 5000);
        config.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 5000);
        config.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 15000);
        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    KafkaTemplate<String, byte[]> tradingDataKafkaTemplate(
            DefaultKafkaProducerFactory<String, byte[]> tradingDataProducerFactory) {
        KafkaTemplate<String, byte[]> template = new KafkaTemplate<>(tradingDataProducerFactory);
        // Failures are reported by the ingestion boundary using coordinates only.
        // The framework's default producer logger may include an untrusted key/value.
        template.setProducerListener(new ProducerListener<>() { });
        return template;
    }
}
