package com.brokeros.risk.tradingdata.application.port;

import com.brokeros.risk.tradingdata.domain.TradingDataEnvelope;

public interface TradingDataEventPublisher {

    void publish(TradingDataEnvelope envelope);
}
