package io.github.takahino.comby.core.matcher.languages;

import io.github.takahino.comby.core.matcher.AbstractLanguageMatcher;
import io.github.takahino.comby.core.matcher.LanguageSyntax;
import io.github.takahino.comby.core.matcher.StructuralTokenizer;
import io.github.takahino.comby.core.matcher.tokenizer.CombyOcamlTokenizer;

import java.util.List;

public class OcamlMatcher extends AbstractLanguageMatcher {
    private static final LanguageSyntax SYNTAX = new LanguageSyntax(
        List.of(
            new LanguageSyntax.StringDelimiter("\"", "\"", true)
        ),
        List.of(
            LanguageSyntax.CommentSyntax.block("(*", "*)", true)  // OCaml はネストコメント
        ),
        List.of("(", "{", "["),
        List.of(")", "}", "]")
    );

    @Override public String name() { return "ocaml"; }
    @Override public List<String> extensions() { return List.of(".ml", ".mli"); }
    @Override public LanguageSyntax syntax() { return SYNTAX; }
    @Override public StructuralTokenizer tokenizer() { return new CombyOcamlTokenizer(); }
}
