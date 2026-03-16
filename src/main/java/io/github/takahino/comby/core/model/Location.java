package io.github.takahino.comby.core.model;

public record Location(int offset, int line, int column) {
    public static Location of(int offset, int line, int column) {
        return new Location(offset, line, column);
    }
    public static Location zero() {
        return new Location(0, 1, 1);
    }
}
