package io.github.takahino.comby.core.matcher;

/**
 * バックトラック可能な文字列走査カーソル。
 * スレッドセーフではないが、MatchEngine は stateless で使うためVirtual Threads競合なし。
 */
public final class StringCursor {
    private final String src;
    private int pos;

    public StringCursor(String src) {
        this.src = src;
        this.pos = 0;
    }

    public StringCursor(String src, int initialPos) {
        this.src = src;
        this.pos = initialPos;
    }

    public int pos() { return pos; }
    public String src() { return src; }
    public int length() { return src.length(); }
    public boolean hasMore() { return pos < src.length(); }
    public boolean hasMore(int ahead) { return pos + ahead < src.length(); }

    public char peek() { return src.charAt(pos); }
    public char peek(int offset) { return src.charAt(pos + offset); }
    public char peekAt(int absPos) { return src.charAt(absPos); }

    public char advance() { return src.charAt(pos++); }
    public void skip(int n) { pos += n; }
    public void setPos(int p) { pos = p; }

    public boolean startsWith(String s) {
        return src.startsWith(s, pos);
    }

    public boolean consume(String s) {
        if (startsWith(s)) {
            pos += s.length();
            return true;
        }
        return false;
    }

    public boolean consume(char c) {
        if (hasMore() && peek() == c) {
            pos++;
            return true;
        }
        return false;
    }

    public String substring(int from, int to) {
        return src.substring(from, to);
    }

    public String remaining() {
        return src.substring(pos);
    }

    /** 現在位置のスナップショット（バックトラック用） */
    public int save() { return pos; }

    /** バックトラック */
    public void restore(int savedPos) { pos = savedPos; }

    public String srcFrom(int from) {
        return src.substring(from, pos);
    }
}
