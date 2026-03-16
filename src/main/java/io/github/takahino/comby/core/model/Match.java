package io.github.takahino.comby.core.model;

public record Match(
    String matchedText,
    Range range,
    MatchEnvironment environment,
    String filePath
) {
    public static Match of(String matchedText, Range range, MatchEnvironment environment, String filePath) {
        return new Match(matchedText, range, environment, filePath);
    }
}
