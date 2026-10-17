package com.carrental.coverage;

public class InsuranceCalculator {

    /** Страховой взнос: тариф в день * количество дней. */
    public long premium(InsurancePlan plan, long days) {
        if (days <= 0) {
            throw new IllegalArgumentException("Days must be positive: " + days);
        }
        long dailyRate = switch (plan) {
            case BASIC -> 10;
            case FULL -> 25;
        };
        return days * dailyRate;
    }

    /** Залог: полная страховка снижает залог. */
    public long securityDeposit(InsurancePlan plan) {
        return switch (plan) {
            case BASIC -> 200;
            case FULL -> 100;
        };
    }

    /** Сколько нужно заблокировать на карте в сумме. */
    public long totalToAuthorize(InsurancePlan plan, long days) {
        return premium(plan, days) + securityDeposit(plan);
    }
}