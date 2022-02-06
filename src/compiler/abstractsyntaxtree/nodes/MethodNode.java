package compiler.abstractsyntaxtree.nodes;

import static compiler.scanner.SymConst.INT;
import static compiler.scanner.SymConst.VOID;
import static compiler.symboltable.Type.Int;
import static compiler.symboltable.Type.Void;

import compiler.abstractsyntaxtree.Node;
import compiler.abstractsyntaxtree.NodeClasz;
import compiler.scanner.SymConst;
import compiler.symboltable.Objekt;

public class MethodNode
                extends Node
{
    private static int DOT_COUNTER = 0;

    public MethodNode( String name, SymConst returnType, Objekt obj )
    {
        this.nodeClasz = NodeClasz.METHOD;
        this.name = name + "_" + DOT_COUNTER++;

        if ( returnType == INT )
            this.type = Int;
        else if ( returnType == VOID )
            this.type = Void;
        else
            System.out.println( "ERROR" );

        this.obj = obj;
    }
}
