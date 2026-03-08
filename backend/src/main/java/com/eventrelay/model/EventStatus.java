package com.eventrelay.model;

public enum EventStatus {
    RECEIVED,
    DUPLICATE,
    PROCESSING,
    PROCESSED,
    FAILED,
    DEAD_LETTER
}
