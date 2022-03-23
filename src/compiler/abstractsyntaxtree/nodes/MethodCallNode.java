package compiler.abstractsyntaxtree.nodes;

import compiler.abstractsyntaxtree.Node;
import compiler.abstractsyntaxtree.NodeClasz;
import compiler.symboltable.Objekt;
import compiler.symboltable.Type;

public class MethodCallNode
                extends Node
{
    private static int DOT_COUNTER = 0;

    /* Real method obj can only be retrieved after we got the type! */
    public MethodCallNode( String name, Objekt signature )
    {
        this.nodeClasz = NodeClasz.METHOD;

        this.name = name;
        this.dotName = name + "_C_" + DOT_COUNTER++;

        this.obj = signature;
    }

    /* ToDo you shouldnt use the objekts symboltable (which goes down), but the one the object resides in! */
    @Override public Type getType()
    {
        error( "NOT IMPLEMENTED --> METHOD CALL, lazy get return" );
        if ( this.type == null )
        {
            // this.obj.setReturnType();
            // this.obj = obj.getSymTable().getObject( obj ); // is one deeper but can search upward
            // this.type = this.obj.getReturnType();
        }
        return this.type;
    }

}
