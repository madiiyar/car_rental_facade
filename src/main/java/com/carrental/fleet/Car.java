package com.carrental.fleet;

/** Справочная запись об автомобиле. dailyRate в целых денежных единицах. */
public record Car(String id, String model, String category, long dailyRate) {
}