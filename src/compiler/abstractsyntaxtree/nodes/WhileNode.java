package compiler.abstractsyntaxtree.nodes;

import compiler.abstractsyntaxtree.Node;
import compiler.abstractsyntaxtree.NodeClasz;

public class WhileNode extends Node
{
    private static int DOT_COUNTER = 0;

    public WhileNode( Node expr )
    {
        this.nodeClasz = NodeClasz.WHILE;
        this.name = "WHILE";
        this.dotName = name + "_" + DOT_COUNTER++;

        this.left = expr;
    }
}
