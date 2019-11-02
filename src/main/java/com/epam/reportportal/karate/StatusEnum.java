/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.epam.reportportal.karate;

import java.util.Arrays;
import java.util.Optional;

/**
 *
 * @author kvijayapandian
 */
public enum StatusEnum {

    IN_PROGRESS("", false),
    PASSED("passed", true),
    FAILED("failed", false),
    STOPPED("stopped", false),
    SKIPPED("skipped", false),
    INTERRUPTED("failed", false),
    CANCELLED("cancelled", false);

    private final String executionCounterField;

    private final boolean positive;

    StatusEnum(String executionCounterField, boolean isPositive) {
        this.executionCounterField = executionCounterField;
        this.positive = isPositive;
    }

    public static Optional<StatusEnum> fromValue(String value) {
        return Arrays.stream(StatusEnum.values()).filter(status -> status.name().equalsIgnoreCase(value)).findAny();
    }

    public static boolean isPresent(String name) {
        return fromValue(name).isPresent();
    }

    public String getExecutionCounterField() {
        return executionCounterField;
    }

    public boolean isPositive() {
        return positive;
    }
}
