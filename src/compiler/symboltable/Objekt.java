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
        /* HACK, ignores returnType for method get returns first found method
         * --> Node should figure out its own type, then get the real object */
        if ( methodObj.returnType != null )
        {
            /* Needed for method put overload */
            if ( this.returnType != methodObj.getReturnType() )
            {
                return false;
            }
        }

        /* Loop over both at the same time */
        Objekt currPara = this.parameterList;
        Objekt compPara = methodObj.parameterList;

        while ( true )
        {
            /* Method paras always have to be Int */
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
            /* Names s stupid right? */
            // if ( !currPara.getName().equals( compPara.getName() ) )
            // {
            //  return false;
            // }

            currPara = currPara.getNextObj();
            compPara = compPara.getNextObj();
        }
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

    public void setValue( long value )
    {
        this.intValue = value;
    }

    public Type getReturnType()
    {
        return this.returnType;
    }

    public String toString()
    {
        StringBuilder str = new StringBuilder();
        str.append( "[" + name );
        str.append( ": " + objClass );

        if ( type != null )
            str.append( ", " + type );

        if ( intValue != null )
            str.append( ", " + intValue );

        if ( returnType != null )
            str.append( ", " + returnType );

        if ( parameterList != null )
        {
            Objekt currObjekt = parameterList;
            while ( currObjekt != null )
            {
                str.append( ", " + currObjekt );
                currObjekt = currObjekt.getNextObj();
            }
        }

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

    public Objekt getParameter()
    {
        return this.parameterList;
    }
}

