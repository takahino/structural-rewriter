package io.github.takahino.comby.core.matcher.tokenizer;

import io.github.takahino.comby.core.matcher.CombyCLikeLexer;
import io.github.takahino.comby.core.matcher.StructuralTokenType;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.Lexer;

public class CombyCLikeTokenizer extends AbstractSimpleTokenizer {

    @Override
    protected Lexer createLexer(String source) {
        return new CombyCLikeLexer(CharStreams.fromString(source));
    }

    @Override
    protected StructuralTokenType mapType(int type) {
        return switch (type) {
            case CombyCLikeLexer.STRING_DQUOTE,
                 CombyCLikeLexer.STRING_SQUOTE -> StructuralTokenType.STRING;
            case CombyCLikeLexer.LINE_COMMENT,
                 CombyCLikeLexer.BLOCK_COMMENT -> StructuralTokenType.COMMENT;
            case CombyCLikeLexer.OPEN          -> StructuralTokenType.OPEN;
            case CombyCLikeLexer.CLOSE         -> StructuralTokenType.CLOSE;
            default                            -> StructuralTokenType.OTHER;
        };
    }
}
