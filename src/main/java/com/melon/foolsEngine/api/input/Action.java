package com.melon.foolsEngine.api.input;

import com.melon.foolsEngine.util.SignalType;

@FunctionalInterface
public interface Action {
    SignalType Type();
}
