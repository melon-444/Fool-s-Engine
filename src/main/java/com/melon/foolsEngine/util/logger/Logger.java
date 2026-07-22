// foolsEngine - A custom 3D game engine in Java
// Copyright (C) 2026  melon_444
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.

package com.melon.foolsEngine.util.logger;

import java.io.PrintStream;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class Logger {

    private static volatile LogLevel currentLevel = LogLevel.DEBUG;
    private static final List<Appender> appenders = new CopyOnWriteArrayList<>();
    private static final DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    static {
        appenders.add(new ConsoleAppender());
    }

    private Logger() {}

    public static void setLevel(LogLevel level) {
        currentLevel = level;
    }

    public static LogLevel getLevel() {
        return currentLevel;
    }

    public static void addAppender(Appender appender) {
        appenders.add(appender);
    }

    public static void removeAppender(Appender appender) {
        appenders.remove(appender);
    }

    public static void trace(String msg) {
        log(LogLevel.TRACE, msg);
    }

    public static void trace(String fmt, Object... args) {
        log(LogLevel.TRACE, fmt, args);
    }

    public static void debug(String msg) {
        log(LogLevel.DEBUG, msg);
    }

    public static void debug(String fmt, Object... args) {
        log(LogLevel.DEBUG, fmt, args);
    }

    public static void info(String msg) {
        log(LogLevel.INFO, msg);
    }

    public static void info(String fmt, Object... args) {
        log(LogLevel.INFO, fmt, args);
    }

    public static void warn(String msg) {
        log(LogLevel.WARN, msg);
    }

    public static void warn(String fmt, Object... args) {
        log(LogLevel.WARN, fmt, args);
    }

    public static void error(String msg) {
        log(LogLevel.ERROR, msg);
    }

    public static void error(String fmt, Object... args) {
        log(LogLevel.ERROR, fmt, args);
    }

    private static void log(LogLevel level, String msg) {
        if (level.severity < currentLevel.severity) {
            return;
        }
        for (Appender a : appenders) {
            a.append(level, msg);
        }
    }

    private static void log(LogLevel level, String fmt, Object... args) {
        if (level.severity < currentLevel.severity) {
            return;
        }
        String msg = String.format(fmt, args);
        for (Appender a : appenders) {
            a.append(level, msg);
        }
    }

    @FunctionalInterface
    public interface Appender {
        void append(LogLevel level, String message);
    }

    private static class ConsoleAppender implements Appender {
        @Override
        public void append(LogLevel level, String message) {
            PrintStream out = (level.severity >= LogLevel.WARN.severity) ? System.err : System.out;
            String timestamp = LocalTime.now().format(timeFmt);
            out.printf("[%s] [%-5s] %s%n", timestamp, level.name(), message);
        }
    }
}
