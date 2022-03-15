package compiler.abstractsyntaxtree.nodes;

import compiler.abstractsyntaxtree.Node;
import compiler.abstractsyntaxtree.NodeClasz;
import compiler.symboltable.Type;

public class NumberNode
                extends Node
{
    private static int DOT_COUNTER = 0;

    public NumberNode( String name )
    {
        this.nodeClasz = NodeClasz.METHOD_VAR;
        this.type = Type.Int;

        this.name = name;
        this.dotName = "N" + this.name + "_" + DOT_COUNTER++;
    }
}
