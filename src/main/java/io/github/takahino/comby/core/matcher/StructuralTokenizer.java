package io.github.takahino.comby.core.matcher;

import java.util.List;

public interface StructuralTokenizer {
    List<StructuralToken> tokenize(String source);
}
