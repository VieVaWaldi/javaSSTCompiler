package compiler.abstractsyntaxtree.nodes;

import static compiler.symboltable.Type.Int;

import compiler.abstractsyntaxtree.INode;
import compiler.abstractsyntaxtree.NodeClasz;
import compiler.abstractsyntaxtree.NodeSubClasz;
import compiler.helper.SymbolContext;
import compiler.scanner.SymConst;
import compiler.symboltable.ObjektConst;
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
            case PLUS ->
            {
                this.name = "+";
                this.dotName = "PLUS_" + DOT_COUNTER++;
                this.nodeSubClasz = NodeSubClasz.PLUS;
            }
            case MINUS ->
            {
                this.name = "-";
                this.dotName = "MINUS_" + DOT_COUNTER++;
                this.nodeSubClasz = NodeSubClasz.BO_MINUS;
            }
            default -> error( "TermNode got wrong SymConst" );

        }
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

    @Override public void checkRules()
    {
        checkUnassignedVarUsage( this.left );
        checkUnassignedVarUsage( this.right );
    }
}
