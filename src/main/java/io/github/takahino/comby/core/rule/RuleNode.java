package io.github.takahino.comby.core.rule;

import java.util.List;

/**
 * ルール式の AST ノード（sealed ADT）。
 */
public sealed interface RuleNode permits
    RuleNode.True_,
    RuleNode.False_,
    RuleNode.Equal,
    RuleNode.NotEqual,
    RuleNode.Match_,
    RuleNode.Rewrite_,
    RuleNode.And {

    record True_() implements RuleNode {}
    record False_() implements RuleNode {}

    /** :[x] == "literal" または :[x] == :[y] */
    record Equal(String left, String right, boolean leftIsVar, boolean rightIsVar) implements RuleNode {}

    /** :[x] != "literal" または :[x] != :[y] */
    record NotEqual(String left, String right, boolean leftIsVar, boolean rightIsVar) implements RuleNode {}

    /** match :[x] { | "pattern" -> ... } */
    record Match_(String variable, List<MatchArm> arms) implements RuleNode {
        public record MatchArm(String pattern, RuleNode body) {}
    }

    /** rewrite :[x] { "template" } */
    record Rewrite_(String variable, String template) implements RuleNode {}

    /** A, B (AND結合) */
    record And(List<RuleNode> conditions) implements RuleNode {}
}
