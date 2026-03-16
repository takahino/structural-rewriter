package io.github.takahino.comby.core.matcher.tokenizer;

import io.github.takahino.comby.core.matcher.CombyHashLexer;
import io.github.takahino.comby.core.matcher.StructuralTokenType;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.Lexer;

public class CombyHashTokenizer extends AbstractSimpleTokenizer {

    @Override
    protected Lexer createLexer(String source) {
        return new CombyHashLexer(CharStreams.fromString(source));
    }

    @Override
    protected StructuralTokenType mapType(int type) {
        return switch (type) {
            case CombyHashLexer.STRING_DQUOTE,
                 CombyHashLexer.STRING_SQUOTE -> StructuralTokenType.STRING;
            case CombyHashLexer.LINE_COMMENT  -> StructuralTokenType.COMMENT;
            case CombyHashLexer.OPEN          -> StructuralTokenType.OPEN;
            case CombyHashLexer.CLOSE         -> StructuralTokenType.CLOSE;
            default                           -> StructuralTokenType.OTHER;
        };
    }
}
