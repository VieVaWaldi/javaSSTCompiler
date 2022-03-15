package compiler.abstractsyntaxtree.nodes;

import compiler.abstractsyntaxtree.Node;
import compiler.abstractsyntaxtree.NodeClasz;
import compiler.abstractsyntaxtree.NodeSubClasz;
import compiler.scanner.SymConst;
import compiler.symboltable.Type;

public class CompareNode
                extends Node
{
    private static int DOT_COUNTER = 0;

    /* How to distinguish the different compare operators */
    public CompareNode( SymConst sym, Node left, Node right )
    {
        this.nodeClasz = NodeClasz.BINOPS;
        this.dotName = sym.toString() + DOT_COUNTER++;

        switch ( sym )
        {
            case EQUALS -> {
                this.name = "==";
                this.nodeSubClasz = NodeSubClasz.EQUALS;
            }
            case LTHAN -> {
                this.name = "<";
                this.nodeSubClasz = NodeSubClasz.LESSTHAN;
            }
            case LTHANOR -> {
                this.name = "<=";
                this.nodeSubClasz = NodeSubClasz.LESSEQUALSTHAN;
            }
            case GTHAN -> {
                this.name = ">";
                this.nodeSubClasz = NodeSubClasz.GREATERTHAN;
            }
            case GTHANOR -> this.name = ">=";
            default -> error( "CompareNode got wrong SymConst" );

        }

        this.type = Type.Int;

        this.left = left;
        this.right = right;
    }
}
