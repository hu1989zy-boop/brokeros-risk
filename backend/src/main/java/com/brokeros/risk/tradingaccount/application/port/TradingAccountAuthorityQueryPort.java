package com.brokeros.risk.tradingaccount.application.port;

import java.util.Optional;
import com.brokeros.risk.tradingaccount.application.AuthorityScopeState;
import com.brokeros.risk.tradingaccount.application.CompletedAuthorityOperation;
import com.brokeros.risk.tradingaccount.application.EligibilityPersistenceView;
import com.brokeros.risk.tradingaccount.application.TradingAccountState;
import com.brokeros.risk.tradingaccount.domain.AccountAuthorityScopeRef;
import com.brokeros.risk.tradingaccount.domain.AuthorityOperationId;
import com.brokeros.risk.tradingaccount.domain.ExternalAccountIdentity;
import com.brokeros.risk.tradingaccount.domain.TradingAccountRef;

public interface TradingAccountAuthorityQueryPort {
    Optional<CompletedAuthorityOperation> findOperation(AuthorityOperationId id);
    Optional<AuthorityScopeState> findScope(AccountAuthorityScopeRef ref);
    Optional<TradingAccountState> findByExternalIdentity(ExternalAccountIdentity identity);
    Optional<EligibilityPersistenceView> findEligibility(TradingAccountRef ref);
}
