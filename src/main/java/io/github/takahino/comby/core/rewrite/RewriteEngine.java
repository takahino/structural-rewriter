package io.github.takahino.comby.core.rewrite;

import io.github.takahino.comby.core.model.*;
import io.github.takahino.comby.core.template.TemplateParser;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * マッチ結果からリライト（置換）を行うエンジン。
 */
public class RewriteEngine {

    /**
     * ソーステキストに対して全マッチを置換し、結果文字列を返す。
     * マッチは重複しないことを前提とする（オフセット昇順）。
     */
    public static String rewrite(String source, List<Match> matches, String rewriteTemplate) {
        if (matches.isEmpty()) return source;

        var sb = new StringBuilder();
        int cursor = 0;

        for (var match : matches) {
            int start = match.range().start().offset();
            int end = match.range().end().offset();

            // マッチ前のテキストをそのまま追加
            sb.append(source, cursor, start);
            // リライトテンプレートを展開
            sb.append(substitute(rewriteTemplate, match.environment(), match));
            cursor = end;
        }

        // 残りのテキストを追加
        sb.append(source, cursor, source.length());
        return sb.toString();
    }

    /**
     * リライトテンプレートを展開する。
     * :[var] → キャプチャ値
     * :[var.UPPERCASE] / :[var.lowercase] / :[var.length] 等の属性対応
     * :[id()] → ランダムID
     */
    public static String substitute(String template, MatchEnvironment env, Match match) {
        var nodes = TemplateParser.parse(template);
        var sb = new StringBuilder();

        for (var node : nodes) {
            switch (node) {
                case TemplateNode.Constant c -> sb.append(c.value());
                case TemplateNode.Hole h -> {
                    String name = h.name();

                    // :[id()] - ランダムID生成
                    if (name.equals("id()") || name.equals("id")) {
                        sb.append(generateId());
                        continue;
                    }

                    // 属性アクセス: name.ATTRIBUTE
                    String varName = name;
                    String attribute = null;
                    int dotIdx = name.indexOf('.');
                    if (dotIdx >= 0) {
                        varName = name.substring(0, dotIdx);
                        attribute = name.substring(dotIdx + 1);
                    }

                    var captured = env.get(varName);
                    if (captured.isPresent()) {
                        String val = captured.get().value();
                        sb.append(applyAttribute(val, attribute, match));
                    } else {
                        // キャプチャなし → そのまま（元のホール表記を残す）
                        sb.append(":[").append(name).append("]");
                    }
                }
            }
        }

        return sb.toString();
    }

    private static String applyAttribute(String value, String attribute, Match match) {
        if (attribute == null) return value;
        return switch (attribute) {
            case "UPPERCASE"        -> value.toUpperCase();
            case "lowercase"        -> value.toLowerCase();
            case "Capitalize"       -> capitalize(value);
            case "UpperCamelCase"   -> toUpperCamelCase(value);
            case "lowerCamelCase"   -> toLowerCamelCase(value);
            case "UPPER_SNAKE_CASE" -> toUpperSnakeCase(value);
            case "length"           -> String.valueOf(value.length());
            case "lines"            -> String.valueOf(countLines(value));
            case "line"             -> match != null ? String.valueOf(match.range().start().line()) : value;
            case "file"             -> match != null && match.filePath() != null ? match.filePath() : value;
            default          -> value;
        };
    }

    private static String capitalize(String s) {
        if (s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }

    private static String toUpperCamelCase(String s) {
        var sb = new StringBuilder();
        boolean cap = true;
        for (char c : s.toCharArray()) {
            if (c == '_' || c == '-') { cap = true; }
            else if (cap) { sb.append(Character.toUpperCase(c)); cap = false; }
            else { sb.append(c); }
        }
        return sb.toString();
    }

    private static String toLowerCamelCase(String s) {
        String upper = toUpperCamelCase(s);
        if (upper.isEmpty()) return upper;
        return Character.toLowerCase(upper.charAt(0)) + upper.substring(1);
    }

    private static String toUpperSnakeCase(String s) {
        // CamelCase → UPPER_SNAKE_CASE
        var sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isUpperCase(c) && i > 0) sb.append('_');
            sb.append(Character.toUpperCase(c));
        }
        return sb.toString();
    }

    private static int countLines(String s) {
        // 改行文字の数をカウント（comby の .lines 属性と同じ挙動）
        int count = 0;
        for (char c : s.toCharArray()) {
            if (c == '\n') count++;
        }
        return count;
    }

    private static String generateId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    /**
     * マッチに対してリライトテンプレートを適用した結果を返す（単体用）。
     */
    public static String substituteMatch(Match match, String rewriteTemplate) {
        return substitute(rewriteTemplate, match.environment(), match);
    }
}
