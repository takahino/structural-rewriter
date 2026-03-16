package io.github.takahino.comby.output;

import io.github.takahino.comby.pipeline.FileProcessor;

import java.util.List;

public interface OutputFormatter {
    void format(List<FileProcessor.FileResult> results);
}
