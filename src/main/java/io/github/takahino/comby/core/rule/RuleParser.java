package io.github.takahino.comby.core.rule;

import java.util.ArrayList;
import java.util.List;

/**
 * ルール文字列をパースして RuleNode を返すパーサ。
 *
 * 対応構文:
 *   where :[x] == "foo"
 *   where :[x] != :[y]
 *   where :[x] == "foo", :[y] != "bar"   (AND)
 */
public class RuleParser {

    public static RuleNode parse(String rule) {
        if (rule == null || rule.isBlank()) return new RuleNode.True_();
        var trimmed = rule.trim();
        if (trimmed.startsWith("where ")) {
            trimmed = trimmed.substring("where ".length()).trim();
        }
        return new RuleParser(trimmed).parseAnd();
    }

    private final String src;
    private int pos;

    private RuleParser(String src) {
        this.src = src;
        this.pos = 0;
    }

    private RuleNode parseAnd() {
        var conditions = new ArrayList<RuleNode>();
        conditions.add(parseCondition());

        while (pos < src.length()) {
            skipWhitespace();
            if (pos < src.length() && src.charAt(pos) == ',') {
                pos++;
                skipWhitespace();
                conditions.add(parseCondition());
            } else {
                break;
            }
        }

        if (conditions.size() == 1) return conditions.get(0);
        return new RuleNode.And(conditions);
    }

    private RuleNode parseCondition() {
        skipWhitespace();

        // :[x] == or :[x] !=
        // "literal" == ... または "literal" != ...
        if (pos < src.length() && src.charAt(pos) == '"') {
            int saved = pos;
            String left = parseStringLiteral();
            skipWhitespace();
            if (pos + 1 < src.length() && src.charAt(pos) == '=' && src.charAt(pos+1) == '=') {
                pos += 2;
                skipWhitespace();
                var rightResult = parseValue();
                return new RuleNode.Equal(left, rightResult.value(), false, rightResult.isVar());
            }
            if (pos + 1 < src.length() && src.charAt(pos) == '!' && src.charAt(pos+1) == '=') {
                pos += 2;
                skipWhitespace();
                var rightResult = parseValue();
                return new RuleNode.NotEqual(left, rightResult.value(), false, rightResult.isVar());
            }
            pos = saved;
        }

        if (pos < src.length() && src.charAt(pos) == ':') {
            int saved = pos;
            String left = tryParseHoleRef();
            if (left != null) {
                skipWhitespace();
                if (pos + 1 < src.length() && src.charAt(pos) == '=' && src.charAt(pos+1) == '=') {
                    pos += 2;
                    skipWhitespace();
                    var rightResult = parseValue();
                    return new RuleNode.Equal(left, rightResult.value(), true, rightResult.isVar());
                }
                if (pos + 1 < src.length() && src.charAt(pos) == '!' && src.charAt(pos+1) == '=') {
                    pos += 2;
                    skipWhitespace();
                    var rightResult = parseValue();
                    return new RuleNode.NotEqual(left, rightResult.value(), true, rightResult.isVar());
                }
                pos = saved;
            }
        }

        // rewrite :[x] { "template" }
        if (tryConsume("rewrite")) {
            skipWhitespace();
            String varName = tryParseHoleRef();
            skipWhitespace();
            if (varName != null && pos < src.length() && src.charAt(pos) == '{') {
                pos++;
                skipWhitespace();
                String template = parseStringLiteral();
                skipWhitespace();
                if (pos < src.length() && src.charAt(pos) == '}') pos++;
                return new RuleNode.Rewrite_(varName, template);
            }
        }

        // true / false
        if (tryConsume("true")) return new RuleNode.True_();
        if (tryConsume("false")) return new RuleNode.False_();

        return new RuleNode.True_();
    }

    private record ValueResult(String value, boolean isVar) {}

    private ValueResult parseValue() {
        skipWhitespace();
        if (pos < src.length() && src.charAt(pos) == '"') {
            return new ValueResult(parseStringLiteral(), false);
        }
        if (pos < src.length() && src.charAt(pos) == ':') {
            String ref = tryParseHoleRef();
            if (ref != null) return new ValueResult(ref, true);
        }
        // unquoted word
        var sb = new StringBuilder();
        while (pos < src.length() && !Character.isWhitespace(src.charAt(pos)) && src.charAt(pos) != ',') {
            sb.append(src.charAt(pos++));
        }
        return new ValueResult(sb.toString(), false);
    }

    private String parseStringLiteral() {
        if (pos >= src.length() || src.charAt(pos) != '"') return "";
        pos++; // skip opening "
        var sb = new StringBuilder();
        while (pos < src.length() && src.charAt(pos) != '"') {
            if (src.charAt(pos) == '\\' && pos + 1 < src.length()) {
                pos++;
                sb.append(src.charAt(pos++));
            } else {
                sb.append(src.charAt(pos++));
            }
        }
        if (pos < src.length()) pos++; // skip closing "
        return sb.toString();
    }

    private String tryParseHoleRef() {
        if (pos >= src.length() || src.charAt(pos) != ':') return null;
        if (pos + 1 >= src.length() || src.charAt(pos + 1) != '[') return null;
        int saved = pos;
        pos += 2;
        // :[[var]]
        if (pos < src.length() && src.charAt(pos) == '[') {
            pos++;
            var sb = new StringBuilder();
            while (pos < src.length() && src.charAt(pos) != ']') sb.append(src.charAt(pos++));
            if (pos < src.length()) pos++; // ]
            if (pos < src.length() && src.charAt(pos) == ']') pos++; // ]
            return sb.toString();
        }
        var sb = new StringBuilder();
        while (pos < src.length() && src.charAt(pos) != ']') sb.append(src.charAt(pos++));
        if (pos < src.length()) pos++; // ]
        return sb.toString();
    }

    private boolean tryConsume(String word) {
        if (src.startsWith(word, pos)) {
            pos += word.length();
            return true;
        }
        return false;
    }

    private void skipWhitespace() {
        while (pos < src.length() && Character.isWhitespace(src.charAt(pos))) pos++;
    }
}
