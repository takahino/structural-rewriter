package io.github.takahino.comby.template;

import io.github.takahino.comby.core.model.HoleSort;
import io.github.takahino.comby.core.model.TemplateNode;
import io.github.takahino.comby.core.template.TemplateParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TemplateParserTest {

    @Test
    void parseConstantOnly() {
        var nodes = TemplateParser.parse("hello world");
        assertEquals(1, nodes.size());
        assertInstanceOf(TemplateNode.Constant.class, nodes.get(0));
        assertEquals("hello world", ((TemplateNode.Constant) nodes.get(0)).value());
    }

    @Test
    void parseSimpleHole() {
        var nodes = TemplateParser.parse(":[x]");
        assertEquals(1, nodes.size());
        var hole = assertInstanceOf(TemplateNode.Hole.class, nodes.get(0));
        assertEquals("x", hole.name());
        assertEquals(HoleSort.EVERYTHING, hole.sort());
    }

    @Test
    void parseAlphanumHole() {
        var nodes = TemplateParser.parse(":[[var]]");
        assertEquals(1, nodes.size());
        var hole = assertInstanceOf(TemplateNode.Hole.class, nodes.get(0));
        assertEquals("var", hole.name());
        assertEquals(HoleSort.ALPHANUM, hole.sort());
    }

    @Test
    void parseRegexHole() {
        var nodes = TemplateParser.parse(":[x~[0-9]+]");
        assertEquals(1, nodes.size());
        var hole = assertInstanceOf(TemplateNode.Hole.class, nodes.get(0));
        assertEquals("x", hole.name());
        assertEquals(HoleSort.REGEX, hole.sort());
        assertEquals("[0-9]+", hole.regex());
    }

    @Test
    void parseNonSpaceHole() {
        var nodes = TemplateParser.parse(":[x.]");
        assertEquals(1, nodes.size());
        var hole = assertInstanceOf(TemplateNode.Hole.class, nodes.get(0));
        assertEquals(HoleSort.NON_SPACE, hole.sort());
    }

    @Test
    void parseLineHole() {
        var nodes = TemplateParser.parse(":[x\\n]");
        assertEquals(1, nodes.size());
        var hole = assertInstanceOf(TemplateNode.Hole.class, nodes.get(0));
        assertEquals(HoleSort.LINE, hole.sort());
    }

    @Test
    void parseEllipsis() {
        var nodes = TemplateParser.parse("...");
        assertEquals(1, nodes.size());
        var hole = assertInstanceOf(TemplateNode.Hole.class, nodes.get(0));
        assertEquals(HoleSort.EVERYTHING, hole.sort());
        assertEquals("_", hole.name());
    }

    @Test
    void parseMixedTemplate() {
        var nodes = TemplateParser.parse("if (:[cond]) { :[body] }");
        assertEquals(5, nodes.size());
        assertInstanceOf(TemplateNode.Constant.class, nodes.get(0));
        assertInstanceOf(TemplateNode.Hole.class, nodes.get(1));
        assertInstanceOf(TemplateNode.Constant.class, nodes.get(2));
        assertInstanceOf(TemplateNode.Hole.class, nodes.get(3));
        assertInstanceOf(TemplateNode.Constant.class, nodes.get(4));

        assertEquals("if (", ((TemplateNode.Constant) nodes.get(0)).value());
        assertEquals("cond", ((TemplateNode.Hole) nodes.get(1)).name());
    }
}
