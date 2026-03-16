package io.github.takahino.comby.core.matcher;

import io.github.takahino.comby.core.model.Match;
import io.github.takahino.comby.core.model.Specification;

import java.util.List;

/**
 * 言語固有のマッチングを担うインターフェース。
 */
public interface LanguageMatcher {
    /** この Matcher が対応するファイル拡張子リスト */
    List<String> extensions();

    /** Matcher の識別名（例: "java", "generic"） */
    String name();

    /** 言語構文定義を返す */
    LanguageSyntax syntax();

    /** ソーステキストから全マッチを返す */
    List<Match> findMatches(String source, Specification spec, String filePath);

    /** ソーステキストに対して置換を行い、結果文字列を返す */
    String rewrite(String source, Specification spec, String filePath);

    /** 使用する StructuralTokenizer（null = 手書きスキャナでフォールバック） */
    default StructuralTokenizer tokenizer() { return null; }
}
