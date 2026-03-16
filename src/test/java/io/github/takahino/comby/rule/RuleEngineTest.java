package io.github.takahino.comby.rule;

import io.github.takahino.comby.core.matcher.LanguageSyntax;
import io.github.takahino.comby.core.matcher.MatchEngine;
import io.github.takahino.comby.core.model.CapturedValue;
import io.github.takahino.comby.core.model.Location;
import io.github.takahino.comby.core.model.MatchEnvironment;
import io.github.takahino.comby.core.model.Range;
import io.github.takahino.comby.core.rule.RuleEngine;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RuleEngineTest {

    private final MatchEngine matchEngine = new MatchEngine(LanguageSyntax.generic());
    private final RuleEngine ruleEngine = new RuleEngine(matchEngine);

    private MatchEnvironment envWith(String name, String value) {
        var range = Range.of(Location.zero(), Location.zero());
        return MatchEnvironment.empty().bind(name, CapturedValue.of(name, value, range));
    }

    @Test
    void nullRuleIsTrue() {
        assertTrue(ruleEngine.evaluate(null, MatchEnvironment.empty(), ""));
    }

    @Test
    void blankRuleIsTrue() {
        assertTrue(ruleEngine.evaluate("  ", MatchEnvironment.empty(), ""));
    }

    @Test
    void equalRule() {
        var env = envWith("x", "foo");
        assertTrue(ruleEngine.evaluate("where :[x] == \"foo\"", env, ""));
        assertFalse(ruleEngine.evaluate("where :[x] == \"bar\"", env, ""));
    }

    @Test
    void notEqualRule() {
        var env = envWith("x", "foo");
        assertTrue(ruleEngine.evaluate("where :[x] != \"bar\"", env, ""));
        assertFalse(ruleEngine.evaluate("where :[x] != \"foo\"", env, ""));
    }

    @Test
    void andRule() {
        var range = Range.of(Location.zero(), Location.zero());
        var env = MatchEnvironment.empty()
            .bind("x", CapturedValue.of("x", "foo", range))
            .bind("y", CapturedValue.of("y", "bar", range));
        assertTrue(ruleEngine.evaluate("where :[x] == \"foo\", :[y] == \"bar\"", env, ""));
        assertFalse(ruleEngine.evaluate("where :[x] == \"foo\", :[y] == \"baz\"", env, ""));
    }

    @Test
    void variableEquality() {
        var range = Range.of(Location.zero(), Location.zero());
        var envEqual = MatchEnvironment.empty()
            .bind("x", CapturedValue.of("x", "hello", range))
            .bind("y", CapturedValue.of("y", "hello", range));
        var envNotEqual = MatchEnvironment.empty()
            .bind("x", CapturedValue.of("x", "hello", range))
            .bind("y", CapturedValue.of("y", "world", range));

        assertTrue(ruleEngine.evaluate("where :[x] == :[y]", envEqual, ""));
        assertFalse(ruleEngine.evaluate("where :[x] == :[y]", envNotEqual, ""));
    }

    // ---- test_match_rule.ml 相当 ----

    @Test
    void literalStringEquality() {
        // リテラル同士の比較は常に固定値
        assertTrue(ruleEngine.evaluate("where \"x\" == \"x\"", MatchEnvironment.empty(), ""));
        assertFalse(ruleEngine.evaluate("where \"x\" == \"y\"", MatchEnvironment.empty(), ""));
    }

    @Test
    void literalStringInequality() {
        assertTrue(ruleEngine.evaluate("where \"x\" != \"y\"", MatchEnvironment.empty(), ""));
        assertFalse(ruleEngine.evaluate("where \"x\" != \"x\"", MatchEnvironment.empty(), ""));
    }

    @Test
    void unboundVariableEquality() {
        // 未束縛の変数は何にもマッチしない → false
        assertFalse(ruleEngine.evaluate("where :[x] == \"y\"", MatchEnvironment.empty(), ""));
    }

    @Test
    void compoundConditionAllTrue() {
        // :[1]==:[3] かつ :[1]!=:[2]  (1→x, 2→y, 3→x)
        var range = Range.of(Location.zero(), Location.zero());
        var env = MatchEnvironment.empty()
            .bind("1", CapturedValue.of("1", "x", range))
            .bind("2", CapturedValue.of("2", "y", range))
            .bind("3", CapturedValue.of("3", "x", range));
        assertTrue(ruleEngine.evaluate("where :[1] == :[3], :[1] != :[2]", env, ""));
    }

    @Test
    void compoundConditionOneFalse() {
        // 条件の1つが false なら全体が false
        var range = Range.of(Location.zero(), Location.zero());
        var env = MatchEnvironment.empty()
            .bind("x", CapturedValue.of("x", "foo", range))
            .bind("y", CapturedValue.of("y", "foo", range));
        // :[x] == "foo" は true, :[x] != :[y] は false (両方 "foo")
        assertFalse(ruleEngine.evaluate("where :[x] == \"foo\", :[x] != :[y]", env, ""));
    }

    @Test
    void ruleFilterMatchesViaIntegration() {
        // where ルールでマッチをフィルタリングする統合テスト
        // :[a] + :[b]: where :[a] == :[b] → "x + x" はマッチ, "x + y" はマッチしない
        var engine = new io.github.takahino.comby.core.matcher.MatchEngine(
            io.github.takahino.comby.core.matcher.LanguageSyntax.generic());
        var spec = io.github.takahino.comby.core.model.Specification.of(
            ":[a] + :[b]", "result", "where :[a] == :[b]");

        var matcherImpl = new io.github.takahino.comby.core.matcher.languages.GenericMatcher();
        var matchYes = matcherImpl.findMatches("x + x", spec, null);
        assertEquals(1, matchYes.size());

        var matchNo = matcherImpl.findMatches("x + y", spec, null);
        assertEquals(0, matchNo.size());
    }
}
