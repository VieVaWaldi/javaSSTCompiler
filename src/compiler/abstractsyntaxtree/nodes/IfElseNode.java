package compiler.abstractsyntaxtree.nodes;

import compiler.abstractsyntaxtree.INode;
import compiler.abstractsyntaxtree.NodeClasz;

public class IfElseNode
                extends INode
{
    private static int DOT_COUNTER = 0;

    /* Automatically creates @(IfNode) */
    public IfElseNode( INode ifNode )
    {
        this.nodeClasz = NodeClasz.IFELSE;
        this.name = "IF_ELSE";
        this.dotName = name + "_" + DOT_COUNTER++;

        this.left = ifNode;
    }
}
