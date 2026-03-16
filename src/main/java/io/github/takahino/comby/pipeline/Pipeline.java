package io.github.takahino.comby.pipeline;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Stream;

/**
 * Virtual Threads を使ったファイル並列処理パイプライン。
 */
public class Pipeline {

    private final PipelineConfig config;

    public Pipeline(PipelineConfig config) {
        this.config = config;
    }

    /**
     * ディレクトリ配下の全ファイルを並列処理し、結果を返す。
     */
    public List<FileProcessor.FileResult> run() throws IOException, InterruptedException {
        var files = collectFiles(Path.of(config.directory()));
        return processFiles(files);
    }

    /**
     * ファイルリストを並列処理する。
     */
    public List<FileProcessor.FileResult> processFiles(List<Path> files) throws InterruptedException {
        var processor = new FileProcessor(config);
        var results = new ArrayList<FileProcessor.FileResult>();

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var futures = new ArrayList<Future<FileProcessor.FileResult>>();

            for (var file : files) {
                futures.add(executor.submit(() -> processor.process(file)));
            }

            for (var future : futures) {
                try {
                    results.add(future.get());
                } catch (Exception e) {
                    System.err.println("Error processing file: " + e.getMessage());
                }
            }
        }

        return results;
    }

    private List<Path> collectFiles(Path dir) throws IOException {
        if (!Files.isDirectory(dir)) {
            return List.of(dir);
        }

        try (Stream<Path> walk = Files.walk(dir)) {
            return walk
                .filter(Files::isRegularFile)
                .filter(p -> config.extensions().isEmpty() || matchesExtension(p))
                .toList();
        }
    }

    private boolean matchesExtension(Path path) {
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        if (dot < 0) return false;
        String ext = name.substring(dot);
        return config.extensions().contains(ext);
    }
}
