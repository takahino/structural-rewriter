package io.github.takahino.comby.core.template;

import io.github.takahino.comby.core.model.HoleDimension;
import io.github.takahino.comby.core.model.HoleSort;
import io.github.takahino.comby.core.model.TemplateNode;

import java.util.ArrayList;
import java.util.List;

/**
 * テンプレート文字列を TemplateNode のリストに変換する再帰降下パーサ。
 *
 * 対応構文:
 *   :[var]        EVERYTHING ホール
 *   :[[var]]      ALPHANUM ホール
 *   :[var~regex]  REGEX ホール
 *   :[var:e]      ESCAPABLE_STRING 次元のホール
 *   :[var:r]      RAW_STRING 次元のホール
 *   :[var:c]      COMMENT 次元のホール
 *   :[var.]       NON_SPACE ホール
 *   :[var\n]      LINE ホール
 *   ...           省略記法（:[_] の別名扱い）
 */
public class TemplateParser {

    public static List<TemplateNode> parse(String template) {
        return new TemplateParser(template).parseTemplate();
    }

    private final String src;
    private int pos;

    private TemplateParser(String src) {
        this.src = src;
        this.pos = 0;
    }

    private List<TemplateNode> parseTemplate() {
        var nodes = new ArrayList<TemplateNode>();
        var constant = new StringBuilder();

        while (pos < src.length()) {
            if (peek() == ':' && pos + 1 < src.length()) {
                char next = src.charAt(pos + 1);
                if (next == '[' || next == ':') {
                    // flush constant
                    if (!constant.isEmpty()) {
                        nodes.add(new TemplateNode.Constant(constant.toString()));
                        constant.setLength(0);
                    }
                    TemplateNode hole = tryParseHole();
                    if (hole != null) {
                        nodes.add(hole);
                        continue;
                    }
                }
            }
            // Check for "..." ellipsis
            if (pos + 2 < src.length() && src.charAt(pos) == '.' && src.charAt(pos+1) == '.' && src.charAt(pos+2) == '.') {
                if (!constant.isEmpty()) {
                    nodes.add(new TemplateNode.Constant(constant.toString()));
                    constant.setLength(0);
                }
                nodes.add(new TemplateNode.Hole("_", HoleSort.EVERYTHING));
                pos += 3;
                continue;
            }
            constant.append(src.charAt(pos++));
        }

        if (!constant.isEmpty()) {
            nodes.add(new TemplateNode.Constant(constant.toString()));
        }
        return nodes;
    }

    private TemplateNode tryParseHole() {
        int saved = pos;

        if (!consume(':')) { pos = saved; return null; }

        // :[[var]] - ALPHANUM
        if (pos < src.length() && src.charAt(pos) == '[' &&
            pos + 1 < src.length() && src.charAt(pos + 1) == '[') {
            pos += 2; // skip [[
            String name = readUntil(']');
            if (name == null || !consume(']') || !consume(']')) { pos = saved; return null; }
            return new TemplateNode.Hole(name, HoleSort.ALPHANUM);
        }

        // :[...] variants
        if (!consume('[')) { pos = saved; return null; }

        // :[ x] - BLANK/SPACE ホール（先頭がスペース）
        if (pos < src.length() && src.charAt(pos) == ' ') {
            pos++; // skip leading space
            String blankName = readHoleName();
            if (!consume(']')) { pos = saved; return null; }
            return new TemplateNode.Hole(blankName.isEmpty() ? "_" : blankName, HoleSort.BLANK);
        }

        // Read name (allow empty for anonymous holes)
        String name = readHoleName();

        if (pos >= src.length()) { pos = saved; return null; }

        char c = src.charAt(pos);

        // :[var~regex]  ネストした [] を考慮して最終 ] を終端とする
        if (c == '~') {
            pos++;
            int brackets = 0;
            int regexStart = pos;
            while (pos < src.length()) {
                char ch = src.charAt(pos);
                if (ch == '[') { brackets++; pos++; }
                else if (ch == ']') {
                    if (brackets == 0) break;
                    brackets--;
                    pos++;
                } else {
                    pos++;
                }
            }
            if (pos >= src.length()) { pos = saved; return null; }
            String regex = src.substring(regexStart, pos);
            pos++; // consume ]
            return new TemplateNode.Hole(name.isEmpty() ? "_" : name, HoleSort.REGEX, HoleDimension.CODE, regex);
        }

        // :[var:e] / :[var:r] / :[var:c]
        if (c == ':') {
            pos++;
            if (pos >= src.length()) { pos = saved; return null; }
            char dim = src.charAt(pos++);
            if (!consume(']')) { pos = saved; return null; }
            HoleDimension dimension = switch (dim) {
                case 'e' -> HoleDimension.ESCAPABLE_STRING;
                case 'r' -> HoleDimension.RAW_STRING;
                case 'c' -> HoleDimension.COMMENT;
                default  -> { pos = saved; yield null; }
            };
            if (dimension == null) return null;
            return new TemplateNode.Hole(name.isEmpty() ? "_" : name, HoleSort.EVERYTHING, dimension, null);
        }

        // :[var.]  - NON_SPACE (直後が ']' の場合)
        // :[var.ATTR] - 属性アクセス (リライトテンプレート用)
        if (c == '.') {
            pos++;
            if (pos < src.length() && src.charAt(pos) == ']') {
                pos++;
                return new TemplateNode.Hole(name.isEmpty() ? "_" : name, HoleSort.NON_SPACE);
            }
            // 属性名を読む
            var attrSb = new StringBuilder();
            while (pos < src.length() && src.charAt(pos) != ']') {
                attrSb.append(src.charAt(pos++));
            }
            if (pos >= src.length()) { pos = saved; return null; }
            pos++; // consume ']'
            String fullName = (name.isEmpty() ? "_" : name) + "." + attrSb;
            return new TemplateNode.Hole(fullName, HoleSort.EVERYTHING);
        }

        // :[var\n] - LINE
        if (c == '\\' && pos + 1 < src.length() && src.charAt(pos + 1) == 'n') {
            pos += 2;
            if (!consume(']')) { pos = saved; return null; }
            return new TemplateNode.Hole(name.isEmpty() ? "_" : name, HoleSort.LINE);
        }

        // :[var] - EVERYTHING
        if (c == ']') {
            pos++;
            return new TemplateNode.Hole(name.isEmpty() ? "_" : name, HoleSort.EVERYTHING);
        }

        pos = saved;
        return null;
    }

    private String readHoleName() {
        var sb = new StringBuilder();
        while (pos < src.length()) {
            char c = src.charAt(pos);
            if (Character.isLetterOrDigit(c) || c == '_') {
                sb.append(c);
                pos++;
            } else {
                break;
            }
        }
        return sb.toString();
    }

    private String readUntil(char stopChar) {
        var sb = new StringBuilder();
        while (pos < src.length() && src.charAt(pos) != stopChar) {
            sb.append(src.charAt(pos++));
        }
        if (pos >= src.length()) return null;
        return sb.toString();
    }

    private boolean consume(char expected) {
        if (pos < src.length() && src.charAt(pos) == expected) {
            pos++;
            return true;
        }
        return false;
    }

    private char peek() {
        return src.charAt(pos);
    }
}
