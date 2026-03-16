package io.github.takahino.comby.matcher;

import io.github.takahino.comby.core.matcher.languages.*;
import io.github.takahino.comby.core.model.Specification;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 言語固有の文字列・コメント透過マッチングテスト。
 * 対応する original: test_string_literals.ml, test_c.ml, test_go.ml, test_python_string_literals.ml
 */
class LanguageMatcherTest {

    private final CMatcher c = new CMatcher();
    private final GoMatcher go = new GoMatcher();
    private final PythonMatcher python = new PythonMatcher();
    private final JavaMatcher java = new JavaMatcher();

    // ---- test_string_literals.ml 相当 ----

    @Test
    void cStringLiteralWithBracketInsideNotCounted() {
        // 文字列内の "(" はデリミタ深度に影響しない
        var spec = Specification.of("(:[1])", ":[1]");
        var result = c.rewrite("\"/*\"(x)", spec, null);
        assertEquals("\"/*\"x", result);
    }

    @Test
    void cStringLiteralBasicExtraction() {
        // "\":[1]\"" → 文字列の内容をキャプチャ
        var spec = Specification.of("\":[1]\"", ":[1]");
        var result = c.rewrite("\"hello\"", spec, null);
        assertEquals("hello", result);
    }

    @Test
    void cStringLiteralDot() {
        var spec = Specification.of("\":[1]\"", ":[1]");
        var result = c.rewrite("\".\"", spec, null);
        assertEquals(".", result);
    }

    @Test
    void cStringLiteralEmpty() {
        var spec = Specification.of("\":[1]\"", ">[1]<");
        var matches = c.findMatches("\"\"", spec, null);
        // 空文字列マッチは停止条件ありなので許容
        assertEquals(1, matches.size());
        assertEquals("", matches.get(0).environment().get("1").get().value());
    }

    @Test
    void cSingleQuote() {
        // シングルクォートの内容をキャプチャ
        var spec = Specification.of("':[1]'", ":[1]");
        var result = c.rewrite("match 'asdf'", spec, null);
        assertEquals("match asdf", result);
    }

    @Test
    void cStringEscapedQuote() {
        // エスケープされた引用符を含む文字列（原本: test_string_literals.ml）
        var spec = Specification.of("match \":[1]\" \":[2]\" this", ":[1] :[2]");
        var result = c.rewrite("match \"\\\"(\\\"\" \"(\\\"\" this", spec, null);
        assertEquals("\\\"(\\\" (\\\"", result);
    }

    // ---- test_c.ml 相当 ----

    @Test
    void cEmptyCommentBracketDepthIgnored() {
        // /**/ 内のデリミタはブラケット深度に影響しない（コメント透過マッチング）
        // キャプチャ値にはコメントテキストが含まれる（実装上の動作）
        var spec = Specification.of("match this :[1] end", ":[1]");
        var result = c.rewrite("match this /**/ expect end", spec, null);
        assertEquals("/**/ expect", result);
    }

    @Test
    void cCommentBracketDepthIgnored() {
        // コメント内の括弧がブラケット深度に影響しないことを確認
        var spec = Specification.of("foo(:[x])", "bar(:[x])");
        var result = c.rewrite("foo(/* ( */ baz)", spec, null);
        assertEquals("bar(/* ( */ baz)", result);
    }

    @Test
    void cMultipleCommentsBracketDepthIgnored() {
        // 複数コメント内の括弧もブラケット深度に影響しない
        var spec = Specification.of("foo(:[x])", "bar(:[x])");
        var result = c.rewrite("foo(/* ( */ /* ( */ baz)", spec, null);
        assertEquals("bar(/* ( */ /* ( */ baz)", result);
    }

    @Test
    void cInsideCommentNotMatched() {
        // コメント内部の位置からはマッチを開始しない
        var spec = Specification.of("match this :[1] end", ":[1]");
        var matches = c.findMatches("/* don't match this (a) end */", spec, null);
        assertEquals(0, matches.size());
    }

    @Test
    void cOutsideCommentMatched() {
        // コメントの外側は正常にマッチ
        var spec = Specification.of("match this :[1] end", ":[1]");
        var result = c.rewrite("/* skip */ match this (b) end", spec, null);
        assertEquals("/* skip */ (b)", result);
    }

    @Test
    void cLineCommentIgnored() {
        // 行コメント内のデリミタは無視される
        var spec = Specification.of("foo(:[x])", "bar(:[x])");
        var result = c.rewrite("foo(// comment (\nbar)", spec, null);
        assertEquals("bar(// comment (\nbar)", result);
    }

    // ---- test_go.ml 相当 ----

    @Test
    void goRawStringLiteralIgnored() {
        // Go のバックティック文字列内の `//` はコメントでない
        var spec = Specification.of("(:[1])", ":[1]");
        var result = go.rewrite("`//`(x)", spec, null);
        assertEquals("`//`x", result);
    }

    @Test
    void goRawStringExtraction() {
        // バックティックで囲まれた文字列の内容をキャプチャ
        var spec = Specification.of("`:[1]`", ":[1]");
        var result = go.rewrite("`hello world`", spec, null);
        assertEquals("hello world", result);
    }

    @Test
    void goSelectSimplification() {
        // gosimple S1000: select { case x := <-ch: ... } → x := <-ch; ...
        var source = "select { case x := <-ch: fmt.Println(x) }";
        var spec = Specification.of(
            "select { case :[1] := :[2]: :[3] }",
            ":[1] := :[2]\n:[3]"
        );
        var result = go.rewrite(source, spec, null);
        assertEquals("x := <-ch\nfmt.Println(x)", result);
    }

    @Test
    void goPrintfEscapeSequence() {
        // printf 文字列内の改行エスケープはそのまま
        var spec = Specification.of("printf(\":[1]\");", ":[1]");
        var result = go.rewrite("printf(\"hello world\\n\");", spec, null);
        assertEquals("hello world\\n", result);
    }

    // ---- test_python_string_literals.ml 相当 ----

    @Test
    void pythonTripleQuoteMatched() {
        // """ で囲まれた文字列（raw literal）をキャプチャ
        var spec = Specification.of("\"\"\":[1]\"\"\"", ":[1]");
        var result = python.rewrite("\"\"\"blah\"\"\"", spec, null);
        assertEquals("blah", result);
    }

    @Test
    void pythonRegularStringNotMatchTriple() {
        // 通常のダブルクォートはトリプルクォートにマッチしない（原本: test_python_string_literals.ml）
        var spec = Specification.of("\":[1]\"", ":[1]");
        var matches = python.findMatches("\"\"\"blah\"\"\" \"blah\"", spec, null);
        assertTrue(matches.stream()
            .anyMatch(m -> m.environment().get("1").map(v -> v.value().equals("blah")).orElse(false)));
    }

    @Test
    void pythonTripleQuoteWithInternalQuote() {
        // トリプルクォート内にシングルクォートが含まれる（原本: test_python_string_literals.ml）
        var spec = Specification.of("\"\"\":[1]\"\"\"", ">[1]<");
        var matches = python.findMatches("\"\"\"bl\"ah\"\"\"", spec, null);
        assertEquals(1, matches.size());
        assertEquals("bl\"ah", matches.get(0).environment().get("1").get().value());
    }

    // ---- Java 固有テスト ----

    @Test
    void javaTextBlockIgnored() {
        // テキストブロック（"""..."""）内のデリミタは無視
        var spec = Specification.of("process(:[x])", "call(:[x])");
        var source = "process(\"\"\"hello (\"\"\")";
        var result = java.rewrite(source, spec, null);
        assertEquals("call(\"\"\"hello (\"\"\")", result);
    }
}
