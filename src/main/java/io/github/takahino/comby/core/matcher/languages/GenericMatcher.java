package io.github.takahino.comby.core.matcher.languages;

import io.github.takahino.comby.core.matcher.AbstractLanguageMatcher;
import io.github.takahino.comby.core.matcher.LanguageSyntax;
import io.github.takahino.comby.core.matcher.StructuralTokenizer;
import io.github.takahino.comby.core.matcher.tokenizer.CombyCLikeTokenizer;

import java.util.List;

public class GenericMatcher extends AbstractLanguageMatcher {
    @Override public String name() { return "generic"; }
    @Override public List<String> extensions() { return List.of(".generic", ".txt"); }
    @Override public LanguageSyntax syntax() { return LanguageSyntax.generic(); }
    @Override public StructuralTokenizer tokenizer() { return new CombyCLikeTokenizer(); }
}
