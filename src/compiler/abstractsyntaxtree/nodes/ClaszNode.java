package compiler.abstractsyntaxtree.nodes;

import compiler.abstractsyntaxtree.INode;
import compiler.abstractsyntaxtree.NodeClasz;

public class ClaszNode
                extends INode
{
    public ClaszNode( String name )
    {
        this.nodeClasz = NodeClasz.CLASZ;
        this.name = name;
    }
}
