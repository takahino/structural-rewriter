package io.github.takahino.comby.output;

import io.github.takahino.comby.core.model.Match;
import io.github.takahino.comby.pipeline.FileProcessor;

import java.util.List;

/**
 * テキスト形式の出力フォーマッター（デフォルト）。
 */
public class TextFormatter implements OutputFormatter {

    private final boolean matchOnly;
    private final boolean alwaysPrint;

    public TextFormatter(boolean matchOnly) {
        this(matchOnly, false);
    }

    public TextFormatter(boolean matchOnly, boolean alwaysPrint) {
        this.matchOnly = matchOnly;
        this.alwaysPrint = alwaysPrint;
    }

    @Override
    public void format(List<FileProcessor.FileResult> results) {
        for (var result : results) {
            if (matchOnly) {
                formatMatches(result);
            } else {
                formatRewrite(result);
            }
        }
    }

    private void formatMatches(FileProcessor.FileResult result) {
        for (Match m : result.matches()) {
            System.out.printf("-- Match at %s:%d:%d--%d:%d --%n",
                result.filePath(),
                m.range().start().line(),
                m.range().start().column(),
                m.range().end().line(),
                m.range().end().column()
            );
            System.out.println(m.matchedText());
            m.environment().all().forEach((name, cv) ->
                System.out.printf("  :[%s] = %s%n", name, cv.value())
            );
        }
    }

    private void formatRewrite(FileProcessor.FileResult result) {
        if (alwaysPrint || result.hasChanges()) {
            System.out.print(result.rewrittenSource());
        }
    }
}
