package compiler.abstractsyntaxtree.nodes;

import static compiler.symboltable.Type.Int;

import compiler.abstractsyntaxtree.INode;
import compiler.abstractsyntaxtree.NodeClasz;
import compiler.abstractsyntaxtree.NodeSubClasz;
import compiler.helper.SymbolContext;
import compiler.scanner.SymConst;
import compiler.symboltable.Type;

public class FactorNode
                extends INode
{
    private static int DOT_COUNTER = 0;

    private Type expected = Int;

    public FactorNode( SymConst sym, INode left, INode right, SymbolContext con )
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
        this.type = Int;

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
                typeError( "Left Factor must be int" );
            }
        }
        if ( this.right != null )
        {
            if ( this.right.getType( expected ) != expected )
            {
                typeError( "Right Factor must be int" );
            }
        }
    }
}
