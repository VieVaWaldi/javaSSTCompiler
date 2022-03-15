package compiler.abstractsyntaxtree.nodes;

import compiler.abstractsyntaxtree.Node;
import compiler.abstractsyntaxtree.NodeClasz;
import compiler.abstractsyntaxtree.NodeSubClasz;
import compiler.scanner.SymConst;
import compiler.symboltable.Type;

public class TermNode
                extends Node
{
    private static int DOT_COUNTER = 0;

    /* How to distinguish the different compare operators */
    public TermNode( SymConst sym, Node left, Node right )
    {
        this.nodeClasz = NodeClasz.BINOPS;

        switch ( sym )
        {
            case PLUS -> {
                this.name = "PLUS";
                this.nodeSubClasz = NodeSubClasz.PLUS;
            }
            case MINUS -> {
                this.name = "MINUS";
                this.nodeSubClasz = NodeSubClasz.BO_MINUS;
            }
            default -> error( "TermNode got wrong SymConst" );

        }
        this.dotName = this.name + "_" + DOT_COUNTER++;
        this.type = Type.Int;

        this.left = left;
        this.right = right;
    }
}
