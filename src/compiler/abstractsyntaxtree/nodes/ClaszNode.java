package compiler.abstractsyntaxtree.nodes;

import compiler.abstractsyntaxtree.Node;
import compiler.abstractsyntaxtree.NodeClasz;

public class ClaszNode
                extends Node
{
    public ClaszNode( String name )
    {
        this.nodeClasz = NodeClasz.CLASZ;
        this.name = name;
    }
}
