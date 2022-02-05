package compiler.abstractsyntaxtree.nodes;

import compiler.abstractsyntaxtree.Node;
import compiler.abstractsyntaxtree.NodeClasz;
import compiler.symboltable.Objekt;
import compiler.symboltable.Type;

public class ConstantNode
                extends Node
{

    public ConstantNode( String name, Type type )
    {
        this.nodeClasz = NodeClasz.CONSTANT;
        this.name = name;
        this.type = type;
    }

    /* Cant be assigned if value is an expression*/
    public void setConstantValue( long value )
    {
        this.constant = value;
    }

    public void setObjekt( Objekt obj )
    {
        this.obj = obj;
    }

}
