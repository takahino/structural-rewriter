package io.github.takahino.comby.core.model;

import java.util.List;

public record RewriteResult(
    String rewrittenSource,
    List<Replacement> replacements
) {
    public record Replacement(Range range, String replacement) {}

    public static RewriteResult of(String rewrittenSource, List<Replacement> replacements) {
        return new RewriteResult(rewrittenSource, replacements);
    }
}
