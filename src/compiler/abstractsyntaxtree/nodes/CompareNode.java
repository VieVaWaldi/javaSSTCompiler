package compiler.abstractsyntaxtree.nodes;

import static compiler.symboltable.ObjektConst.CLASS_VAR;
import static compiler.symboltable.ObjektConst.CONSTANT;
import static compiler.symboltable.ObjektConst.METHOD_VAR;
import static compiler.symboltable.Type.Int;

import compiler.abstractsyntaxtree.INode;
import compiler.abstractsyntaxtree.NodeClasz;
import compiler.abstractsyntaxtree.NodeSubClasz;
import compiler.helper.SymbolContext;
import compiler.scanner.SymConst;
import compiler.symboltable.Objekt;
import compiler.symboltable.Type;

public class CompareNode
                extends INode
{
    private static int DOT_COUNTER = 0;

    /* How to distinguish the different compare operators */
    public CompareNode( SymConst sym, INode left, INode right, SymbolContext con )
    {
        this.nodeClasz = NodeClasz.BINOPS;
        this.dotName = sym.toString() + DOT_COUNTER++;

        switch ( sym )
        {
            case EQUALS ->
            {
                this.name = "==";
                this.nodeSubClasz = NodeSubClasz.EQUALS;
            }
            case LTHAN ->
            {
                this.name = "<";
                this.nodeSubClasz = NodeSubClasz.LESSTHAN;
            }
            case LTHANOR ->
            {
                this.name = "<=";
                this.nodeSubClasz = NodeSubClasz.LESSEQUALSTHAN;
            }
            case GTHAN ->
            {
                this.name = ">";
                this.nodeSubClasz = NodeSubClasz.GREATERTHAN;
            }
            case GTHANOR ->
            {
                this.name = ">=";
                this.nodeSubClasz = NodeSubClasz.GREATEREQUALSTHAN;
            }
            default -> error( "CompareNode got wrong SymConst" );

        }

        this.type = Type.Bool;

        this.left = left;
        this.right = right;

        this.context = con;
        this.expected = Int;
    }

    @Override public void checkExpected()
    {
        if ( this.left != null ) // should crash before if happens but whatever
        {
            if ( this.left.getType( expected ) != expected )
            {
                typeError( "Left Node must be int" );
            }
        }
        if ( this.right != null )
        {
            if ( this.right.getType( expected ) != expected )
            {
                typeError( "Right Node must be int" );
            }
        }
    }

    @Override public void checkRules()
    {
        checkUnassignedVarUsage( this.left );
        checkUnassignedVarUsage( this.right );
    }
}
