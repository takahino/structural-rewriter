package io.github.takahino.comby.core.rule;

import io.github.takahino.comby.core.matcher.MatchEngine;
import io.github.takahino.comby.core.matcher.StringCursor;
import io.github.takahino.comby.core.model.MatchEnvironment;
import io.github.takahino.comby.core.template.TemplateParser;

/**
 * ルール式を評価してマッチを承認/却下するエンジン。
 * Java 21 パターンマッチング for switch を使用。
 */
public class RuleEngine {

    private final MatchEngine matchEngine;

    public RuleEngine(MatchEngine matchEngine) {
        this.matchEngine = matchEngine;
    }

    /**
     * ルール文字列を評価する。
     * @param rule ルール文字列（"where ..."）
     * @param env マッチ環境
     * @param source 元のソーステキスト（rewrite ルール用）
     * @return ルールが true なら true
     */
    public boolean evaluate(String rule, MatchEnvironment env, String source) {
        if (rule == null || rule.isBlank()) return true;
        var node = RuleParser.parse(rule);
        return evalNode(node, env, source);
    }

    private boolean evalNode(RuleNode node, MatchEnvironment env, String source) {
        return switch (node) {
            case RuleNode.True_  ignored -> true;
            case RuleNode.False_ ignored -> false;

            case RuleNode.Equal(var left, var right, var leftIsVar, var rightIsVar) -> {
                String lv = leftIsVar  ? resolve(left, env)  : left;
                String rv = rightIsVar ? resolve(right, env) : right;
                yield lv != null && lv.equals(rv);
            }

            case RuleNode.NotEqual(var left, var right, var leftIsVar, var rightIsVar) -> {
                String lv = leftIsVar  ? resolve(left, env)  : left;
                String rv = rightIsVar ? resolve(right, env) : right;
                yield lv == null || !lv.equals(rv);
            }

            case RuleNode.And(var conditions) ->
                conditions.stream().allMatch(c -> evalNode(c, env, source));

            case RuleNode.Rewrite_(var variable, var template) -> {
                // rewrite ルール: 変数の値に対してテンプレートマッチを実行し環境を更新
                var val = resolve(variable, env);
                yield val != null; // 変数が存在すれば true（副作用は別途）
            }

            case RuleNode.Match_(var variable, var arms) -> {
                var val = resolve(variable, env);
                if (val == null) yield false;
                for (var arm : arms) {
                    var nodes = TemplateParser.parse(arm.pattern());
                    var cursor = new StringCursor(val);
                    var result = matchEngine.matchNodes(cursor, nodes, env);
                    if (result.isPresent() && cursor.pos() == val.length()) {
                        yield evalNode(arm.body(), result.get(), source);
                    }
                }
                yield false;
            }
        };
    }

    private String resolve(String varName, MatchEnvironment env) {
        return env.get(varName).map(cv -> cv.value()).orElse(null);
    }
}
