package com.carrental.coverage;

import java.util.List;

public class DepositHandler {

    private final DepositRepository repository;
    private final InsuranceCalculator calculator;

    public DepositHandler(DepositRepository repository, InsuranceCalculator calculator) {
        this.repository = repository;
        this.calculator = calculator;
    }

    /** Шаг B цепочки: заблокировать на карте страховку и залог. Сбой возникает ДО save, на диске ничего не остаётся. */
    public Deposit authorize(String reservationId, String customerId, InsurancePlan plan, long days) {
        long amount = calculator.totalToAuthorize(plan, days);
        long available = repository.getCardLimit(customerId) - repository.sumActiveAmount(customerId);
        if (amount > available) {
            throw new InsufficientFundsException(
                    "Customer " + customerId + " needs " + amount + " but only " + available + " is available");
        }
        Deposit deposit = Deposit.authorize(reservationId, customerId, amount);
        repository.save(deposit);
        return deposit;
    }

    public Deposit capture(String depositId) {
        Deposit d = load(depositId);
        d.capture();
        repository.save(d);
        return d;
    }

    /** Компенсация шага B: снять холд. */
    public Deposit release(String depositId) {
        Deposit d = load(depositId);
        d.release();
        repository.save(d);
        return d;
    }

    public void setCardLimit(String customerId, long limit) {
        repository.saveCardLimit(customerId, limit);
    }

    public long availableFunds(String customerId) {
        return repository.getCardLimit(customerId) - repository.sumActiveAmount(customerId);
    }

    public Deposit get(String depositId) {
        return load(depositId);
    }

    public List<Deposit> all() {
        return repository.findAll();
    }

    private Deposit load(String depositId) {
        return repository.findById(depositId)
                .orElseThrow(() -> new DepositNotFoundException(depositId));
    }
}