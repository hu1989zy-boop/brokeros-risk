package com.brokeros.risk.audit.application.port;

import com.brokeros.risk.audit.domain.AuditRecord;

public interface AuditRecordWriter {

    void append(AuditRecord record);
}
