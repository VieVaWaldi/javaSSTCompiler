package compiler.abstractsyntaxtree.nodes;

import compiler.abstractsyntaxtree.Node;
import compiler.abstractsyntaxtree.NodeClasz;
import compiler.symboltable.Type;

public class ClassVarNode extends Node
{
    public ClassVarNode( String name, Type type ) {
        this.nodeClasz  = NodeClasz.CLASS_VAR;
        this.name = name;
        this.type = type;
    }

}
