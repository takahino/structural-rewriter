package io.github.takahino.comby.matcher;

import io.github.takahino.comby.core.matcher.LanguageSyntax;
import io.github.takahino.comby.core.matcher.MatchEngine;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ホール種別の拡張テスト。
 * 対応する original: test_hole_extensions.ml, test_regex_holes.ml
 */
class HoleExtensionTest {

    private final MatchEngine engine = new MatchEngine(LanguageSyntax.generic());

    // ---- NON_SPACE ホール :[x.] ----

    @Test
    void nonSpaceHoleMatchesUntilWhitespace() {
        // ":[x.]" は空白以外の連続した文字をキャプチャ
        var matches = engine.findAll("foo.bar.quux", ":[x.]", null);
        assertFalse(matches.isEmpty());
        assertEquals("foo.bar.quux", matches.get(0).environment().get("x").get().value());
    }

    @Test
    void nonSpaceHoleStopsAtWhitespace() {
        // "   foo.     foo.bar.quux derp" から各非空白トークンをキャプチャ
        var matches = engine.findAll("   foo.     foo.bar.quux derp", ":[x.]", null);
        assertEquals(3, matches.size());
        assertEquals("foo.", matches.get(0).environment().get("x").get().value());
        assertEquals("foo.bar.quux", matches.get(1).environment().get("x").get().value());
        assertEquals("derp", matches.get(2).environment().get("x").get().value());
    }

    @Test
    void nonSpaceHoleWithSuffixContext() {
        // "foo.:[x.]ux" → NON_SPACE ホールが停止条件 "ux" の前まで取得 → "bar.qu"
        // NON_SPACE は空白 OR 次のConstantのどちらかで停止する
        var matches = engine.findAll("   foo.     foo.bar.quux derp", "foo.:[x.]ux", null);
        assertEquals(1, matches.size());
        assertEquals("bar.qu", matches.get(0).environment().get("x").get().value());
    }

    // ---- BLANK ホール :[ x] ----

    @Test
    void blankHoleCapturesWhitespaceOnly() {
        // ":[ x]" は空白文字のみをキャプチャ
        var source = "   foo.     foo.bar.quux derp";
        var matches = engine.findAll(source, ":[ x]", null);
        assertFalse(matches.isEmpty());
        // 最初のマッチは先頭の "   "
        assertTrue(matches.get(0).environment().get("x").get().value().isBlank());
        assertFalse(matches.get(0).environment().get("x").get().value().isEmpty());
    }

    @Test
    void blankHoleWithContext() {
        // "foo:[ x]bar" → :[ x] は foo と bar の間の空白をキャプチャ
        var matches = engine.findAll("foo   bar", "foo:[ x]bar", null);
        assertEquals(1, matches.size());
        assertEquals("   ", matches.get(0).environment().get("x").get().value());
    }

    // ---- LINE ホール :[x\n] ----

    @Test
    void lineHoleCapturesUntilNewline() {
        // ":[x\n]" は改行まで（改行を含まない）をキャプチャ
        var matches = engine.findAll("first line\nsecond line", ":[x\\n]", null);
        assertFalse(matches.isEmpty());
        assertEquals("first line", matches.get(0).environment().get("x").get().value());
    }

    @Test
    void lineHoleWithLeadingContext() {
        // "prefix :[x\n]" → 各行の prefix 以降をキャプチャ
        var source = "prefix foo\nprefix bar\nprefix baz";
        var matches = engine.findAll(source, "prefix :[x\\n]", null);
        assertEquals(3, matches.size());
        assertEquals("foo", matches.get(0).environment().get("x").get().value());
        assertEquals("bar", matches.get(1).environment().get("x").get().value());
        assertEquals("baz", matches.get(2).environment().get("x").get().value());
    }

    // ---- ALPHANUM ホール :[[x]] ----

    @Test
    void alphanumHoleCapturesIdentifier() {
        // EVERYTHING ホール + 停止条件でサフィックス前まで取得（原本: test_hole_extensions.ml）
        // "foo.b:[x]r.quux" で :[x] は停止条件 "r.quux" の直前まで取得 → "a"
        var matches = engine.findAll("   foo.     foo.bar.quux derp", "foo.b:[x]r.quux", null);
        assertEquals(1, matches.size());
        assertEquals("a", matches.get(0).environment().get("x").get().value());
    }

    @Test
    void alphanumImplicitEquality() {
        // ":[[x]] :[[m]] :[[x]]" → 同一変数は等しくなければならない
        // "a b a" はマッチ → :[m]="b"
        var matchYes = engine.findAll("a b a", ":[[x]] :[[m]] :[[x]]", null);
        assertEquals(1, matchYes.size());
        assertEquals("b", matchYes.get(0).environment().get("m").get().value());

        // "a b c" はマッチしない（最初と最後が異なる）
        var matchNo = engine.findAll("a b c", ":[[x]] :[[m]] :[[x]]", null);
        assertEquals(0, matchNo.size());
    }

    @Test
    void alphanumWildcardIgnored() {
        // ":[[x]] :[[_]] :[[_]]" → アンダースコアは捨て変数
        var matches = engine.findAll("a b c", ":[[x]] :[[_]] :[[_]]", null);
        assertEquals(1, matches.size());
        assertEquals("a", matches.get(0).environment().get("x").get().value());
    }

    // ---- REGEX ホール :[x~regex] ----

    @Test
    void regexHoleBasic() {
        // ":[x~\\w+]" → "foo" にマッチ
        var matches = engine.findAll("foo", ":[x~\\w+]", null);
        assertFalse(matches.isEmpty());
        assertEquals("foo", matches.get(0).environment().get("x").get().value());
    }

    @Test
    void regexHoleWithCharacterClass() {
        // ":[x~[0-9]+]" → 数字列をキャプチャ
        var matches = engine.findAll("abc 123 def", ":[x~[0-9]+]", null);
        assertEquals(1, matches.size());
        assertEquals("123", matches.get(0).environment().get("x").get().value());
    }

    @Test
    void regexHoleWithContext() {
        // "(:[x~[^)]+])" → 閉じ括弧以外の文字をキャプチャ
        var matches = engine.findAll("(hello world)", "(:[x~[^)]+])", null);
        assertEquals(1, matches.size());
        assertEquals("hello world", matches.get(0).environment().get("x").get().value());
    }

    @Test
    void regexHoleOptional() {
        // ":[x~no(vember)?]" → "no" と "november" の両方にマッチ
        var source = "no november";
        var matches = engine.findAll(source, ":[x~no(vember)?]", null);
        assertEquals(2, matches.size());
        assertEquals("no", matches.get(0).environment().get("x").get().value());
        assertEquals("november", matches.get(1).environment().get("x").get().value());
    }

    @Test
    void regexHoleMultipleMatches() {
        // ":[x~[a-z]+]" → 各小文字単語をキャプチャ
        var matches = engine.findAll("foo bar baz", ":[x~[a-z]+]", null);
        assertEquals(3, matches.size());
        assertEquals("foo", matches.get(0).environment().get("x").get().value());
        assertEquals("bar", matches.get(1).environment().get("x").get().value());
        assertEquals("baz", matches.get(2).environment().get("x").get().value());
    }
}
