package compiler.abstractsyntaxtree.nodes;

import static java.lang.String.format;

import compiler.abstractsyntaxtree.INode;
import compiler.abstractsyntaxtree.NodeClasz;
import compiler.helper.SymbolContext;
import compiler.symboltable.Objekt;

public class ReturnNode
                extends INode
{
    private static int DOT_COUNTER = 0;

    private Objekt relatedMethodObj;

    public ReturnNode( INode expr, SymbolContext con, Objekt relatedMethodObj )
    {
        this.nodeClasz = NodeClasz.RETURN;
        this.name = "RETURN";
        this.dotName = name + "_" + DOT_COUNTER++;

        this.left = expr;

        this.context = con;
        this.expected = null;
        this.relatedMethodObj = relatedMethodObj;
    }

    @Override public void checkExpected()
    {
        if ( this.expected == null )
        {
            this.expected = relatedMethodObj.getReturnType();
        }

        if ( this.left != null )
        {
            if ( this.left.getType( expected ) != expected )
            {
                typeError( format( "Return type must be %s", expected ) );
            }
        }
    }

    @Override public void checkRules()
    {
        checkUnassignedVarUsage( this.left );
    }

}
