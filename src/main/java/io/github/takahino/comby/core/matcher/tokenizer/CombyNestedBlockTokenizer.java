package io.github.takahino.comby.core.matcher.tokenizer;

import io.github.takahino.comby.core.matcher.CombyNestedBlockLexer;
import io.github.takahino.comby.core.matcher.StructuralTokenType;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.Lexer;

public class CombyNestedBlockTokenizer extends AbstractNestedCommentTokenizer {

    @Override
    protected Lexer createLexer(String source) {
        return new CombyNestedBlockLexer(CharStreams.fromString(source));
    }

    @Override
    protected int openCommentType() { return CombyNestedBlockLexer.OPEN_BLOCK_COMMENT; }

    @Override
    protected int nestedOpenType() { return CombyNestedBlockLexer.NBC_OPEN; }

    @Override
    protected int nestedCloseType() { return CombyNestedBlockLexer.NBC_CLOSE; }

    @Override
    protected StructuralTokenType mapType(int type) {
        return switch (type) {
            case CombyNestedBlockLexer.STRING_DQUOTE,
                 CombyNestedBlockLexer.STRING_SQUOTE -> StructuralTokenType.STRING;
            case CombyNestedBlockLexer.LINE_COMMENT  -> StructuralTokenType.COMMENT;
            case CombyNestedBlockLexer.OPEN          -> StructuralTokenType.OPEN;
            case CombyNestedBlockLexer.CLOSE         -> StructuralTokenType.CLOSE;
            default                                  -> StructuralTokenType.OTHER;
        };
    }
}
