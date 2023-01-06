package compiler.abstractsyntaxtree.nodes;

import static compiler.symboltable.ObjektConst.CLASS_VAR;
import static compiler.symboltable.ObjektConst.CONSTANT;
import static compiler.symboltable.ObjektConst.METHOD_VAR;
import static compiler.symboltable.Type.Int;

import compiler.abstractsyntaxtree.INode;
import compiler.abstractsyntaxtree.NodeClasz;
import compiler.abstractsyntaxtree.NodeSubClasz;
import compiler.helper.SymbolContext;
import compiler.symboltable.Objekt;

public class AssignmentNode
                extends INode
{
    private static int DOT_COUNTER = 0;

    public AssignmentNode( INode var, INode expr, SymbolContext con )
    {
        this.nodeClasz = NodeClasz.BINOPS;
        this.nodeSubClasz = NodeSubClasz.ASSIGNMENT;
        this.type = Int;

        this.name = "=";
        this.dotName = "ASSIGN_" + DOT_COUNTER++;

        this.left = var;
        this.right = expr;

        this.expected = Int;
        this.context = con;
    }

    @Override public void checkExpected()
    {
        if ( this.left != null )
        {
            if ( this.left.getType( expected ) != expected )
            {
                typeError( "Left Factor must be int" );
            }
        }
        if ( this.right != null )
        {
            if ( this.right.getType( expected ) != expected )
            {
                typeError( "Right Factor must be int" );
            }
        }
    }

    @Override public void checkRules()
    {
        Objekt varObj = left.getObj();
        if ( varObj.objClazEquals( CONSTANT ) && varObj.hasValue() )
        {
            typeError( "Cant assign value to a constant." );
        }

        // Set was_assigned flag for local variables, so they can be used
        if ( varObj.objClazEquals( CLASS_VAR ) )
        {
            varObj.setWasAssignedValue();
        }
        if ( varObj.objClazEquals( METHOD_VAR ) )
        {
            varObj.setWasAssignedValue();
        }

        checkUnassignedVarUsage( this.right );
    }
}
