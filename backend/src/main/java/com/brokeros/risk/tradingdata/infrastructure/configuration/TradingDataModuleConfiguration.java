package com.brokeros.risk.tradingdata.infrastructure.configuration;

import java.time.Clock;

import com.brokeros.risk.security.application.AuthorizationGuard;
import com.brokeros.risk.tradingdata.application.TradingDataIngestionService;
import com.brokeros.risk.tradingdata.application.port.TradingDataEventPublisher;
import com.brokeros.risk.tradingdata.application.port.TradingDataEventStore;
import com.brokeros.risk.tradingdata.application.port.TradingDataMetricsPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration(proxyBeanMethods = false)
public class TradingDataModuleConfiguration {

    @Bean
    TradingDataIngestionService tradingDataIngestionService(
            AuthorizationGuard authorizationGuard,
            TradingDataEventStore eventStore,
            TradingDataEventPublisher eventPublisher,
            TradingDataMetricsPort metrics,
            Clock securityClock,
            PlatformTransactionManager transactionManager) {
        return new TradingDataIngestionService(
                authorizationGuard,
                eventStore,
                eventPublisher,
                metrics,
                securityClock,
                transactionManager);
    }
}
