package com.melon.foolsEngine.util;

public enum LogLevel {
    TRACE(0),
    DEBUG(1),
    INFO(2),
    WARN(3),
    ERROR(4),
    OFF(5);

    public final int severity;

    LogLevel(int severity) {
        this.severity = severity;
    }
}
