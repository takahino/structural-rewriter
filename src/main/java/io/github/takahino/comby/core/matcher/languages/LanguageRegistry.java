package io.github.takahino.comby.core.matcher.languages;

import io.github.takahino.comby.core.matcher.LanguageMatcher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 拡張子・言語名 → LanguageMatcher の解決レジストリ。
 */
public class LanguageRegistry {

    private static final LanguageRegistry INSTANCE = new LanguageRegistry();

    private final List<LanguageMatcher> matchers = new ArrayList<>();
    private final Map<String, LanguageMatcher> byExtension = new HashMap<>();
    private final Map<String, LanguageMatcher> byName = new HashMap<>();
    private final GenericMatcher genericMatcher = new GenericMatcher();

    private LanguageRegistry() {
        register(new JavaMatcher());
        register(new PythonMatcher());
        register(new JavaScriptMatcher());
        register(new TypeScriptMatcher());
        register(new CMatcher());
        register(new CppMatcher());
        register(new GoMatcher());
        register(new RustMatcher());
        register(new RubyMatcher());
        register(new PhpMatcher());
        register(new ScalaMatcher());
        register(new KotlinMatcher());
        register(new SwiftMatcher());
        register(new CSharpMatcher());
        register(new SqlMatcher());
        register(new BashMatcher());
        register(new OcamlMatcher());
        register(genericMatcher);
    }

    private void register(LanguageMatcher matcher) {
        matchers.add(matcher);
        byName.put(matcher.name(), matcher);
        for (String ext : matcher.extensions()) {
            byExtension.put(ext, matcher);
        }
    }

    public static LanguageRegistry getInstance() {
        return INSTANCE;
    }

    /** ファイル拡張子（例: ".java"）で Matcher を解決。不明なら Generic を返す */
    public LanguageMatcher forExtension(String extension) {
        return byExtension.getOrDefault(extension, genericMatcher);
    }

    /** 言語名（例: "java"）で Matcher を解決。不明なら Generic を返す */
    public LanguageMatcher forName(String name) {
        return byName.getOrDefault(name, genericMatcher);
    }

    /** ファイルパスから Matcher を解決 */
    public LanguageMatcher forFilePath(String filePath) {
        if (filePath == null) return genericMatcher;
        int dot = filePath.lastIndexOf('.');
        if (dot < 0) return genericMatcher;
        return forExtension(filePath.substring(dot));
    }

    public GenericMatcher generic() { return genericMatcher; }

    public List<LanguageMatcher> all() { return List.copyOf(matchers); }
}
