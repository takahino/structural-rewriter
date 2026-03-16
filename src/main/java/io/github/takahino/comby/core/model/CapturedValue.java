package io.github.takahino.comby.core.model;

public record CapturedValue(String name, String value, Range range) {
    public static CapturedValue of(String name, String value, Range range) {
        return new CapturedValue(name, value, range);
    }
}
