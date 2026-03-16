package io.github.takahino.comby.output;

import com.github.difflib.DiffUtils;
import com.github.difflib.UnifiedDiffUtils;
import io.github.takahino.comby.pipeline.FileProcessor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

/**
 * インタラクティブな review モードのフォーマッター。
 * 各変更についてユーザーに accept/reject を求める。
 */
public class InteractiveFormatter implements OutputFormatter {

    @Override
    public void format(List<FileProcessor.FileResult> results) {
        var reader = new BufferedReader(new InputStreamReader(System.in));

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

            System.out.println("--- " + result.filePath() + " ---");
            diff.forEach(System.out::println);
            System.out.print("Accept? [y/N] ");
            System.out.flush();

            try {
                String answer = reader.readLine();
                if ("y".equalsIgnoreCase(answer) || "yes".equalsIgnoreCase(answer)) {
                    Files.writeString(result.filePath(), result.rewrittenSource());
                    System.out.println("Accepted.");
                } else {
                    System.out.println("Skipped.");
                }
            } catch (IOException e) {
                System.err.println("Error reading input: " + e.getMessage());
            }
        }
    }
}
