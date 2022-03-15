package compiler.abstractsyntaxtree.nodes;

import compiler.abstractsyntaxtree.Node;
import compiler.abstractsyntaxtree.NodeClasz;

public class IfNode
                extends Node
{
    private static int DOT_COUNTER = 0;

    public IfNode( Node expr )
    {
        this.nodeClasz = NodeClasz.IF;
        this.name = "IF";
        this.dotName = name + "_" + DOT_COUNTER++;

        this.left = expr;
    }
}
