package io.github.takahino.comby.matcher;

import io.github.takahino.comby.core.matcher.LanguageSyntax;
import io.github.takahino.comby.core.matcher.MatchEngine;
import io.github.takahino.comby.core.model.Match;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MatchEngineTest {

    private final MatchEngine engine = new MatchEngine(LanguageSyntax.generic());

    @Test
    void matchSimpleConstant() {
        var matches = engine.findAll("hello world", "hello", null);
        assertEquals(1, matches.size());
        assertEquals("hello", matches.get(0).matchedText());
    }

    @Test
    void matchWithHole() {
        var matches = engine.findAll("foo(bar)", "foo(:[arg])", null);
        assertEquals(1, matches.size());
        assertEquals("foo(bar)", matches.get(0).matchedText());
        assertEquals("bar", matches.get(0).environment().get("arg").get().value());
    }

    @Test
    void matchBalancedParens() {
        var matches = engine.findAll("foo(bar(baz))", "foo(:[arg])", null);
        assertEquals(1, matches.size());
        assertEquals("bar(baz)", matches.get(0).environment().get("arg").get().value());
    }

    @Test
    void matchMultipleOccurrences() {
        var matches = engine.findAll("foo(a) foo(b)", "foo(:[x])", null);
        assertEquals(2, matches.size());
        assertEquals("a", matches.get(0).environment().get("x").get().value());
        assertEquals("b", matches.get(1).environment().get("x").get().value());
    }

    @Test
    void matchAlphanumHole() {
        var matches = engine.findAll("int foo = 42;", "int :[[name]] = :[val];", null);
        assertEquals(1, matches.size());
        assertEquals("foo", matches.get(0).environment().get("name").get().value());
        assertEquals("42", matches.get(0).environment().get("val").get().value());
    }

    @Test
    void matchSameVariableMultipleTimes() {
        // :[x] + :[x] should only match where both sides are equal
        var matches = engine.findAll("a + a", ":[x] + :[x]", null);
        assertEquals(1, matches.size());

        var noMatch = engine.findAll("a + b", ":[x] + :[x]", null);
        assertEquals(0, noMatch.size());
    }

    @Test
    void matchIfCondition() {
        var source = "if (x > 0) { return; }";
        var matches = engine.findAll(source, "if (:[cond])", null);
        assertEquals(1, matches.size());
        assertEquals("x > 0", matches.get(0).environment().get("cond").get().value());
    }

    @Test
    void matchNestedFunctionCalls() {
        var matches = engine.findAll("foo(a, bar(b, c))", ":[fn](:[args])", null);
        assertTrue(matches.size() >= 1);
        // 最初のマッチは foo(a, bar(b, c))
        assertEquals("a, bar(b, c)", matches.get(0).environment().get("args").get().value());
    }

    @Test
    void noMatchWhenNotFound() {
        var matches = engine.findAll("hello world", "xyz", null);
        assertEquals(0, matches.size());
    }

    // ---- test_generic.ml 相当 ----

    @Test
    void extractBetweenContexts() {
        // "a :[1] c d" → :[1]="b"
        var matches = engine.findAll("a b c d", "a :[1] c d", null);
        assertEquals(1, matches.size());
        assertEquals("b", matches.get(0).environment().get("1").get().value());
    }

    @Test
    void extractMiddleRange() {
        // "a :[1] d" → :[1]="b c"
        var matches = engine.findAll("a b c d", "a :[1] d", null);
        assertEquals(1, matches.size());
        assertEquals("b c", matches.get(0).environment().get("1").get().value());
    }

    @Test
    void extractWithLeadingContext() {
        // "a :[1]" → :[1]="b c d"
        var matches = engine.findAll("a b c d", "a :[1]", null);
        assertEquals(1, matches.size());
        assertEquals("b c d", matches.get(0).environment().get("1").get().value());
    }

    @Test
    void twoVariablesWithMiddleContext() {
        // "a :[2] :[1] d" → :[2]="b", :[1]="c"
        var matches = engine.findAll("a b c d", "a :[2] :[1] d", null);
        assertEquals(1, matches.size());
        assertEquals("b", matches.get(0).environment().get("2").get().value());
        assertEquals("c", matches.get(0).environment().get("1").get().value());
    }

    @Test
    void extractWithColonSuffix() {
        // ":[1]:" → :[1]="x"
        var matches = engine.findAll("x:", ":[1]:", null);
        assertEquals(1, matches.size());
        assertEquals("x", matches.get(0).environment().get("1").get().value());
    }

    @Test
    void extractInsideParens() {
        // "(:[1]) d" → :[1]="a b c"
        var matches = engine.findAll("(a b c) d", "(:[1]) d", null);
        assertEquals(1, matches.size());
        assertEquals("a b c", matches.get(0).environment().get("1").get().value());
    }

    @Test
    void twoVariablesInsideParens() {
        // "(:[1] b :[2]) d" → :[1]="a", :[2]="c"
        var matches = engine.findAll("(a b c) d", "(:[1] b :[2]) d", null);
        assertEquals(1, matches.size());
        assertEquals("a", matches.get(0).environment().get("1").get().value());
        assertEquals("c", matches.get(0).environment().get("2").get().value());
    }

    @Test
    void deeplyNestedExtraction() {
        // "((:[1]) q) d" → :[1]="a b c"
        var matches = engine.findAll("((a b c) q) d", "((:[1]) q) d", null);
        assertEquals(1, matches.size());
        assertEquals("a b c", matches.get(0).environment().get("1").get().value());
    }

    @Test
    void outerFunctionSkipsInner() {
        // "outer(:[1],src)" matches "outer(inner(dst,src),src)" → :[1]="inner(dst,src)"
        var matches = engine.findAll("outer(inner(dst,src),src)", "outer(:[1],src)", null);
        assertEquals(1, matches.size());
        assertEquals("inner(dst,src)", matches.get(0).environment().get("1").get().value());
    }

    @Test
    void matchFailsWhenSuffixMissing() {
        // "a :[1] b c" は "a x b bbq" にはマッチしない（"c" が存在しないため）
        var matches = engine.findAll("a x b bbq", "a :[1] b c", null);
        assertEquals(0, matches.size());
    }

    @Test
    void doubleSpacePattern() {
        // ":[1]  :[2]" → スペース2個を含むパターン
        var matches = engine.findAll("two  spaces", ":[1]  :[2]", null);
        assertEquals(1, matches.size());
        assertEquals("two", matches.get(0).environment().get("1").get().value());
        assertEquals("spaces", matches.get(0).environment().get("2").get().value());
    }

    @Test
    void matchIgnoresStringDelimiters() {
        var engine = new MatchEngine(new LanguageSyntax(
            List.of(new LanguageSyntax.StringDelimiter("\"", "\"", true)),
            List.of(),
            List.of("(", "{", "["),
            List.of(")", "}", "]")
        ));
        // 文字列内の括弧はバランスカウントに含まれない
        var matches = engine.findAll("foo(\"(bar)\", baz)", "foo(:[args])", null);
        assertEquals(1, matches.size());
        assertEquals("\"(bar)\", baz", matches.get(0).environment().get("args").get().value());
    }
}
