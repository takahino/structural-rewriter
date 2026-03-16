package io.github.takahino.comby.core.matcher.tokenizer;

import io.github.takahino.comby.core.matcher.CombyGoLexer;
import io.github.takahino.comby.core.matcher.StructuralTokenType;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.Lexer;

public class CombyGoTokenizer extends AbstractSimpleTokenizer {

    @Override
    protected Lexer createLexer(String source) {
        return new CombyGoLexer(CharStreams.fromString(source));
    }

    @Override
    protected StructuralTokenType mapType(int type) {
        return switch (type) {
            case CombyGoLexer.STRING_BACKTICK,
                 CombyGoLexer.STRING_DQUOTE,
                 CombyGoLexer.STRING_SQUOTE -> StructuralTokenType.STRING;
            case CombyGoLexer.LINE_COMMENT,
                 CombyGoLexer.BLOCK_COMMENT -> StructuralTokenType.COMMENT;
            case CombyGoLexer.OPEN          -> StructuralTokenType.OPEN;
            case CombyGoLexer.CLOSE         -> StructuralTokenType.CLOSE;
            default                         -> StructuralTokenType.OTHER;
        };
    }
}
