package com.brokeros.risk.tradingaccount.application.port;

import com.brokeros.risk.tradingaccount.application.AccountProvisioningResult;
import com.brokeros.risk.tradingaccount.application.AuthorizedMutationContext;
import com.brokeros.risk.tradingaccount.application.ChangeAccountLifecycleSpec;
import com.brokeros.risk.tradingaccount.application.ChangeScopeLifecycleSpec;
import com.brokeros.risk.tradingaccount.application.LifecycleChangeResult;
import com.brokeros.risk.tradingaccount.application.RegisterAccountSpec;
import com.brokeros.risk.tradingaccount.application.RegisterScopeSpec;
import com.brokeros.risk.tradingaccount.application.ScopeProvisioningResult;

public interface TradingAccountAuthorityMutationPort {
    ScopeProvisioningResult registerScope(RegisterScopeSpec spec, AuthorizedMutationContext context);
    AccountProvisioningResult registerAccount(RegisterAccountSpec spec, AuthorizedMutationContext context);
    LifecycleChangeResult changeScopeLifecycle(ChangeScopeLifecycleSpec spec, AuthorizedMutationContext context);
    LifecycleChangeResult changeAccountLifecycle(ChangeAccountLifecycleSpec spec, AuthorizedMutationContext context);
}
