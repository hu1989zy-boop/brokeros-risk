package com.brokeros.risk.tradingaccount.infrastructure.configuration;

import com.brokeros.risk.security.application.RegisteredServiceDescriptor;

public final class TradingAccountReferenceProvisionerDescriptor
        implements RegisteredServiceDescriptor {

    public static final String SERVICE_CODE = "trading-account-reference-provisioner";

    @Override
    public String serviceCode() {
        return SERVICE_CODE;
    }
}
