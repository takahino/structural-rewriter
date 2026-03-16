package io.github.takahino.comby;

import io.github.takahino.comby.core.matcher.languages.LanguageRegistry;
import io.github.takahino.comby.core.model.Match;
import io.github.takahino.comby.core.model.Specification;

import java.util.List;

/**
 * comby-java のメイン API ファサード。
 *
 * <p>すべてのメソッドは static で、インスタンス生成不要。
 *
 * <p>基本的な使い方:
 * <pre>{@code
 * // ソース中の foo(...) を bar(...) に書き換え
 * String result = Comby.rewrite(source, "foo(:[args])", "bar(:[args])");
 *
 * // マッチ一覧を取得
 * List<Match> matches = Comby.matches(source, "System.out.println(:[msg])", "java");
 * for (Match m : matches) {
 *     String captured = m.environment().get("msg").get().value();
 * }
 * }</pre>
 */
public final class Comby {

    private Comby() {}

    // -------------------------------------------------------------------------
    // rewrite
    // -------------------------------------------------------------------------

    /**
     * ソース文字列に対してマッチ＆書き換えを適用し、結果を返す。
     * 言語は generic（汎用）マッチャーを使用。
     *
     * @param source          対象ソース文字列
     * @param matchTemplate   comby マッチテンプレート（例: {@code "foo(:[args])"}）
     * @param rewriteTemplate comby 書き換えテンプレート（例: {@code "bar(:[args])"}）
     * @return 書き換え後の文字列。マッチがなければ source をそのまま返す
     */
    public static String rewrite(String source, String matchTemplate, String rewriteTemplate) {
        return rewrite(source, matchTemplate, rewriteTemplate, "generic", null);
    }

    /**
     * 言語を指定してマッチ＆書き換えを適用する。
     *
     * @param source          対象ソース文字列
     * @param matchTemplate   comby マッチテンプレート
     * @param rewriteTemplate comby 書き換えテンプレート
     * @param language        言語名（例: {@code "java"}, {@code "python"}, {@code "go"}）
     * @return 書き換え後の文字列
     */
    public static String rewrite(String source, String matchTemplate, String rewriteTemplate,
                                  String language) {
        return rewrite(source, matchTemplate, rewriteTemplate, language, null);
    }

    /**
     * 言語とルールを指定してマッチ＆書き換えを適用する。
     *
     * @param source          対象ソース文字列
     * @param matchTemplate   comby マッチテンプレート
     * @param rewriteTemplate comby 書き換えテンプレート
     * @param language        言語名（例: {@code "java"}）
     * @param rule            where ルール（例: {@code "where :[x] == \"foo\""}）。不要なら null
     * @return 書き換え後の文字列
     */
    public static String rewrite(String source, String matchTemplate, String rewriteTemplate,
                                  String language, String rule) {
        var matcher = LanguageRegistry.getInstance().forName(language);
        var spec = Specification.of(matchTemplate, rewriteTemplate, rule);
        return matcher.rewrite(source, spec, null);
    }

    // -------------------------------------------------------------------------
    // matches
    // -------------------------------------------------------------------------

    /**
     * ソース文字列からすべてのマッチを取得する。
     * 言語は generic（汎用）マッチャーを使用。
     *
     * @param source        対象ソース文字列
     * @param matchTemplate comby マッチテンプレート
     * @return マッチ結果リスト（マッチなしなら空リスト）
     */
    public static List<Match> matches(String source, String matchTemplate) {
        return matches(source, matchTemplate, "generic", null);
    }

    /**
     * 言語を指定してすべてのマッチを取得する。
     *
     * @param source        対象ソース文字列
     * @param matchTemplate comby マッチテンプレート
     * @param language      言語名（例: {@code "java"}, {@code "python"}）
     * @return マッチ結果リスト
     */
    public static List<Match> matches(String source, String matchTemplate, String language) {
        return matches(source, matchTemplate, language, null);
    }

    /**
     * 言語とルールを指定してすべてのマッチを取得する。
     *
     * @param source        対象ソース文字列
     * @param matchTemplate comby マッチテンプレート
     * @param language      言語名
     * @param rule          where ルール。不要なら null
     * @return マッチ結果リスト
     */
    public static List<Match> matches(String source, String matchTemplate, String language,
                                       String rule) {
        var matcher = LanguageRegistry.getInstance().forName(language);
        var spec = Specification.of(matchTemplate, matchTemplate, rule);
        return matcher.findMatches(source, spec, null);
    }

    // -------------------------------------------------------------------------
    // ファイルパス自動検出
    // -------------------------------------------------------------------------

    /**
     * ファイルパスから言語を自動判定してマッチ＆書き換えを適用する。
     * 拡張子が対応言語のものであれば専用マッチャー、未知なら generic を使用。
     *
     * @param source          対象ソース文字列
     * @param matchTemplate   comby マッチテンプレート
     * @param rewriteTemplate comby 書き換えテンプレート
     * @param filePath        ファイルパス（例: {@code "src/Main.java"}）。拡張子で言語を判定
     * @return 書き換え後の文字列
     */
    public static String rewriteFile(String source, String matchTemplate, String rewriteTemplate,
                                      String filePath) {
        return rewriteFile(source, matchTemplate, rewriteTemplate, filePath, null);
    }

    /**
     * ファイルパスから言語を自動判定してマッチ＆書き換えを適用する（ルール付き）。
     *
     * @param source          対象ソース文字列
     * @param matchTemplate   comby マッチテンプレート
     * @param rewriteTemplate comby 書き換えテンプレート
     * @param filePath        ファイルパス
     * @param rule            where ルール。不要なら null
     * @return 書き換え後の文字列
     */
    public static String rewriteFile(String source, String matchTemplate, String rewriteTemplate,
                                      String filePath, String rule) {
        var matcher = LanguageRegistry.getInstance().forFilePath(filePath);
        var spec = Specification.of(matchTemplate, rewriteTemplate, rule);
        return matcher.rewrite(source, spec, filePath);
    }

    /**
     * ファイルパスから言語を自動判定してすべてのマッチを取得する。
     *
     * @param source        対象ソース文字列
     * @param matchTemplate comby マッチテンプレート
     * @param filePath      ファイルパス
     * @return マッチ結果リスト
     */
    public static List<Match> matchesFile(String source, String matchTemplate, String filePath) {
        var matcher = LanguageRegistry.getInstance().forFilePath(filePath);
        var spec = Specification.of(matchTemplate, matchTemplate, null);
        return matcher.findMatches(source, spec, filePath);
    }
}
