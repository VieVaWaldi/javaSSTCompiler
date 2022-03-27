package compiler.abstractsyntaxtree.nodes;

import static compiler.symboltable.Type.Int;

import compiler.abstractsyntaxtree.INode;
import compiler.abstractsyntaxtree.NodeClasz;
import compiler.helper.SymbolContext;
import compiler.symboltable.Objekt;

public class MethodVarNode
                extends INode
{
    private static int DOT_COUNTER = 0;

    public MethodVarNode( String name, Objekt obj, SymbolContext con )
    {
        this.nodeClasz = NodeClasz.METHOD_VAR;
        this.type = Int;

        this.name = name;
        this.dotName = name + "_" + DOT_COUNTER++;

        this.obj = obj;
        this.context = con;
    }
}
