package compiler.abstractsyntaxtree.nodes;

import static compiler.scanner.SymConst.INT;
import static compiler.scanner.SymConst.VOID;
import static compiler.symboltable.Type.Int;
import static compiler.symboltable.Type.Void;

import compiler.abstractsyntaxtree.INode;
import compiler.abstractsyntaxtree.NodeClasz;
import compiler.helper.SymbolContext;
import compiler.scanner.SymConst;
import compiler.symboltable.Objekt;

public class MethodNode
                extends INode
{
    private static int DOT_COUNTER = 0;

    public MethodNode( String name, SymConst returnType, Objekt obj, SymbolContext con )
    {
        this.nodeClasz = NodeClasz.METHOD;
        this.name = name;
        this.dotName = name + "_" + DOT_COUNTER++;

        if ( returnType == INT )
            this.type = Int;
        else if ( returnType == VOID )
            this.type = Void;
        else
            error( "Method got wrong return type, must be int or void." );

        this.obj = obj;
        this.context = con;
    }

    @Override public void checkRules()
    {
        if ( this.type == Void )
        {
            return;
        }

        INode currNode = this.left;
        while ( currNode.getLink() != null )
        {
            currNode = currNode.getLink();
        }

        if ( currNode instanceof ReturnNode )
        {
            return;
        }
        else if ( currNode instanceof WhileNode ) // weil kein durchlauf
        {
            // one right, last link must return
            typeError( "Not all paths return." );
        }
        else if ( currNode instanceof IfElseNode )
        {
            // if and else must have return as last statement
            INode ifNode = currNode.getRight();
            while ( ifNode.getLink() != null )
            {
                ifNode = ifNode.getLink();
            }
            if ( !( ifNode instanceof ReturnNode ) )
            {
                typeError( "Not all paths return." );
            }

            INode elseNode = currNode.getLeft();
            while ( elseNode.getLink() != null )
            {
                elseNode = elseNode.getLink();
            }
            if ( !( elseNode instanceof ReturnNode ) )
            {
                typeError( "Not all paths return." );
            }

        }
        else if ( currNode instanceof AssignmentNode )
        {
            typeError( "Not all paths return." );
        }
        else if ( currNode instanceof MethodCallNode )
        {
            typeError( "Not all paths return." );
        }

    }
}
