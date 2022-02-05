package compiler.abstractsyntaxtree;

import compiler.abstractsyntaxtree.nodes.ConstantNode;
import compiler.symboltable.Objekt;
import compiler.symboltable.Type;

public abstract class Node
{
    protected String name; // ? can this be done nicer

    protected Node left;

    protected Node right;

    protected Node link; // Next Sequence in code

    protected NodeClasz nodeClasz; // Types of Node

    protected NodeSubClasz nodeSubClasz; // SubType des Node Types

    protected Type type; // Type in the programming language

    /* this 2 are specific */

    protected Objekt obj;

    protected Long constant;

    /* wip */
    public void traverse()
    {

    }

    public void appendLeft( Node node )
    {
        this.left = node;
    }

    public void appendRight( Node node )
    {
        this.right = node;
    }

    public void appendLR( Node l, Node r )
    {
        this.left = l;
        this.right = r;
    }

    public void appendLink( Node node )
    {
        if (this.link == null) {
            this.link = node;
        }
        else {
            this.link.appendLink( node );
        }
    }

    public Node getLeft()
    {
        return this.left;
    }

    public Node getRight()
    {
        return this.right;
    }

    public Node getLink()
    {
        return this.link;
    }
}
