package io.github.takahino.comby.output;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.takahino.comby.core.model.Match;
import io.github.takahino.comby.pipeline.FileProcessor;

import java.util.List;

/**
 * JSON Lines（1マッチ1行）形式の出力フォーマッター。
 */
public class JsonLinesFormatter implements OutputFormatter {

    private final ObjectMapper mapper = new ObjectMapper();
    private final boolean matchOnly;

    public JsonLinesFormatter(boolean matchOnly) {
        this.matchOnly = matchOnly;
    }

    @Override
    public void format(List<FileProcessor.FileResult> results) {
        for (var result : results) {
            if (matchOnly) {
                for (Match m : result.matches()) {
                    System.out.println(toMatchJson(m, result.filePath().toString()));
                }
            } else if (result.hasChanges()) {
                System.out.println(toRewriteJson(result));
            }
        }
    }

    private String toMatchJson(Match m, String filePath) {
        try {
            ObjectNode node = mapper.createObjectNode();
            node.put("uri", filePath);
            node.put("matched_text", m.matchedText());

            ObjectNode range = node.putObject("range");
            ObjectNode start = range.putObject("start");
            start.put("offset", m.range().start().offset());
            start.put("line", m.range().start().line());
            start.put("column", m.range().start().column());
            ObjectNode end = range.putObject("end");
            end.put("offset", m.range().end().offset());
            end.put("line", m.range().end().line());
            end.put("column", m.range().end().column());

            ArrayNode env = node.putArray("environment");
            m.environment().all().forEach((name, cv) -> {
                ObjectNode binding = env.addObject();
                binding.put("variable", name);
                binding.put("value", cv.value());
            });

            return mapper.writeValueAsString(node);
        } catch (Exception e) {
            return "{}";
        }
    }

    private String toRewriteJson(FileProcessor.FileResult result) {
        try {
            ObjectNode node = mapper.createObjectNode();
            node.put("uri", result.filePath().toString());
            node.put("rewritten_source", result.rewrittenSource());
            return mapper.writeValueAsString(node);
        } catch (Exception e) {
            return "{}";
        }
    }
}
