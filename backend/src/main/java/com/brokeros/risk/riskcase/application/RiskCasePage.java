package com.brokeros.risk.riskcase.application;

import java.util.List;

public record RiskCasePage<T>(
        List<T> items,
        int page,
        int size,
        boolean hasNext) {

    public RiskCasePage {
        items = List.copyOf(items);
        if (page < 0 || size < 1 || size > 100 || items.size() > size) {
            throw new IllegalArgumentException("risk case page is outside its bounded contract");
        }
    }
}
