package compiler.abstractsyntaxtree.nodes;

import static compiler.symboltable.Type.Bool;

import compiler.abstractsyntaxtree.INode;
import compiler.abstractsyntaxtree.NodeClasz;
import compiler.helper.SymbolContext;

public class IfNode
                extends INode
{
    private static int DOT_COUNTER = 0;

    public IfNode( INode expr, SymbolContext con )
    {
        this.nodeClasz = NodeClasz.IF;
        this.name = "IF";
        this.dotName = name + "_" + DOT_COUNTER++;

        this.left = expr;

        this.expected = Bool;
        this.context = con;
    }

    @Override public void checkExpected()
    {
        if ( this.left != null )
        {
            if ( this.left.getType( expected ) != expected )
            {
                typeError( "Left Factor must be bool" );
            }
        }
    }
}
