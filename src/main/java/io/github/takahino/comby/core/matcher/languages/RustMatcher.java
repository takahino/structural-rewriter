package io.github.takahino.comby.core.matcher.languages;

import io.github.takahino.comby.core.matcher.AbstractLanguageMatcher;
import io.github.takahino.comby.core.matcher.LanguageSyntax;
import io.github.takahino.comby.core.matcher.StructuralTokenizer;
import io.github.takahino.comby.core.matcher.tokenizer.CombyNestedBlockTokenizer;

import java.util.List;

public class RustMatcher extends AbstractLanguageMatcher {
    private static final LanguageSyntax SYNTAX = new LanguageSyntax(
        List.of(
            new LanguageSyntax.StringDelimiter("\"", "\"", true),
            new LanguageSyntax.StringDelimiter("'", "'", true)
        ),
        List.of(
            LanguageSyntax.CommentSyntax.line("//"),
            LanguageSyntax.CommentSyntax.block("/*", "*/", true)  // Rust はネストコメント対応
        ),
        List.of("(", "{", "["),
        List.of(")", "}", "]")
    );

    @Override public String name() { return "rust"; }
    @Override public List<String> extensions() { return List.of(".rs"); }
    @Override public LanguageSyntax syntax() { return SYNTAX; }
    @Override public StructuralTokenizer tokenizer() { return new CombyNestedBlockTokenizer(); }
}
