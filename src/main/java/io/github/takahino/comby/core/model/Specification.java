package io.github.takahino.comby.core.model;

public record Specification(
    String matchTemplate,
    String rewriteTemplate,
    String rule
) {
    public static Specification of(String matchTemplate, String rewriteTemplate, String rule) {
        return new Specification(matchTemplate, rewriteTemplate, rule);
    }

    public static Specification of(String matchTemplate, String rewriteTemplate) {
        return new Specification(matchTemplate, rewriteTemplate, null);
    }
}
