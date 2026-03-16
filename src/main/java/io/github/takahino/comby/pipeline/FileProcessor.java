package io.github.takahino.comby.pipeline;

import io.github.takahino.comby.core.matcher.LanguageMatcher;
import io.github.takahino.comby.core.matcher.languages.LanguageRegistry;
import io.github.takahino.comby.core.model.Match;
import io.github.takahino.comby.core.model.Specification;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 1ファイルに対してマッチ/リライト処理を実行する。
 */
public class FileProcessor {

    public record FileResult(
        Path filePath,
        String originalSource,
        String rewrittenSource,
        List<Match> matches
    ) {
        public boolean hasChanges() {
            return !originalSource.equals(rewrittenSource);
        }
    }

    private final PipelineConfig config;
    private final LanguageRegistry registry = LanguageRegistry.getInstance();

    public FileProcessor(PipelineConfig config) {
        this.config = config;
    }

    public FileResult process(Path filePath) throws IOException {
        String source = Files.readString(filePath);
        LanguageMatcher matcher = resolveMatcher(filePath.toString());

        var allMatches = new ArrayList<Match>();
        String rewritten = source;

        for (Specification spec : config.specifications()) {
            var matches = matcher.findMatches(rewritten, spec, filePath.toString());
            allMatches.addAll(matches);
            if (!config.matchOnly() && spec.rewriteTemplate() != null) {
                rewritten = matcher.rewrite(rewritten, spec, filePath.toString());
            }
        }

        return new FileResult(filePath, source, rewritten, allMatches);
    }

    public FileResult processSource(String source, String virtualPath) {
        LanguageMatcher matcher = resolveMatcher(virtualPath);

        var allMatches = new ArrayList<Match>();
        String rewritten = source;

        for (Specification spec : config.specifications()) {
            var matches = matcher.findMatches(rewritten, spec, virtualPath);
            allMatches.addAll(matches);
            if (!config.matchOnly() && spec.rewriteTemplate() != null) {
                rewritten = matcher.rewrite(rewritten, spec, virtualPath);
            }
        }

        // Windows で "<stdin>" はパスとして無効なため安全なパスを使用
        Path safePath;
        try {
            safePath = Path.of(virtualPath);
        } catch (Exception e) {
            safePath = Path.of("stdin");
        }
        return new FileResult(safePath, source, rewritten, allMatches);
    }

    private LanguageMatcher resolveMatcher(String filePath) {
        if (config.matcher() != null && !config.matcher().equals("generic")) {
            return registry.forName(config.matcher());
        }
        return registry.forFilePath(filePath);
    }
}
