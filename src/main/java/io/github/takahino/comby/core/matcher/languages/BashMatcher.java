package io.github.takahino.comby.core.matcher.languages;

import io.github.takahino.comby.core.matcher.AbstractLanguageMatcher;
import io.github.takahino.comby.core.matcher.LanguageSyntax;
import io.github.takahino.comby.core.matcher.StructuralTokenizer;
import io.github.takahino.comby.core.matcher.tokenizer.CombyHashTokenizer;

import java.util.List;

public class BashMatcher extends AbstractLanguageMatcher {
    private static final LanguageSyntax SYNTAX = new LanguageSyntax(
        List.of(
            new LanguageSyntax.StringDelimiter("\"", "\"", true),
            new LanguageSyntax.StringDelimiter("'", "'", false)
        ),
        List.of(LanguageSyntax.CommentSyntax.line("#")),
        List.of("(", "{", "["),
        List.of(")", "}", "]")
    );

    @Override public String name() { return "bash"; }
    @Override public List<String> extensions() { return List.of(".sh", ".bash"); }
    @Override public LanguageSyntax syntax() { return SYNTAX; }
    @Override public StructuralTokenizer tokenizer() { return new CombyHashTokenizer(); }
}
