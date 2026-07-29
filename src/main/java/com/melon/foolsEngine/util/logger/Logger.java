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

/**
 * Instance-based logger with per-module naming.
 * <pre>{@code
 *   Logger log = new Logger("Renderer");   // explicit name
 *   Logger log = new Logger();             // caller-derived name (abbreviated)
 * }</pre>
 */
public record Logger(String name) {

    private static final DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private static volatile LogLevel currentLevel = LogLevel.DEBUG;
    private static final List<Appender> appenders = new CopyOnWriteArrayList<>();

    static {
        appenders.add(new ConsoleAppender());
    }

    public Logger() {
        this(deriveCallerName());
    }

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

    public void trace(String msg) {
        log(LogLevel.TRACE, msg);
    }

    public void trace(String fmt, Object... args) {
        log(LogLevel.TRACE, fmt, args);
    }

    public void debug(String msg) {
        log(LogLevel.DEBUG, msg);
    }

    public void debug(String fmt, Object... args) {
        log(LogLevel.DEBUG, fmt, args);
    }

    public void info(String msg) {
        log(LogLevel.INFO, msg);
    }

    public void info(String fmt, Object... args) {
        log(LogLevel.INFO, fmt, args);
    }

    public void warn(String msg) {
        log(LogLevel.WARN, msg);
    }

    public void warn(String fmt, Object... args) {
        log(LogLevel.WARN, fmt, args);
    }

    public void error(String msg) {
        log(LogLevel.ERROR, msg);
    }

    public void error(String fmt, Object... args) {
        log(LogLevel.ERROR, fmt, args);
    }

    private void log(LogLevel level, String msg) {
        if (level.severity < currentLevel.severity) {
            return;
        }
        String timestamp = LocalTime.now().format(timeFmt);
        String formatted = "[" + timestamp + "][" + level.name() + "][" + name + "]" + msg;
        for (Appender a : appenders) {
            a.append(level, formatted);
        }
    }

    private void log(LogLevel level, String fmt, Object... args) {
        if (level.severity < currentLevel.severity) {
            return;
        }
        log(level, String.format(fmt, args));
    }

    private static String deriveCallerName() {
        return StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
                .walk(frames -> frames
                        .dropWhile(f -> f.getDeclaringClass() == Logger.class)
                        .findFirst()
                        .map(f -> abbreviate(f.getDeclaringClass().getName())))
                .orElse("Unknown");
    }

    private static String abbreviate(String fullName) {
        int lastDot = fullName.lastIndexOf('.');
        if (lastDot < 0) {
            return fullName;
        }
        String pkg = fullName.substring(0, lastDot);
        String cls = fullName.substring(lastDot + 1);
        String[] parts = pkg.split("\\.");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                sb.append('.');
            }
            String p = parts[i];
            sb.append(p.length() <= 2 ? p : p.substring(0, 2));
        }
        sb.append('.').append(cls);
        return sb.toString();
    }

    @FunctionalInterface
    public interface Appender {
        void append(LogLevel level, String message);
    }

    private static class ConsoleAppender implements Appender {
        @Override
        public void append(LogLevel level, String message) {
            PrintStream out = (level.severity >= LogLevel.WARN.severity) ? System.err : System.out;
            out.println(message);
        }
    }
}
