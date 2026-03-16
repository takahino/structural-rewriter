package io.github.takahino.comby.integration;

import io.github.takahino.comby.core.matcher.languages.CppMatcher;
import io.github.takahino.comby.core.matcher.languages.JavaMatcher;
import io.github.takahino.comby.core.matcher.languages.GenericMatcher;
import io.github.takahino.comby.core.model.Specification;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 公式ドキュメントのサンプルを使ったエンドツーエンドテスト。
 */
class CombyIntegrationTest {

    private final GenericMatcher generic = new GenericMatcher();
    private final JavaMatcher java = new JavaMatcher();
    private final CppMatcher cpp = new CppMatcher();

    @Test
    void negateIfCondition() {
        var spec = Specification.of("if (:[cond])", "if (!(:[cond]))");
        var result = generic.rewrite("if (x > 0) { return; }", spec, null);
        assertEquals("if (!(x > 0)) { return; }", result);
    }

    @Test
    void matchFunctionCall() {
        var spec = Specification.of(":[fn](:[args])", "");
        var matches = generic.findMatches("foo(a, bar(b, c))", spec, null);
        assertFalse(matches.isEmpty());
        // foo(a, bar(b, c)) がマッチすること
        boolean foundFoo = matches.stream()
            .anyMatch(m -> m.environment().get("fn").map(cv -> cv.value().equals("foo")).orElse(false));
        assertTrue(foundFoo);
    }

    @Test
    void matchAndRewriteMultiple() {
        var source = "foo(a) bar(b) foo(c)";
        var spec = Specification.of("foo(:[x])", "baz(:[x])");
        var result = generic.rewrite(source, spec, null);
        assertEquals("baz(a) bar(b) baz(c)", result);
    }

    @Test
    void matchJavaMethodCall() {
        var source = "System.out.println(\"hello\");";
        var spec = Specification.of("System.out.println(:[arg])", "logger.info(:[arg])");
        var result = java.rewrite(source, spec, null);
        assertEquals("logger.info(\"hello\");", result);
    }

    @Test
    void matchJavaIgnoresStringContent() {
        // 文字列内の括弧はバランスに含まれない
        var source = "foo(\"test(a)\", b)";
        var spec = Specification.of("foo(:[args])", "bar(:[args])");
        var result = java.rewrite(source, spec, null);
        assertEquals("bar(\"test(a)\", b)", result);
    }

    @Test
    void matchJavaIgnoresComments() {
        var source = "foo(/* comment ( */ bar)";
        var spec = Specification.of("foo(:[x])", "baz(:[x])");
        var result = java.rewrite(source, spec, null);
        assertEquals("baz(/* comment ( */ bar)", result);
    }

    @Test
    void matchWithRule() {
        var source = "x + x";
        var spec = Specification.of(":[a] + :[b]", "twice(:[a])", "where :[a] == :[b]");
        var result = java.rewrite(source, spec, null);
        assertEquals("twice(x)", result);
    }

    @Test
    void noMatchWithRule() {
        var source = "x + y";
        var spec = Specification.of(":[a] + :[b]", "twice(:[a])", "where :[a] == :[b]");
        var result = java.rewrite(source, spec, null);
        // ルールが false なので変更なし
        assertEquals("x + y", result);
    }

    @Test
    void multipleSpecifications() {
        var source = "foo(a) bar(b)";
        var spec1 = Specification.of("foo(:[x])", "FOO(:[x])");
        var spec2 = Specification.of("bar(:[x])", "BAR(:[x])");
        // spec1を適用後にspec2を適用
        var intermediate = generic.rewrite(source, spec1, null);
        var result = generic.rewrite(intermediate, spec2, null);
        assertEquals("FOO(a) BAR(b)", result);
    }

    @Test
    void removeCppScopeResolutionFromDeclaration() {
        // C++ の ClassName::MethodName(params) → C# ライクな MethodName(params) への変換
        var spec = Specification.of(
            ":[[ret]] :[[cls]]:::[[meth]](:[params])",
            ":[ret] :[meth](:[params])"
        );

        // 基本ケース
        var result = cpp.rewrite("void CItemSelectDlg::LoadItemList(int nCategory)", spec, null);
        assertEquals("void LoadItemList(int nCategory)", result);
    }

    @Test
    void removeCppScopeResolutionCaptures() {
        // 各ホールが正しくキャプチャされていること
        var spec = Specification.of(
            ":[[ret]] :[[cls]]:::[[meth]](:[params])",
            ":[ret] :[meth](:[params])"
        );
        var matches = cpp.findMatches("void CItemSelectDlg::LoadItemList(int nCategory)", spec, null);

        assertEquals(1, matches.size());
        var env = matches.get(0).environment();
        assertEquals("void",             env.get("ret").get().value());
        assertEquals("CItemSelectDlg",   env.get("cls").get().value());
        assertEquals("LoadItemList",     env.get("meth").get().value());
        assertEquals("int nCategory",    env.get("params").get().value());
    }

    @Test
    void removeCppScopeResolutionFromFullImplementation() {
        // 関数本体を含む実装全体に適用しても宣言行のみ変換されること
        var source = """
                void CItemSelectDlg::LoadItemList(int nCategory)
                {
                    m_listItem.ResetContent();

                    switch (nCategory)
                    {
                    case 0: // Food
                        m_listItem.AddString(_T("Apple"));
                        break;
                    case 1: // Electronics
                        m_listItem.AddString(_T("TV"));
                        break;
                    }
                }
                """;
        var spec = Specification.of(
            ":[[ret]] :[[cls]]:::[[meth]](:[params])",
            ":[ret] :[meth](:[params])"
        );
        var result = cpp.rewrite(source, spec, null);

        assertTrue(result.startsWith("void LoadItemList(int nCategory)"),
            "宣言行の ClassName:: が除去されていること");
        assertFalse(result.contains("CItemSelectDlg::"),
            "CItemSelectDlg:: が残っていないこと");
    }

    @Test
    void removeCppScopeResolutionMultipleMethods() {
        // 複数のメソッド宣言が一括変換されること
        var source = """
                void CItemSelectDlg::OnInitDialog()
                {
                }
                void CItemSelectDlg::LoadItemList(int nCategory)
                {
                }
                """;
        var spec = Specification.of(
            ":[[ret]] :[[cls]]:::[[meth]](:[params])",
            ":[ret] :[meth](:[params])"
        );
        var result = cpp.rewrite(source, spec, null);

        assertFalse(result.contains("CItemSelectDlg::"), "すべての :: が除去されていること");
        assertTrue(result.contains("void OnInitDialog()"));
        assertTrue(result.contains("void LoadItemList(int nCategory)"));
    }

    @Test
    void matchCountIsCorrect() {
        var source = "a + b + c";
        var spec = Specification.of(":[x] + :[y]", "");
        var matches = generic.findMatches(source, spec, null);
        // "a + b" と "a + b + c" 等が見つかる (最左最長マッチ)
        assertFalse(matches.isEmpty());
    }
}
