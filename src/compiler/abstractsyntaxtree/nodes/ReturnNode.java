package compiler.abstractsyntaxtree.nodes;

import compiler.abstractsyntaxtree.Node;
import compiler.abstractsyntaxtree.NodeClasz;

public class ReturnNode extends Node
{
    private static int DOT_COUNTER = 0;

    public ReturnNode( Node expr )
    {
        this.nodeClasz = NodeClasz.RETURN;
        this.name = "RETURN";
        this.dotName = name + "_" + DOT_COUNTER++;

        this.left = expr;
    }
}
