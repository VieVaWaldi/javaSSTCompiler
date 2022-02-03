package compiler.symboltable;

import static compiler.scanner.SymConst.INT;
import static compiler.symboltable.ObjektConst.PARA;
import static compiler.symboltable.Type.Int;
import static compiler.symboltable.Type.Void;

import compiler.scanner.SymConst;

/**
 * Part of phase 2.1 symbol table.
 * <p>
 * Every declared identifier will be represented as @(Objekt).
 *
 * @(Objekt)'s can be linked to a list via @(next).
 * </p>
 * <p>
 * @(ObjektConst) defines how this Objekt is treated. As a variable, method, class or block.
 * </p>
 */
public class Objekt
{
    private String name;

    private ObjektConst objClass;

    private Type type;

    private Objekt next;

    /**
     * Constant declarations --> Is a variable
     */
    private Long intValue;

    /**
     * Procedure declarations --> Is a function
     */
    private Objekt parameterList; // List of parameters, same scope as symTab below

    private Type returnType;

    /**
     * Class description --> Is a class
     */
    private Objekt varDef; // List of all instance variables

    private Objekt methodDef; // List of all method declarations

    /**
     * --> Is a block OR a class OR a method which are a block
     */
    private Symboltable symTab; // Is a block

    /***************************************************************************/

    public Objekt( String name )
    {
        this.name = name;
    }

    public Objekt( String name, ObjektConst objClass, Type type, Symboltable symTabDown )
    {
        initObjekt( name, objClass, type, symTabDown, null );
    }

    public Objekt( String name, ObjektConst objClass, Type type, Symboltable symTabDown, SymConst returnType )
    {
        initObjekt( name, objClass, type, symTabDown, returnType );
    }

    private void initObjekt( String name, ObjektConst objClass, Type type, Symboltable symTabDown, SymConst returnType )
    {
        this.name = name;
        this.objClass = objClass;
        if ( type != null )
        {
            this.type = type;
        }
        if ( symTabDown != null )
        {
            this.symTab = symTabDown;
        }
        if ( returnType != null )
        {
            if ( returnType == INT )
                this.returnType = Int;
            else
                this.returnType = Void;
        }
    }

    public void appendParaDef( Objekt methodParaObj )
    {
        if ( methodParaObj.objClazEquals( PARA ) )
        {
            Objekt currPara = this.parameterList;
            if ( currPara == null )
            {
                this.parameterList = methodParaObj;
                return;
            }
            while ( currPara.getNextObj() != null )
            {
                currPara = currPara.getNextObj();
            }
            currPara.setNextObj( methodParaObj );
        }
        else
        {
            error( "ParaList was not given a para but a " + methodParaObj.toString() );
        }
    }

    public boolean methodSignatureIsEqualTo( Objekt methodObj )
    {
        if ( this.returnType != methodObj.getReturnType() )
        {
            return false;
        }

        /* Loop over both at the same time --> ToDo sorry that this looks so complicated */
        Objekt currPara = this.parameterList;
        Objekt compPara = methodObj.parameterList;

        while ( true )
        {
            if ( currPara == null && compPara == null )
            {
                return true;
            }
            if ( currPara != null && compPara == null )
            {
                return false;
            }
            if ( currPara == null && compPara != null )
            {
                return false;
            }
            if ( !currPara.getName().equals( compPara.getName() ) )
            {
                return false;
            }
            if ( currPara.getReturnType() != compPara.getReturnType() )
            {
                return false;
            }
            currPara = currPara.getNextObj();
            compPara = compPara.getNextObj();
        }

        //        while ( currPara != null && compPara != null )
        //        {
        //            if ( currPara.getName() != compPara.getName() )
        //            {
        //                return false;
        //            }
        //            if ( currPara.getType() != compPara.getType() )
        //            {
        //                return false;
        //            }
        //            currPara = currPara.getNextObj();
        //            compPara = compPara.getNextObj();
        //        }
        //
        //        return true;
    }

    /***************************************************************************/

    public String getName()
    {
        return name;
    }

    public boolean nameEquals( String name )
    {
        return this.name.equals( name );
    }

    public Objekt getNextObj()
    {
        return next;
    }

    public void setNextObj( Objekt obj )
    {
        this.next = obj;
    }

    public ObjektConst getObjClasz()
    {
        return this.objClass;
    }

    public boolean objClazEquals( ObjektConst objConst )
    {
        if ( this.objClass == null ) /* ToDo dangerous */
            return false;
        return this.objClass.equals( objConst );
    }

    public boolean hasSymTable()
    {
        return symTab != null;
    }

    public Symboltable getSymTable()
    {
        return symTab;
    }

    public void setValue( String value )
    {
        this.intValue = Long.parseLong( value );
    }

    private Type getType()
    {
        return this.type;
    }

    public Type getReturnType()
    {
        return this.returnType;
    }

    public Objekt getParameterList()
    {
        return this.parameterList;
    }

    public String toString()
    {
        StringBuilder str = new StringBuilder();
        str.append( "[" + name );
        str.append( ": C " + objClass );

        if ( type != null )
            str.append( ", T " + type );

        if ( intValue != null )
            str.append( ", V " + intValue );

        if ( parameterList != null )
        {
            Objekt currObjekt = parameterList;
            while ( currObjekt != null )
            {
                str.append( ", P " + currObjekt );
                currObjekt = currObjekt.getNextObj();
            }
        }

        if ( returnType != null )
            str.append( ", R " + returnType );

        // varDef
        // methodDef

        str.append( "]" );

        return str.toString();
    }

    private void error( String msg )
    {
        System.out.println( msg );
        System.exit( 1 );
    }

}

