package compiler.abstractsyntaxtree.nodes;

import static compiler.symboltable.Type.Int;

import compiler.abstractsyntaxtree.Node;
import compiler.abstractsyntaxtree.NodeClasz;
import compiler.symboltable.Objekt;

public class MethodVarNode
                extends Node
{
    private static int DOT_COUNTER = 0;

    public MethodVarNode( String name, Objekt obj )
    {
        this.nodeClasz = NodeClasz.CLASS_VAR;
        this.type = Int;

        this.name = name + "_" + DOT_COUNTER++;
        this.obj = obj;
    }

}
