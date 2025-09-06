package com.openmarket.utils;

import lombok.experimental.UtilityClass;
import org.slf4j.Logger;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

@UtilityClass
public class LogWrapper {

    public static <T> T logWrap(Logger logger, String operationName, Supplier<T> supplier) {
        logger.info("{} started", operationName);
        T result = supplier.get();
        logger.info("{} finished", operationName);
        return result;
    }

    public static void logWrap(Logger logger, String operationName, BooleanSupplier supplier) {
        logger.info("{} started", operationName);
        if (supplier.getAsBoolean()) {
            logger.info("{} finished", operationName);
        } else {
            logger.info("{} failed", operationName);
        }
    }
}
