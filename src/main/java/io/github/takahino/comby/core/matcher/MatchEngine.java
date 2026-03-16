package io.github.takahino.comby.core.matcher;

import io.github.takahino.comby.core.model.*;
import io.github.takahino.comby.core.template.TemplateParser;

import java.util.*;
import java.util.regex.Pattern;

/**
 * comby のコアマッチングエンジン。
 * stateless 設計 - すべての状態は引数として渡す。
 */
public class MatchEngine {

    private final LanguageSyntax syntax;
    private final StructuralTokenizer tokenizer;
    private final Map<String, Pattern> regexCache = new HashMap<>();

    /** ANTLR Tokenizer を使用するコンストラクタ */
    public MatchEngine(LanguageSyntax syntax, StructuralTokenizer tokenizer) {
        this.syntax = syntax;
        this.tokenizer = tokenizer;
    }

    /** 後方互換コンストラクタ（手書きスキャナを使用） */
    public MatchEngine(LanguageSyntax syntax) {
        this(syntax, null);
    }

    /** STRING/COMMENT トークンのジャンプマップと、内部オフセットのスキップセットを保持 */
    private record MatchContext(Set<Integer> skippedOffsets, Map<Integer, Integer> tokenJumps) {}

    /**
     * ソーステキスト全体からマッチを探し、全マッチを返す。
     * コメント・文字列リテラルの内部からはマッチを開始しない。
     */
    public List<Match> findAll(String source, String templateStr, String filePath) {
        var nodes = TemplateParser.parse(templateStr);
        if (nodes.isEmpty()) return List.of();

        var ctx = (tokenizer != null)
                ? buildContextFromTokens(tokenizer.tokenize(source))
                : buildContextFromScan(source);

        var matches = new ArrayList<Match>();
        int startPos = 0;

        while (startPos <= source.length()) {
            if (startPos < source.length() && ctx.skippedOffsets().contains(startPos)) {
                startPos++;
                continue;
            }
            var cursor = new StringCursor(source, startPos);
            var result = tryMatchAt(cursor, nodes, MatchEnvironment.empty(), source, ctx);
            if (result.isPresent()) {
                var m = result.get();
                matches.add(Match.of(m.matchedText(), m.range(), m.environment(), filePath));
                int newPos = m.range().end().offset();
                if (newPos <= startPos) {
                    startPos++;
                } else {
                    startPos = newPos;
                }
            } else {
                startPos++;
            }
        }
        return matches;
    }

    /** ANTLR トークンリストから MatchContext を構築 */
    private MatchContext buildContextFromTokens(List<StructuralToken> tokens) {
        var skipped = new HashSet<Integer>();
        var jumps   = new HashMap<Integer, Integer>();
        for (var tok : tokens) {
            if (tok.type() == StructuralTokenType.STRING
                    || tok.type() == StructuralTokenType.COMMENT) {
                jumps.put(tok.startOffset(), tok.endOffset());
                for (int i = tok.startOffset() + 1; i < tok.endOffset(); i++) {
                    skipped.add(i);
                }
            }
        }
        return new MatchContext(skipped, jumps);
    }

    /** 手書きスキャナから MatchContext を構築（フォールバック） */
    private MatchContext buildContextFromScan(String source) {
        var offsets = new HashSet<Integer>();
        var cursor = new StringCursor(source);
        while (cursor.hasMore()) {
            int before = cursor.pos();
            if (tryConsumeString(cursor)) {
                for (int i = before + 1; i < cursor.pos() - 1; i++) offsets.add(i);
                continue;
            }
            if (tryConsumeComment(cursor)) {
                for (int i = before + 1; i < cursor.pos(); i++) offsets.add(i);
                continue;
            }
            cursor.advance();
        }
        return new MatchContext(offsets, Map.of());
    }

    /**
     * 指定位置からパターンにマッチするか試みる。
     */
    private Optional<Match> tryMatchAt(StringCursor cursor, List<TemplateNode> nodes,
                                        MatchEnvironment env, String fullSource,
                                        MatchContext ctx) {
        int startOffset = cursor.pos();
        var matchResult = matchNodes(cursor, nodes, env, ctx);
        if (matchResult.isEmpty()) return Optional.empty();

        int endOffset = cursor.pos();
        var startLoc = offsetToLocation(fullSource, startOffset);
        var endLoc = offsetToLocation(fullSource, endOffset);
        var range = Range.of(startLoc, endLoc);
        var matched = fullSource.substring(startOffset, endOffset);
        return Optional.of(Match.of(matched, range, matchResult.get(), null));
    }

    /**
     * ノードリスト全体をマッチさせる。成功時は環境を返す。
     */
    public Optional<MatchEnvironment> matchNodes(StringCursor cursor, List<TemplateNode> nodes,
                                                  MatchEnvironment env) {
        return matchNodes(cursor, nodes, env, new MatchContext(Set.of(), Map.of()));
    }

    private Optional<MatchEnvironment> matchNodes(StringCursor cursor, List<TemplateNode> nodes,
                                                   MatchEnvironment env, MatchContext ctx) {
        for (int i = 0; i < nodes.size(); i++) {
            var node = nodes.get(i);
            switch (node) {
                case TemplateNode.Constant c -> {
                    if (!cursor.consume(c.value())) return Optional.empty();
                }
                case TemplateNode.Hole h -> {
                    var stopCondition = computeStopCondition(nodes, i + 1);
                    var result = matchHole(cursor, h, stopCondition, env, ctx);
                    if (result.isEmpty()) return Optional.empty();
                    env = result.get();
                }
            }
        }
        return Optional.of(env);
    }

    /**
     * ホールのマッチ。停止条件まで消費してキャプチャする。
     */
    private Optional<MatchEnvironment> matchHole(StringCursor cursor, TemplateNode.Hole hole,
                                                   String stopCondition, MatchEnvironment env,
                                                   MatchContext ctx) {
        int startPos = cursor.pos();

        String captured = switch (hole.sort()) {
            case ALPHANUM   -> consumeAlphanum(cursor);
            case NON_SPACE  -> consumeNonSpace(cursor, stopCondition);
            case LINE       -> consumeLine(cursor);
            case REGEX      -> consumeRegex(cursor, hole.regex());
            case BLANK      -> consumeBlank(cursor);
            case EVERYTHING, EXPRESSION ->
                consumeEverything(cursor, stopCondition, ctx);
        };

        if (captured == null) return Optional.empty();

        if (env.contains(hole.name())) {
            var existing = env.get(hole.name()).get();
            if (!existing.value().equals(captured)) return Optional.empty();
            return Optional.of(env);
        }

        if (hole.name().equals("_")) {
            return Optional.of(env);
        }

        int endPos = cursor.pos();
        var startLoc = offsetToLocation(cursor.src(), startPos);
        var endLoc = offsetToLocation(cursor.src(), endPos);
        var range = Range.of(startLoc, endLoc);
        var capturedValue = CapturedValue.of(hole.name(), captured, range);
        return Optional.of(env.bind(hole.name(), capturedValue));
    }

    private String computeStopCondition(List<TemplateNode> nodes, int fromIndex) {
        for (int i = fromIndex; i < nodes.size(); i++) {
            if (nodes.get(i) instanceof TemplateNode.Constant c && !c.value().isEmpty()) {
                return c.value();
            }
        }
        return null;
    }

    /**
     * EVERYTHING マッチ: 括弧バランスを保ちながら停止条件まで消費。
     */
    private String consumeEverything(StringCursor cursor, String stopCondition, MatchContext ctx) {
        int start = cursor.pos();
        int depth = 0;

        while (cursor.hasMore()) {
            if (depth == 0 && stopCondition != null && cursor.startsWith(stopCondition)) {
                break;
            }
            // ANTLR トークンによるジャンプ（文字列・コメント一括スキップ）
            if (tokenizer != null) {
                Integer jumpTo = ctx.tokenJumps().get(cursor.pos());
                if (jumpTo != null) {
                    cursor.setPos(jumpTo);
                    continue;
                }
            } else {
                // フォールバック: 手書きスキャナ
                if (tryConsumeString(cursor)) continue;
                if (tryConsumeComment(cursor)) continue;
            }

            char c = cursor.peek();

            // ANTLR モード: 文字列/コメント内部（skippedOffsets）ではデリミタ深度を変えない
            boolean inStringOrComment = (tokenizer != null)
                    && ctx.skippedOffsets().contains(cursor.pos());

            if (isOpenDelim(c)) {
                if (!inStringOrComment) depth++;
                cursor.advance();
                continue;
            }

            if (isCloseDelim(c)) {
                if (!inStringOrComment) {
                    if (depth == 0) break;
                    depth--;
                }
                cursor.advance();
                continue;
            }

            cursor.advance();
        }

        if (stopCondition == null && depth != 0) {
            cursor.setPos(start);
            return null;
        }

        String result = cursor.src().substring(start, cursor.pos());
        if (result.isEmpty() && stopCondition == null) return null;
        return result;
    }

    /**
     * 文字列リテラルの消費を試みる（フォールバック用）。
     */
    boolean tryConsumeString(StringCursor cursor) {
        for (var sd : syntax.stringDelimiters()) {
            if (cursor.startsWith(sd.open())) {
                cursor.skip(sd.open().length());
                while (cursor.hasMore()) {
                    if (sd.escapable() && cursor.peek() == '\\' && cursor.hasMore(1)) {
                        cursor.skip(2);
                        continue;
                    }
                    if (cursor.startsWith(sd.close())) {
                        cursor.skip(sd.close().length());
                        return true;
                    }
                    cursor.advance();
                }
                return true;
            }
        }
        return false;
    }

    /**
     * コメントの消費を試みる（フォールバック用）。
     */
    boolean tryConsumeComment(StringCursor cursor) {
        for (var cs : syntax.comments()) {
            if (cursor.startsWith(cs.open())) {
                cursor.skip(cs.open().length());
                int depth = 1;
                while (cursor.hasMore()) {
                    if (cs.nested() && cursor.startsWith(cs.open())) {
                        cursor.skip(cs.open().length());
                        depth++;
                        continue;
                    }
                    if (cursor.startsWith(cs.close())) {
                        cursor.skip(cs.close().length());
                        depth--;
                        if (depth == 0) return true;
                        continue;
                    }
                    cursor.advance();
                }
                return true;
            }
        }
        return false;
    }

    private String consumeAlphanum(StringCursor cursor) {
        int start = cursor.pos();
        while (cursor.hasMore() && (Character.isLetterOrDigit(cursor.peek()) || cursor.peek() == '_')) {
            cursor.advance();
        }
        if (cursor.pos() == start) return null;
        return cursor.src().substring(start, cursor.pos());
    }

    private String consumeNonSpace(StringCursor cursor, String stopCondition) {
        int start = cursor.pos();
        while (cursor.hasMore() && !Character.isWhitespace(cursor.peek())) {
            if (stopCondition != null && cursor.startsWith(stopCondition)) break;
            cursor.advance();
        }
        if (cursor.pos() == start) return null;
        return cursor.src().substring(start, cursor.pos());
    }

    private String consumeLine(StringCursor cursor) {
        int start = cursor.pos();
        while (cursor.hasMore() && cursor.peek() != '\n') {
            cursor.advance();
        }
        return cursor.src().substring(start, cursor.pos());
    }

    private String consumeBlank(StringCursor cursor) {
        int start = cursor.pos();
        while (cursor.hasMore() && Character.isWhitespace(cursor.peek())) {
            cursor.advance();
        }
        if (cursor.pos() == start) return null;
        return cursor.src().substring(start, cursor.pos());
    }

    private String consumeRegex(StringCursor cursor, String regex) {
        if (regex == null) return null;
        var pattern = regexCache.computeIfAbsent(regex, r -> Pattern.compile("^(?:" + r + ")"));
        var matcher = pattern.matcher(cursor.remaining());
        if (!matcher.find()) return null;
        String captured = matcher.group();
        cursor.skip(captured.length());
        return captured;
    }

    private boolean isOpenDelim(char c) {
        for (var d : syntax.openDelimiters()) {
            if (d.length() == 1 && d.charAt(0) == c) return true;
        }
        return false;
    }

    private boolean isCloseDelim(char c) {
        for (var d : syntax.closeDelimiters()) {
            if (d.length() == 1 && d.charAt(0) == c) return true;
        }
        return false;
    }

    /** オフセットから Location を計算 */
    public static Location offsetToLocation(String src, int offset) {
        int line = 1, col = 1;
        for (int i = 0; i < offset && i < src.length(); i++) {
            if (src.charAt(i) == '\n') { line++; col = 1; }
            else { col++; }
        }
        return Location.of(offset, line, col);
    }
}
