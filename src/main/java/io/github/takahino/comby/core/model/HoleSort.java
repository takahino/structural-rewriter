package io.github.takahino.comby.core.model;

public enum HoleSort {
    EVERYTHING,    // :[var]  - balancedマッチ
    EXPRESSION,    // not used directly, alias for EVERYTHING in some contexts
    ALPHANUM,      // :[[var]] - 英数字のみ
    NON_SPACE,     // :[var.] - 空白以外
    LINE,          // :[var\n] - 改行まで
    BLANK,         // :[~\s*] - 空白
    REGEX          // :[var~regex] - 正規表現
}
