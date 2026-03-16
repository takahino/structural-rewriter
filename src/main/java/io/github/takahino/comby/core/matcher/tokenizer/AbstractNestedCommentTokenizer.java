package io.github.takahino.comby.core.matcher.tokenizer;

import io.github.takahino.comby.core.matcher.StructuralToken;
import io.github.takahino.comby.core.matcher.StructuralTokenType;
import io.github.takahino.comby.core.matcher.StructuralTokenizer;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.Token;

import java.util.ArrayList;
import java.util.List;

abstract class AbstractNestedCommentTokenizer implements StructuralTokenizer {

    protected abstract Lexer createLexer(String source);

    protected abstract StructuralTokenType mapType(int type);

    /** ネストコメントを開始するトークンタイプ（例: OPEN_BLOCK_COMMENT）*/
    protected abstract int openCommentType();

    /** ネスト深度を増加させるトークンタイプ（例: NBC_OPEN）*/
    protected abstract int nestedOpenType();

    /** ネスト深度を減少させるトークンタイプ（例: NBC_CLOSE）*/
    protected abstract int nestedCloseType();

    @Override
    public final List<StructuralToken> tokenize(String source) {
        var lexer = createLexer(source);
        lexer.removeErrorListeners();
        var stream = new CommonTokenStream(lexer);
        stream.fill();

        var tokens = stream.getTokens();
        var result = new ArrayList<StructuralToken>();
        int i = 0;
        while (i < tokens.size()) {
            var t = tokens.get(i);
            if (t.getType() == Token.EOF) break;

            if (t.getType() == openCommentType()) {
                // ネストコメント全体を1トークンとして結合（深度追跡）
                int start = t.getStartIndex();
                int end = t.getStopIndex() + 1;
                int depth = 1;
                i++;
                while (i < tokens.size() && depth > 0) {
                    var ct = tokens.get(i);
                    if (ct.getType() == Token.EOF) break;
                    end = ct.getStopIndex() + 1;
                    if (ct.getType() == nestedOpenType()) {
                        depth++;
                    } else if (ct.getType() == nestedCloseType()) {
                        depth--;
                    }
                    i++;
                }
                result.add(new StructuralToken(StructuralTokenType.COMMENT, start, end));
            } else {
                result.add(new StructuralToken(mapType(t.getType()),
                        t.getStartIndex(), t.getStopIndex() + 1));
                i++;
            }
        }
        return result;
    }
}
