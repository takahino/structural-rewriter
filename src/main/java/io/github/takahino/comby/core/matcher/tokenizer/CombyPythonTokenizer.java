package io.github.takahino.comby.core.matcher.tokenizer;

import io.github.takahino.comby.core.matcher.CombyPythonLexer;
import io.github.takahino.comby.core.matcher.StructuralTokenType;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.Lexer;

public class CombyPythonTokenizer extends AbstractSimpleTokenizer {

    @Override
    protected Lexer createLexer(String source) {
        return new CombyPythonLexer(CharStreams.fromString(source));
    }

    @Override
    protected StructuralTokenType mapType(int type) {
        return switch (type) {
            case CombyPythonLexer.STRING_TRIPLE_DQ,
                 CombyPythonLexer.STRING_TRIPLE_SQ,
                 CombyPythonLexer.STRING_DQUOTE,
                 CombyPythonLexer.STRING_SQUOTE -> StructuralTokenType.STRING;
            case CombyPythonLexer.LINE_COMMENT   -> StructuralTokenType.COMMENT;
            case CombyPythonLexer.OPEN           -> StructuralTokenType.OPEN;
            case CombyPythonLexer.CLOSE          -> StructuralTokenType.CLOSE;
            default                              -> StructuralTokenType.OTHER;
        };
    }
}
