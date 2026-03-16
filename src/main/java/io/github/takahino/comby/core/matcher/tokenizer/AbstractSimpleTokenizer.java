package io.github.takahino.comby.core.matcher.tokenizer;

import io.github.takahino.comby.core.matcher.StructuralToken;
import io.github.takahino.comby.core.matcher.StructuralTokenType;
import io.github.takahino.comby.core.matcher.StructuralTokenizer;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.Token;

import java.util.List;

abstract class AbstractSimpleTokenizer implements StructuralTokenizer {

    protected abstract Lexer createLexer(String source);

    protected abstract StructuralTokenType mapType(int type);

    @Override
    public final List<StructuralToken> tokenize(String source) {
        var lexer = createLexer(source);
        lexer.removeErrorListeners();
        var stream = new CommonTokenStream(lexer);
        stream.fill();
        return stream.getTokens().stream()
                .filter(t -> t.getType() != Token.EOF)
                .map(t -> new StructuralToken(mapType(t.getType()),
                        t.getStartIndex(), t.getStopIndex() + 1))
                .toList();
    }
}
