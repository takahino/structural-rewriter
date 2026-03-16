package io.github.takahino.comby.cli;

import com.moandjiezana.toml.Toml;
import io.github.takahino.comby.core.model.Specification;
import io.github.takahino.comby.output.*;
import io.github.takahino.comby.pipeline.FileProcessor;
import io.github.takahino.comby.pipeline.Pipeline;
import io.github.takahino.comby.pipeline.PipelineConfig;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

@Command(
    name = "comby",
    mixinStandardHelpOptions = true,
    version = "comby 0.1.0",
    description = "Structural code search and replace"
)
public class CombyCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "Match template", arity = "0..1")
    private String matchTemplate;

    @Parameters(index = "1", description = "Rewrite template", arity = "0..1")
    private String rewriteTemplate;

    @Option(names = {"-d", "--directory"}, description = "Directory to search")
    private String directory;

    @Option(names = {"-stdin"}, description = "Read from stdin")
    private boolean stdin;

    @Option(names = {"-stdout"}, description = "Write to stdout")
    private boolean stdout;

    @Option(names = {"-i", "-in-place"}, description = "Rewrite files in place")
    private boolean inPlace;

    @Option(names = {"-matcher", "-m"}, description = "Language matcher (e.g. java, python)")
    private String matcher;

    @Option(names = {"-rule"}, description = "Rule to filter matches")
    private String rule;

    @Option(names = {"-match-only"}, description = "Only show matches, do not rewrite")
    private boolean matchOnly;

    @Option(names = {"-json-lines"}, description = "Output in JSON Lines format")
    private boolean jsonLines;

    @Option(names = {"-diff"}, description = "Output as unified diff")
    private boolean diff;

    @Option(names = {"-review"}, description = "Interactively review changes")
    private boolean review;

    @Option(names = {"-count"}, description = "Show match count")
    private boolean count;

    @Option(names = {"-jobs", "-j"}, description = "Number of parallel jobs", defaultValue = "0")
    private int jobs;

    @Option(names = {"-f", "-extensions"}, description = "File extensions (comma-separated, e.g. .java,.kt)")
    private String extensions;

    @Option(names = {"-config"}, description = "TOML config file")
    private File configFile;

    @Option(names = {"-templates"}, description = "Directory of TOML template files")
    private File templatesDir;

    @Override
    public Integer call() throws Exception {
        var specs = loadSpecifications();
        if (specs.isEmpty()) {
            System.err.println("No match template specified.");
            return 1;
        }

        int actualJobs = jobs > 0 ? jobs : Runtime.getRuntime().availableProcessors();
        var extList = parseExtensions();

        var configBuilder = PipelineConfig.builder()
            .specifications(specs)
            .matcher(matcher != null ? matcher : "generic")
            .directory(directory != null ? directory : ".")
            .extensions(extList)
            .jobs(actualJobs)
            .matchOnly(matchOnly)
            .jsonLines(jsonLines)
            .diff(diff)
            .review(review)
            .count(count)
            .stdout(stdout)
            .inPlace(inPlace);

        var config = configBuilder.build();
        var formatter = selectFormatter(config);

        List<FileProcessor.FileResult> results;

        if (useStdin()) {
            String source = new String(System.in.readAllBytes());
            var processor = new FileProcessor(config);
            results = List.of(processor.processSource(source, "<stdin>"));
        } else {
            var pipeline = new Pipeline(config);
            results = pipeline.run();
        }

        if (count) {
            long total = results.stream().mapToLong(r -> r.matches().size()).sum();
            System.out.println(total);
            return 0;
        }

        if (inPlace && !matchOnly && !useStdin()) {
            for (var result : results) {
                if (result.hasChanges()) {
                    Files.writeString(result.filePath(), result.rewrittenSource());
                }
            }
            return 0;
        }

        formatter.format(results);

        return 0;
    }

    /**
     * stdin から読み込むかどうかを判定する。
     * {@code -stdin} フラグが明示されている場合、または
     * {@code -d} が未指定かつ stdin がパイプ（非インタラクティブ）の場合に true。
     */
    private boolean useStdin() {
        return stdin || (directory == null && System.console() == null);
    }

    private List<Specification> loadSpecifications() throws IOException {
        var specs = new ArrayList<Specification>();

        // コマンドラインのテンプレート
        if (matchTemplate != null) {
            specs.add(Specification.of(matchTemplate,
                rewriteTemplate != null ? rewriteTemplate : matchTemplate,
                rule));
        }

        // TOML設定ファイル
        if (configFile != null && configFile.exists()) {
            specs.addAll(loadFromToml(configFile));
        }

        // テンプレートディレクトリ
        if (templatesDir != null && templatesDir.isDirectory()) {
            var tomlFiles = templatesDir.listFiles(f -> f.getName().endsWith(".toml"));
            if (tomlFiles != null) {
                for (var f : tomlFiles) {
                    specs.addAll(loadFromToml(f));
                }
            }
        }

        return specs;
    }

    private List<Specification> loadFromToml(File file) throws IOException {
        var specs = new ArrayList<Specification>();
        var toml = new Toml().read(file);

        // [[rules]] 配列形式
        var rules = toml.getTables("rules");
        if (rules != null) {
            for (var entry : rules) {
                String match = entry.getString("match");
                String rewrite = entry.getString("rewrite", "");
                String ruleStr = entry.getString("rule", null);
                if (match != null) {
                    specs.add(Specification.of(match, rewrite, ruleStr));
                }
            }
        }

        return specs;
    }

    private List<String> parseExtensions() {
        if (extensions == null || extensions.isBlank()) return List.of();
        var result = new ArrayList<String>();
        for (String ext : extensions.split(",")) {
            String trimmed = ext.trim();
            if (!trimmed.startsWith(".")) trimmed = "." + trimmed;
            result.add(trimmed);
        }
        return result;
    }

    private OutputFormatter selectFormatter(PipelineConfig config) {
        if (config.review()) return new InteractiveFormatter();
        if (config.diff()) return new DiffFormatter();
        if (config.jsonLines()) return new JsonLinesFormatter(config.matchOnly());
        // stdin モード or -stdout 指定時は変更なしでも出力する
        boolean alwaysPrint = useStdin() || stdout;
        return new TextFormatter(config.matchOnly(), alwaysPrint);
    }
}
