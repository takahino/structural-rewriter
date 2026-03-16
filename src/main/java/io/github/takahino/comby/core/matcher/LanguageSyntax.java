package io.github.takahino.comby.core.matcher;

import java.util.List;

/**
 * 言語ごとの構文定義。
 */
public record LanguageSyntax(
    List<StringDelimiter> stringDelimiters,
    List<CommentSyntax> comments,
    List<String> openDelimiters,
    List<String> closeDelimiters
) {
    /** 文字列リテラルの開始・終了デリミタ定義 */
    public record StringDelimiter(String open, String close, boolean escapable) {}

    /** コメント構文定義 */
    public record CommentSyntax(String open, String close, boolean nested) {
        /** 行コメント */
        public static CommentSyntax line(String open) {
            return new CommentSyntax(open, "\n", false);
        }
        /** ブロックコメント */
        public static CommentSyntax block(String open, String close, boolean nested) {
            return new CommentSyntax(open, close, nested);
        }
    }

    /** デフォルト（汎用）構文定義 */
    public static LanguageSyntax generic() {
        return new LanguageSyntax(
            List.of(
                new StringDelimiter("\"", "\"", true),
                new StringDelimiter("'", "'", true)
            ),
            List.of(),
            List.of("(", "{", "["),
            List.of(")", "}", "]")
        );
    }
}
