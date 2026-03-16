package io.github.takahino.comby.core.matcher;

import io.github.takahino.comby.core.model.Match;
import io.github.takahino.comby.core.model.Specification;
import io.github.takahino.comby.core.rewrite.RewriteEngine;
import io.github.takahino.comby.core.rule.RuleEngine;

import java.util.List;

/**
 * LanguageMatcher の共通実装。
 * サブクラスは syntax() と name() と extensions() を実装するだけでよい。
 */
public abstract class AbstractLanguageMatcher implements LanguageMatcher {

    @Override
    public List<Match> findMatches(String source, Specification spec, String filePath) {
        var engine = new MatchEngine(syntax(), tokenizer());
        var matches = engine.findAll(source, spec.matchTemplate(), filePath);

        if (spec.rule() == null) return matches;

        var ruleEngine = new RuleEngine(engine);
        return matches.stream()
            .filter(m -> ruleEngine.evaluate(spec.rule(), m.environment(), source))
            .toList();
    }

    @Override
    public String rewrite(String source, Specification spec, String filePath) {
        var matches = findMatches(source, spec, filePath);
        if (matches.isEmpty()) return source;
        return RewriteEngine.rewrite(source, matches, spec.rewriteTemplate());
    }
}
