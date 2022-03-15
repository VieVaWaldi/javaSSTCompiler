package compiler.abstractsyntaxtree.nodes;

import compiler.abstractsyntaxtree.Node;
import compiler.abstractsyntaxtree.NodeClasz;

public class IfElseNode
                extends Node
{
    private static int DOT_COUNTER = 0;

    /* Automatically creates @(IfNode) */
    public IfElseNode( Node ifNode )
    {
        this.nodeClasz = NodeClasz.IFELSE;
        this.name = "IF_ELSE";
        this.dotName = name + "_" + DOT_COUNTER++;

        this.left = ifNode;
    }
}
