package compiler.abstractsyntaxtree.nodes;

import static compiler.symboltable.Type.Int;

import compiler.abstractsyntaxtree.INode;
import compiler.abstractsyntaxtree.NodeClasz;
import compiler.helper.SymbolContext;
import compiler.symboltable.Objekt;
import compiler.symboltable.Type;

public class MethodCallNode
                extends INode
{
    private static int DOT_COUNTER = 0;

    /**
     * Objekt in here works as a signature to retrieve the real method from symbol table,
     * once a parent node expects this signature call to be of a certain type.
     * Then getType() will look for a methodObjekt with the right number of parameters,
     * as well as expected return type.
     */
    public MethodCallNode( String name, Objekt signature, SymbolContext con )
    {
        this.nodeClasz = NodeClasz.METHOD;

        this.name = name;
        this.dotName = name + "_C_" + DOT_COUNTER++;

        this.obj = signature;

        this.context = con;
        this.expected = Int;
    }

    @Override public Type getType( Type type )
    {
        if ( this.type == null )
        {
            // Now, this method call must be of expected type
            this.obj.setType( type );
            this.obj.setReturnType( type );

            // This symboltable is always below level of all method puts
            Objekt thisMethodObj = this.obj.getSymTable().getObject( this.obj, this.context );
            this.type = thisMethodObj.getReturnType();
        }
        return this.type;
    }

    // Already checked for right count
    @Override public void checkExpected()
    {
        INode currNode = this.left;

        while ( currNode != null )
        {
            if ( currNode.getType( expected ) != expected )
            {
                typeError( "Parameter must be int" );
            }

            currNode = currNode.getLink();
        }
    }

    @Override public void checkRules()
    {
        INode currNode = this.left;

        while ( currNode != null )
        {
            checkUnassignedVarUsage( currNode );
            currNode = currNode.getLink();
        }
    }
}
