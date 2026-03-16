package io.github.takahino.comby.core.matcher.tokenizer;

import io.github.takahino.comby.core.matcher.CombyOcamlLexer;
import io.github.takahino.comby.core.matcher.StructuralTokenType;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.Lexer;

public class CombyOcamlTokenizer extends AbstractNestedCommentTokenizer {

    @Override
    protected Lexer createLexer(String source) {
        return new CombyOcamlLexer(CharStreams.fromString(source));
    }

    @Override
    protected int openCommentType() { return CombyOcamlLexer.OPEN_COMMENT; }

    @Override
    protected int nestedOpenType() { return CombyOcamlLexer.NC_OPEN; }

    @Override
    protected int nestedCloseType() { return CombyOcamlLexer.NC_CLOSE; }

    @Override
    protected StructuralTokenType mapType(int type) {
        return switch (type) {
            case CombyOcamlLexer.STRING_DQUOTE -> StructuralTokenType.STRING;
            case CombyOcamlLexer.OPEN          -> StructuralTokenType.OPEN;
            case CombyOcamlLexer.CLOSE         -> StructuralTokenType.CLOSE;
            default                            -> StructuralTokenType.OTHER;
        };
    }
}
