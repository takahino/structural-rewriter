package io.github.takahino.comby.core.matcher.languages;

import io.github.takahino.comby.core.matcher.AbstractLanguageMatcher;
import io.github.takahino.comby.core.matcher.LanguageSyntax;
import io.github.takahino.comby.core.matcher.StructuralTokenizer;
import io.github.takahino.comby.core.matcher.tokenizer.CombyCLikeTokenizer;

import java.util.List;

public class CMatcher extends AbstractLanguageMatcher {
    private static final LanguageSyntax SYNTAX = new LanguageSyntax(
        List.of(
            new LanguageSyntax.StringDelimiter("\"", "\"", true),
            new LanguageSyntax.StringDelimiter("'", "'", true)
        ),
        List.of(
            LanguageSyntax.CommentSyntax.line("//"),
            LanguageSyntax.CommentSyntax.block("/*", "*/", false)
        ),
        List.of("(", "{", "["),
        List.of(")", "}", "]")
    );

    @Override public String name() { return "c"; }
    @Override public List<String> extensions() { return List.of(".c", ".h"); }
    @Override public LanguageSyntax syntax() { return SYNTAX; }
    @Override public StructuralTokenizer tokenizer() { return new CombyCLikeTokenizer(); }
}
