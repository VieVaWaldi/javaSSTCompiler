package compiler.abstractsyntaxtree.nodes;

import compiler.abstractsyntaxtree.INode;
import compiler.abstractsyntaxtree.NodeClasz;
import compiler.symboltable.Objekt;
import compiler.symboltable.Type;

public class ConstantNode
                extends INode
{

    public ConstantNode( String name, Type type, Objekt obj )
    {
        this.nodeClasz = NodeClasz.CONSTANT;
        this.name = name;
        this.type = type;
        this.obj = obj;
    }

    /* Cant be assigned if value is an expression*/
    public void setConstantValue( long value )
    {
        this.constant = value;
    }

}
