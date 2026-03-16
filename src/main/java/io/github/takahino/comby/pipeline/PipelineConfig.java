package io.github.takahino.comby.pipeline;

import io.github.takahino.comby.core.model.Specification;

import java.util.List;

public record PipelineConfig(
    List<Specification> specifications,
    String matcher,
    String directory,
    List<String> extensions,
    int jobs,
    boolean matchOnly,
    boolean jsonLines,
    boolean diff,
    boolean review,
    boolean count,
    boolean stdout,
    boolean inPlace
) {
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private List<Specification> specifications = List.of();
        private String matcher = "generic";
        private String directory = ".";
        private List<String> extensions = List.of();
        private int jobs = Runtime.getRuntime().availableProcessors();
        private boolean matchOnly = false;
        private boolean jsonLines = false;
        private boolean diff = false;
        private boolean review = false;
        private boolean count = false;
        private boolean stdout = false;
        private boolean inPlace = false;

        public Builder specifications(List<Specification> v) { specifications = v; return this; }
        public Builder matcher(String v) { matcher = v; return this; }
        public Builder directory(String v) { directory = v; return this; }
        public Builder extensions(List<String> v) { extensions = v; return this; }
        public Builder jobs(int v) { jobs = v; return this; }
        public Builder matchOnly(boolean v) { matchOnly = v; return this; }
        public Builder jsonLines(boolean v) { jsonLines = v; return this; }
        public Builder diff(boolean v) { diff = v; return this; }
        public Builder review(boolean v) { review = v; return this; }
        public Builder count(boolean v) { count = v; return this; }
        public Builder stdout(boolean v) { stdout = v; return this; }
        public Builder inPlace(boolean v) { inPlace = v; return this; }

        public PipelineConfig build() {
            return new PipelineConfig(specifications, matcher, directory, extensions,
                jobs, matchOnly, jsonLines, diff, review, count, stdout, inPlace);
        }
    }
}
