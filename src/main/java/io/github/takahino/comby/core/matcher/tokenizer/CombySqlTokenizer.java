package io.github.takahino.comby.core.matcher.tokenizer;

import io.github.takahino.comby.core.matcher.CombySqlLexer;
import io.github.takahino.comby.core.matcher.StructuralTokenType;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.Lexer;

public class CombySqlTokenizer extends AbstractSimpleTokenizer {

    @Override
    protected Lexer createLexer(String source) {
        return new CombySqlLexer(CharStreams.fromString(source));
    }

    @Override
    protected StructuralTokenType mapType(int type) {
        return switch (type) {
            case CombySqlLexer.STRING_SQUOTE,
                 CombySqlLexer.STRING_DQUOTE -> StructuralTokenType.STRING;
            case CombySqlLexer.LINE_COMMENT,
                 CombySqlLexer.BLOCK_COMMENT -> StructuralTokenType.COMMENT;
            case CombySqlLexer.OPEN          -> StructuralTokenType.OPEN;
            case CombySqlLexer.CLOSE         -> StructuralTokenType.CLOSE;
            default                          -> StructuralTokenType.OTHER;
        };
    }
}
