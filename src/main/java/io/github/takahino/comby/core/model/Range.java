package io.github.takahino.comby.core.model;

public record Range(Location start, Location end) {
    public static Range of(Location start, Location end) {
        return new Range(start, end);
    }
    public int length() {
        return end.offset() - start.offset();
    }
}
