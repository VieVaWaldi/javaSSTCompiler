package compiler.abstractsyntaxtree.nodes;

import static compiler.symboltable.Type.Int;

import compiler.abstractsyntaxtree.Node;
import compiler.abstractsyntaxtree.NodeClasz;
import compiler.abstractsyntaxtree.NodeSubClasz;

public class AssignmentNode
                extends Node
{
    private static int DOT_COUNTER = 0;

    public AssignmentNode( Node var, Node expr )
    {
        this.nodeClasz = NodeClasz.BINOPS;
        this.nodeSubClasz = NodeSubClasz.ASSIGNMENT;
        this.type = Int;

        this.name = "=";
        this.dotName = "ASSIGN_" + DOT_COUNTER++;

        this.left = var;
        this.right = expr;
    }
}
