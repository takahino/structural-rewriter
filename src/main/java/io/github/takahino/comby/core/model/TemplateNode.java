package io.github.takahino.comby.core.model;

public sealed interface TemplateNode permits TemplateNode.Constant, TemplateNode.Hole {

    record Constant(String value) implements TemplateNode {}

    record Hole(
        String name,
        HoleSort sort,
        HoleDimension dimension,
        String regex  // sort==REGEX のときのみ使用
    ) implements TemplateNode {
        public Hole(String name, HoleSort sort) {
            this(name, sort, HoleDimension.CODE, null);
        }
    }
}
