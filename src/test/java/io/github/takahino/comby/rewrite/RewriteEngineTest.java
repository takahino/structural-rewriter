package io.github.takahino.comby.rewrite;

import io.github.takahino.comby.core.matcher.LanguageSyntax;
import io.github.takahino.comby.core.matcher.MatchEngine;
import io.github.takahino.comby.core.model.Specification;
import io.github.takahino.comby.core.rewrite.RewriteEngine;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RewriteEngineTest {

    private final MatchEngine engine = new MatchEngine(LanguageSyntax.generic());

    @Test
    void basicSubstitution() {
        var source = "foo(bar)";
        var spec = Specification.of("foo(:[x])", "baz(:[x])");
        var matches = engine.findAll(source, spec.matchTemplate(), null);
        var result = RewriteEngine.rewrite(source, matches, spec.rewriteTemplate());
        assertEquals("baz(bar)", result);
    }

    @Test
    void negateCondition() {
        var source = "if (x > 0) { return; }";
        var spec = Specification.of("if (:[cond])", "if (!(:[cond]))");
        var matches = engine.findAll(source, spec.matchTemplate(), null);
        var result = RewriteEngine.rewrite(source, matches, spec.rewriteTemplate());
        assertEquals("if (!(x > 0)) { return; }", result);
    }

    @Test
    void uppercaseAttribute() {
        var source = "hello";
        var spec = Specification.of(":[x]", ":[x.UPPERCASE]");
        var matches = engine.findAll(source, spec.matchTemplate(), null);
        var result = RewriteEngine.rewrite(source, matches, spec.rewriteTemplate());
        assertEquals("HELLO", result);
    }

    @Test
    void multipleMatchesRewrites() {
        var source = "foo(a) and foo(b)";
        var spec = Specification.of("foo(:[x])", "bar(:[x])");
        var matches = engine.findAll(source, spec.matchTemplate(), null);
        var result = RewriteEngine.rewrite(source, matches, spec.rewriteTemplate());
        assertEquals("bar(a) and bar(b)", result);
    }

    // ---- test_rewrite_attributes.ml 相当 ----

    @Test
    void lowercaseAttribute() {
        // :[a.lowercase] → 小文字変換
        var source = "LOWERCASE";
        var spec = Specification.of(":[[a]]", ":[a.lowercase]");
        var matches = engine.findAll(source, spec.matchTemplate(), null);
        var result = RewriteEngine.rewrite(source, matches, spec.rewriteTemplate());
        assertEquals("lowercase", result);
    }

    @Test
    void capitalizeAttribute() {
        // :[a.Capitalize] → 先頭大文字
        var source = "capitalize";
        var spec = Specification.of(":[[a]]", ":[a.Capitalize]");
        var matches = engine.findAll(source, spec.matchTemplate(), null);
        var result = RewriteEngine.rewrite(source, matches, spec.rewriteTemplate());
        assertEquals("Capitalize", result);
    }

    @Test
    void upperCamelCaseAttribute() {
        // :[a.UpperCamelCase] → スネークケース→アッパーキャメル
        var source = "upper_camel_case";
        var spec = Specification.of(":[a.]", ":[a.UpperCamelCase]");
        var matches = engine.findAll(source, spec.matchTemplate(), null);
        var result = RewriteEngine.rewrite(source, matches, spec.rewriteTemplate());
        assertEquals("UpperCamelCase", result);
    }

    @Test
    void lowerCamelCaseAttribute() {
        // :[a.lowerCamelCase] → スネークケース→ローワーキャメル
        var source = "lower_camel_case";
        var spec = Specification.of(":[a.]", ":[a.lowerCamelCase]");
        var matches = engine.findAll(source, spec.matchTemplate(), null);
        var result = RewriteEngine.rewrite(source, matches, spec.rewriteTemplate());
        assertEquals("lowerCamelCase", result);
    }

    @Test
    void upperSnakeCaseAttribute() {
        // :[a.UPPER_SNAKE_CASE] → キャメルケース→アッパースネーク
        var source = "UpperSnakeCase";
        var spec = Specification.of(":[[a]]", ":[a.UPPER_SNAKE_CASE]");
        var matches = engine.findAll(source, spec.matchTemplate(), null);
        var result = RewriteEngine.rewrite(source, matches, spec.rewriteTemplate());
        assertEquals("UPPER_SNAKE_CASE", result);
    }

    @Test
    void linesAttribute() {
        // :[x.lines] → キャプチャ内の改行文字数（comby の .lines 属性と同じ）
        var source = "{\nline1\nline2\n}";
        var spec = Specification.of("{:[x]}", ":[x.lines]");
        var matches = engine.findAll(source, spec.matchTemplate(), null);
        var result = RewriteEngine.rewrite(source, matches, spec.rewriteTemplate());
        // "\nline1\nline2\n" の改行数=3
        assertEquals("3", result);
    }

    @Test
    void linesTwoLines() {
        // 2行コンテンツのブロック → 2
        var source = "{\nfoo\nbar\n}";
        var spec = Specification.of("{:[x]}", ":[x.lines]");
        var matches = engine.findAll(source, spec.matchTemplate(), null);
        var result = RewriteEngine.rewrite(source, matches, spec.rewriteTemplate());
        assertEquals("3", result);
    }

    @Test
    void noMatchesReturnsOriginal() {
        var source = "hello world";
        var spec = Specification.of("xyz", "abc");
        var matches = engine.findAll(source, spec.matchTemplate(), null);
        var result = RewriteEngine.rewrite(source, matches, spec.rewriteTemplate());
        assertEquals("hello world", result);
    }
}
