package io.github.takahino.comby.output;

import com.github.difflib.DiffUtils;
import com.github.difflib.UnifiedDiffUtils;
import io.github.takahino.comby.pipeline.FileProcessor;

import java.util.Arrays;
import java.util.List;

/**
 * Unified diff 形式の出力フォーマッター。
 */
public class DiffFormatter implements OutputFormatter {

    @Override
    public void format(List<FileProcessor.FileResult> results) {
        for (var result : results) {
            if (!result.hasChanges()) continue;

            var originalLines = Arrays.asList(result.originalSource().split("\n", -1));
            var rewrittenLines = Arrays.asList(result.rewrittenSource().split("\n", -1));

            var patch = DiffUtils.diff(originalLines, rewrittenLines);
            var diff = UnifiedDiffUtils.generateUnifiedDiff(
                "a/" + result.filePath(),
                "b/" + result.filePath(),
                originalLines, patch, 3
            );

            diff.forEach(System.out::println);
        }
    }
}
