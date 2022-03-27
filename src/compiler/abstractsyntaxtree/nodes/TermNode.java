package compiler.abstractsyntaxtree.nodes;

import static compiler.symboltable.Type.Int;

import compiler.abstractsyntaxtree.INode;
import compiler.abstractsyntaxtree.NodeClasz;
import compiler.abstractsyntaxtree.NodeSubClasz;
import compiler.helper.SymbolContext;
import compiler.scanner.SymConst;
import compiler.symboltable.Type;

public class TermNode
                extends INode
{
    private static int DOT_COUNTER = 0;

    /* How to distinguish the different compare operators */
    public TermNode( SymConst sym, INode left, INode right, SymbolContext con )
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

        this.expected = Int;
        this.context = con;
    }

    public void checkExpected()
    {
        if ( this.left != null )
        {
            if ( this.left.getType( expected ) != expected )
            {
                typeError( "Left Term must be int" );
            }
        }
        if ( this.right != null )
        {
            if ( this.right.getType( expected ) != expected )
            {
                typeError( "Right Term must be int" );
            }
        }
    }
}
