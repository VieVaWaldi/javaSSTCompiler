package compiler.abstractsyntaxtree.nodes;

import compiler.abstractsyntaxtree.Node;
import compiler.abstractsyntaxtree.NodeClasz;
import compiler.abstractsyntaxtree.NodeSubClasz;
import compiler.scanner.SymConst;
import compiler.symboltable.Type;

public class FactorNode
                extends Node
{
    private static int DOT_COUNTER = 0;

    /* How to distinguish the different compare operators */
    public FactorNode( SymConst sym, Node left, Node right )
    {
        this.nodeClasz = NodeClasz.BINOPS;
        this.dotName = sym.toString() + DOT_COUNTER++;

        switch ( sym )
        {
            case TIMES -> {
                this.name = "*";
                this.nodeSubClasz = NodeSubClasz.TIMES;
            }
            case QUOT -> {
                this.name = "/";
                this.nodeSubClasz = NodeSubClasz.DIV;
            }
            default -> error( "FactorNode got wrong SymConst" );

        }
        this.type = Type.Int;

        this.left = left;
        this.right = right;
    }

}
