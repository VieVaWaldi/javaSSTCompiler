package compiler.symboltable;

import static compiler.symboltable.ObjektConst.METHOD;
import static java.lang.String.format;

import compiler.helper.SymbolContext;

/**
 * Phase 2.2 symbol table. Used in multiple phases but is created in 2.1.
 * <p>
 * Creates a list for all variables in this sym tables scope.
 * Special Blocks are not needed because variables can only be defined
 * at the beginning of a method or a class.
 * </p>
 * <p>
 * In the @(Parser):
 * Every IDENT will be stored in the @(Symtable) of the corresponding scope.
 * Only for methods their parameters will be stored in the methods objekt as a signature.
 * This makes method overloading possible. When searching for IDENTs methods are treated extra.
 * </p>
 * <p>
 * Every read identifier found in the parser will be received from this table.
 * Throws error when identifier is declared multiple times in the same scope.
 * Throws an error if wanted identifier is not available.
 * </p>
 */
public class Symboltable
{
    private Objekt head; // List of variables in this block

    private Symboltable enclosure; // SymTable above

    private static int tableCounter = 1;

    public Symboltable( Symboltable symbolTableCurrent )
    {
        this.enclosure = symbolTableCurrent;
    }

    /**
     * Inserts an Object into this symbol tables scope.
     * Throws an exception if obj already exists in a given scope.
     */
    public void putObjekt( Objekt obj, SymbolContext context )
    {
        if ( getObjektFromEnclosure( obj ) != null )
        {
            variableError( context, format( "Objekt %s has already been defined", obj.getName() ) );
        }

        if ( head == null )
        {
            head = obj;
            return;
        }

        Objekt currObjekt = head;
        while ( currObjekt.getNextObj() != null )
        {
            currObjekt = currObjekt.getNextObj();
        }
        currObjekt.setNextObj( obj );
    }

    /**
     * Retrieves an Object from the symbol table data structure, can look
     * in the tables above but not below.
     * Throws an exception if obj does not exist this upwards scope.
     */
    public Objekt getObject( String name, SymbolContext context )
    {
        Objekt retObjekt = getObjektFromEnclosure( new Objekt( name ) );

        if ( retObjekt != null )
        {
            return retObjekt;
        }
        else
        {
            variableError( context, format( "Variable or method %s has not been defined", name ) );
            return null;
        }
    }

    /**
     * Same as below but takes the Objekt instead of, for method calls!
     */
    public Objekt getObject( Objekt obj, SymbolContext context )
    {
        Objekt retObjekt = getObjektFromEnclosure( obj );

        if ( retObjekt != null )
        {
            return retObjekt;
        }
        else
        {
            variableError( context,
                            format( "Method %s has not been defined. Maybe you used the wrong number of parameters.",
                                            obj.getName() ) );
            return null;
        }
    }

    /**
     * Searches for a given Objekt signature in the complete upwards enclosure and returns the
     * corresponding Objekt if found.
     * Usually only the name is needed, but for methods the parameters and returnType have to match.
     */
    private Objekt getObjektFromEnclosure( Objekt obj )
    {
        /* 1. Search in own scope */
        Objekt currObjekt = head;
        while ( currObjekt != null )
        {
            /* 1.1 Obj fits name */
            if ( currObjekt.nameEquals( obj.getName() ) )
            {
                if ( currObjekt.objClazEquals( METHOD ) && obj.objClazEquals( METHOD ) )
                {
                    if ( currObjekt.methodSignatureIsEqualTo( obj ) )
                    {
                        return currObjekt;
                    }
                }
                else
                {
                    return currObjekt; // ToDo this also is the return for lazy method get for now
                }
            }
            currObjekt = currObjekt.getNextObj();
        }

        // 2. If upwards scope exists search there, return false if no upwards enclosure.
        if ( enclosure != null )
        {
            return enclosure.getObjektFromEnclosure( obj );
        }

        return null;
    }

    private void variableError( SymbolContext context, String em )
    {
        if ( context != null )
        {
            System.out.printf( "$ VariableError at Line %s:%s with %s: \"%s\".\n$ %s%n", context.getLine(),
                            context.getColumn(), context.getSym(), context.getValue(), em );
        }
        else
        {
            System.out.printf( "$ VariableError, due to an error i cant tell you where :( \n$ %s%n", em );
        }
        System.exit( 1 );
    }

    public void debugSymTable()
    {
        System.out.println( "\n=== Symboltable ===" );
        debugSymTable( 0 );
    }

    private void debugSymTable( int tab )
    {
        Objekt currObjekt = head;
        StringBuilder tabs = new StringBuilder();
        for ( int i = 0; i < tab; i++ )
        {
            tabs.append( "   " );
        }

        System.out.println( tabs + "#" + tableCounter + ". Tabelle (" );
        tableCounter++;

        while ( currObjekt != null )
        {
            System.out.println( tabs + currObjekt.toString() );

            if ( currObjekt.hasSymTable() )
            {
                currObjekt.getSymTable().debugSymTable( tab + 1 );
            }
            currObjekt = currObjekt.getNextObj();
        }
        System.out.println( tabs + ")\n" );
    }

    public Symboltable getEnclosure()
    {
        return enclosure;
    }
}
