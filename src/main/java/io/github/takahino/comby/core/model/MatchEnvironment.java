package io.github.takahino.comby.core.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class MatchEnvironment {
    private final Map<String, CapturedValue> bindings;

    public MatchEnvironment() {
        this.bindings = new HashMap<>();
    }

    private MatchEnvironment(Map<String, CapturedValue> bindings) {
        this.bindings = new HashMap<>(bindings);
    }

    public Optional<CapturedValue> get(String name) {
        return Optional.ofNullable(bindings.get(name));
    }

    public boolean contains(String name) {
        return bindings.containsKey(name);
    }

    public MatchEnvironment bind(String name, CapturedValue value) {
        var next = new MatchEnvironment(bindings);
        next.bindings.put(name, value);
        return next;
    }

    public Map<String, CapturedValue> all() {
        return Collections.unmodifiableMap(bindings);
    }

    public boolean isEmpty() {
        return bindings.isEmpty();
    }

    public static MatchEnvironment empty() {
        return new MatchEnvironment();
    }
}
