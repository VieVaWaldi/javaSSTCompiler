package compiler.parser;

import static compiler.parser.SyntaxErrorMsgs.*;
import static compiler.scanner.SymConst.*;
import static compiler.symboltable.ObjektConst.CLASS_VAR;
import static compiler.symboltable.ObjektConst.CLASZ;
import static compiler.symboltable.ObjektConst.CONSTANT;
import static compiler.symboltable.ObjektConst.METHOD;
import static compiler.symboltable.ObjektConst.METHOD_VAR;
import static compiler.symboltable.ObjektConst.PARA;
import static compiler.symboltable.Type.Clasz;
import static compiler.symboltable.Type.Int;

import java.io.EOFException;
import java.util.ArrayList;

import compiler.helper.SymbolContext;
import compiler.scanner.Scanner;
import compiler.scanner.SymConst;
import compiler.symboltable.LazyMethodEvaluator;
import compiler.symboltable.Objekt;
import compiler.symboltable.Symboltable;

/**
 * 2. Phase: Syntaxanalyse.
 * <p>
 * Phase 2.1. Richtige Symbole an richtiger Stelle überprüfen, checkt dadurch ob die Grammatik passt.
 * Hat für jedes NT eine eigene Methode.
 * Methods can perform a lookahead, meaning the next sym is already in memory. And methods can expect
 * one look before, meaning methods expect a look ahead.
 * </p>
 * <p>
 * Phase 2.2. Symboltabelle. See more in @(Symboltable).
 * </p>
 * <p>
 * Phase 2.3. Abstract syntax tree. See more in @(...AST...).
 * </p>
 */
public class Parser
{
    final private Scanner scanner;

    private SymbolContext con;

    private ArrayList<SymbolContext> lastConList = new ArrayList<>();

    private int LAST_CON_IDX = -1;

    private Symboltable symbolTableHead;

    private Symboltable symbolTableCurrent;

    private Objekt lastMethodObj;

    private LazyMethodEvaluator lazyMethodEvaluator;

    private boolean isDebugEnabledParser = true;

    private boolean isDebugEnabledSymTab = true;

    public Parser( Scanner scanner )
    {
        this.scanner = scanner;
        symbolTableHead = new Symboltable( null );
        symbolTableCurrent = symbolTableHead;
    }

    /**
     * Entry point for the parser because every file needs to start with "class".
     */
    public void Class()
    {
        try
        {
            nextSym();
            if ( symEquals( CLASS ) )
            {
                nextSymAndThrowErrIfNotEqual( IDENT, ERR_CLASS_NAME );

                /* SYMTAB SET and SYMTAB BLOCK */
                Symboltable symTabClass = new Symboltable( symbolTableCurrent );
                symbolTableCurrent.putObjekt( new Objekt( con.getValue(), CLASZ, Clasz, symTabClass ), con );
                symbolTableCurrent = symTabClass;
                lazyMethodEvaluator = new LazyMethodEvaluator( symTabClass );

                Classbody();

                lazyMethodEvaluator.startMethodObjektEvaluation();

                if ( isDebugEnabledSymTab )
                    symbolTableHead.debugSymTable();
            }
            else
            {
                syntaxError( ERR_CLASS_START_WITH );
            }
        }
        catch ( Exception EOF )
        {
            syntaxError( ERR_REACHED_EOF );
        }
    }

    /**
     * Expects one look before.
     *
     * @throws EOFException
     */
    private void Classbody()
                    throws EOFException
    {
        nextSymAndThrowErrIfNotEqual( LBRACK, ERR_EXPECTED_LB );
        Declarations();
        throwErrIfSymNotEqual( RBRACK, ERR_EXPECTED_RB );
    }

    /**
     * Performs one look ahead.
     * The three cases can't be mixed.
     */
    private void Declarations()
                    throws EOFException
    {
        nextSym();
        while ( symEquals( FINAL ) )
        {
            nextSymAndThrowErrIfNotEqual( INT, ERR_TYPE_MUST_INT );
            nextSymAndThrowErrIfNotEqual( IDENT, ERR_IDENT_NOT_VALID );

            /* SYMTAB SET --> constant */
            // ADD TO list of constants of objekt class --> Is not needed though, only for method paras i think
            Objekt constantObj = new Objekt( con.getValue(), CONSTANT, Int, null );
            symbolTableCurrent.putObjekt( constantObj, con );

            nextSymAndThrowErrIfNotEqual( EQUAL, ERR_EXPECTED_EQUAL );
            Expression();

            /* SYMTAB SET --> If no expression set value of constant */
            if ( getLastCon( 1 ).getSym() == EQUAL && getLastCon().getSym() == NUMBER )
            {
                constantObj.setValue( getLastCon().getValue() );
            }

            throwErrIfSymNotEqual( SEMICO, ERR_EXPECTED_SEMICO );
            nextSym();
        }

        while ( symEquals( INT ) )
        {
            nextSymAndThrowErrIfNotEqual( IDENT, ERR_IDENT_NOT_VALID );

            /* SYMTAB SET --> class var */
            // ADD TO list of var_class of objekt class --> Is not needed though, only for method paras i think
            symbolTableCurrent.putObjekt( new Objekt( con.getValue(), CLASS_VAR, Int, null ), con );

            nextSymAndThrowErrIfNotEqual( SEMICO, ERR_EXPECTED_SEMICO );
            nextSym();
        }

        while ( symEquals( PUBLIC ) )
        {
            MethodDeclaration();
            nextSym();
        }
    }

    private void MethodDeclaration()
                    throws EOFException
    {
        MethodHead();
        MethodBody();

        /* SYMTAB BLOCK EXIT --> method exit */
        symbolTableCurrent = symbolTableCurrent.getEnclosure();
    }

    /**
     * Assumes PUBLIC has already been checked!
     */
    private void MethodHead()
                    throws EOFException
    {
        MethodType();
        nextSymAndThrowErrIfNotEqual( IDENT, ERR_IDENT_NOT_VALID );

        /* SYMTAB SET and METHOD OVERLOAD --> method void or int type */
        // ADD TO list of methodDef of objekt class --> Is not needed though, only for method paras i think
        Symboltable symTabMethod = new Symboltable( symbolTableCurrent );
        lastMethodObj = new Objekt( con.getValue(), METHOD, null, symTabMethod, getLastCon().getSym() );
        lazyMethodEvaluator.lazyMethodObjektPut( lastMethodObj, con );

        /* SYMTAB BLOCK --> method body always comes after */
        symbolTableCurrent = symTabMethod;

        FormalParameters();
    }

    /**
     * Must be int OR void.
     */
    private void MethodType()
                    throws EOFException
    {
        nextSym();
        if ( !( symEquals( VOID ) || symEquals( INT ) ) )
        {
            syntaxError( ERR_METHOD_TYPE );
        }
    }

    /**
     * Hier brauche ich die erste Vorschau von 2.
     */
    private void FormalParameters()
                    throws EOFException
    {
        nextSymAndThrowErrIfNotEqual( LPAREN, ERR_EXPECTED_LP );
        FpSection();
        throwErrIfSymNotEqual( RPAREN, ERR_EXPECTED_RP );
    }

    /**
     * Performs one look ahead.
     * Recursively checks if is a FpSection.
     */
    private void FpSection()
                    throws EOFException
    {
        nextSym();
        if ( symEquals( INT ) )
        {
            nextSymAndThrowErrIfNotEqual( IDENT, ERR_IDENT_NOT_VALID );

            /* SYMTAB SET and SYMTAB PARA LIST --> first para if not null, must be int */
            symbolTableCurrent.putObjekt( new Objekt( con.getValue(), PARA, Int, null ), con );
            lastMethodObj.appendParaDef( new Objekt( con.getValue(), PARA, Int, null ) );

            nextSym();
            while ( symEquals( COMMA ) )
            {
                nextSymAndThrowErrIfNotEqual( INT, ERR_TYPE_MUST_INT );
                nextSymAndThrowErrIfNotEqual( IDENT, ERR_IDENT_NOT_VALID );

                /* SYMTAB SET and SYMTAB PARA LIST --> n paras if not null, must be int */
                symbolTableCurrent.putObjekt( new Objekt( con.getValue(), PARA, Int, null ), con );
                lastMethodObj.appendParaDef( new Objekt( con.getValue(), PARA, Int, null ) );

                nextSym();
            }
        }
    }

    private void MethodBody()
                    throws EOFException
    {
        nextSymAndThrowErrIfNotEqual( LBRACK, ERR_EXPECTED_LB );
        LocalDeclaration();
        StatementSequence();
        throwErrIfSymNotEqual( RBRACK, ERR_EXPECTED_RB );
    }

    /**
     * Performs one look ahead.
     * Calls itself recursively if a local declaration was found.
     */
    private void LocalDeclaration()
                    throws EOFException
    {
        nextSym();
        while ( symEquals( INT ) )
        {
            nextSymAndThrowErrIfNotEqual( IDENT, ERR_IDENT_NOT_VALID );

            /* SYMTAB SET --> first var def in method */
            symbolTableCurrent.putObjekt( new Objekt( con.getValue(), METHOD_VAR, Int, null ), con );

            nextSymAndThrowErrIfNotEqual( SEMICO, ERR_EXPECTED_SEMICO );
            nextSym();
        }
    }

    /**
     * Expects one look ahead.
     * Performs one look ahead.
     * Expects at least one statement plus infinite more optional ones.
     */
    private void StatementSequence()
                    throws EOFException
    {
        // these are called again with no sym.next in statement
        if ( symEquals( IDENT ) || symEquals( IF ) || symEquals( WHILE ) || symEquals(
                        RETURN ) ) // checks double but i dont know how else to do it
        {
            Statement();
        }
        else
        {
            syntaxError( ERR_NO_STATE );
        }
    }

    /**
     * Expects one look before.
     * Calls itself again on a valid statement --> This kinda makes it my statement sequence
     */
    private void Statement()
                    throws EOFException
    {
        if ( symEquals( IDENT ) )
        {
            /* Can be assignment OR procedure_call -> get next char, store last? */
            nextSym();
            if ( symEquals( EQUAL ) ) /* Must be assignment. */
            {
                /* SYMTAB GET --> assignment of existing var */
                debugParser( " > IDENT VAR ASSIGN: " + getLastCon().getValue() );
                symbolTableCurrent.getObject( getLastCon().getValue(), getLastCon() );

                Assignment();
            }
            else if ( symEquals( LPAREN ) ) /* Must be procedure_call,  assumes ident and lparen were already called! */
            {
                /* SYMTAB GET and LAZY METHOD EVAL --> method (procedure) call */
                debugParser( " > IDENT FUNC CALL NO ASSIGN: " + getLastCon().getValue() );
                lazyMethodEvaluator.lazyMethodObjektGet( getLastCon().getValue(), getLastCon() );

                ProcedureCall();
            }
            else
            {
                syntaxError( ERR_EXPECTED_EQUAL_OR_LB );
            }
            nextSym();
            Statement();
        }
        else if ( symEquals( IF ) )
        {
            If();
            nextSym();
            Statement();
        }
        else if ( symEquals( WHILE ) )
        {
            While();
            nextSym();
            Statement();
        }
        else if ( symEquals( RETURN ) )
        {
            Return();
            nextSym();
            Statement();
        }
        /*If we arrive here the recursive statement sequence is over*/
    }

    /**
     * Assumes IDENT has already been checked! AND
     * Assumes EQUAL has already been checked!
     */
    private void Assignment()
                    throws EOFException
    {
        Expression();
        throwErrIfSymNotEqual( SEMICO, ERR_EXPECTED_SEMICO );
    }

    /**
     * Assumes IDENT has already been checked! AND
     * Assumes LPAREN has already been checked!
     * Because so far only shows up when ident only was the other option.
     */
    private void ProcedureCall()
                    throws EOFException
    {
        InternProcedureCall();
        nextSymAndThrowErrIfNotEqual( SEMICO, ERR_EXPECTED_SEMICO );
    }

    private void InternProcedureCall()
                    throws EOFException
    {
        ActualParameters();
    }

    private void ActualParameters()
                    throws EOFException
    {
        Expression();

        while ( symEquals( COMMA ) )
        {
            Expression();
        }

        throwErrIfSymNotEqual( RPAREN, ERR_EXPECTED_RP );
    }

    /**
     * Assumes IF has already been checked!
     */
    private void If()
                    throws EOFException
    {
        nextSymAndThrowErrIfNotEqual( LPAREN, ERR_EXPECTED_LP );
        Expression();
        throwErrIfSymNotEqual( RPAREN, ERR_EXPECTED_RP );
        nextSymAndThrowErrIfNotEqual( LBRACK, ERR_EXPECTED_LB );

        /* SYMTAB BLOCK --> Cant declare vars in here */

        nextSym();
        StatementSequence();
        throwErrIfSymNotEqual( RBRACK, ERR_EXPECTED_RB );
        nextSymAndThrowErrIfNotEqual( ELSE, ERR_EXPECTED_LB );
        nextSymAndThrowErrIfNotEqual( LBRACK, ERR_EXPECTED_LB );

        /* SYMTAB BLOCK --> Cant declare vars in here */

        nextSym();
        StatementSequence();
        throwErrIfSymNotEqual( RBRACK, ERR_EXPECTED_RB );
    }

    /**
     * Assumes WHILE has already been checked!
     */
    private void While()
                    throws EOFException
    {
        nextSymAndThrowErrIfNotEqual( LPAREN, ERR_EXPECTED_LP );
        Expression();
        throwErrIfSymNotEqual( RPAREN, ERR_EXPECTED_RP );
        nextSymAndThrowErrIfNotEqual( LBRACK, ERR_EXPECTED_LB );

        /* SYMTAB BLOCK --> Cant declare vars in here */

        nextSym();
        StatementSequence();
        throwErrIfSymNotEqual( RBRACK, ERR_EXPECTED_RB );
    }

    /**
     * Assumes RETURN has already been checked!
     */
    private void Return()
                    throws EOFException
    {
        SimpleExpression( true );
        throwErrIfSymNotEqual( SEMICO, ERR_EXPECTED_SEMICO );
    }

    /**
     * Performs one look ahead.
     */
    private void Expression()
                    throws EOFException
    {
        SimpleExpression( false );

        while ( symEquals( EQUALS ) || symEquals( LTHAN ) || symEquals( LTHANOR ) || symEquals( GTHAN ) || symEquals(
                        GTHANOR ) )
        {
            SimpleExpression( false );
        }
    }

    /**
     * Performs one look ahead.
     */
    private void SimpleExpression( boolean optional )
                    throws EOFException
    {
        Term( optional );

        while ( symEquals( PLUS ) || symEquals( MINUS ) )
        {
            Term( false );
        }
    }

    /**
     * Performs one look ahead.
     */
    private void Term( boolean optional )
                    throws EOFException
    {
        Factor( optional );

        while ( symEquals( TIMES ) || symEquals( QUOT ) )
        {
            Factor( false );
        }
    }

    private void Factor( boolean optional )
                    throws EOFException
    {
        nextSym();
        if ( symEquals( IDENT ) )
        {
            nextSym();
            if ( symEquals( LPAREN ) ) /* assumes ident and lparen were already called! */
            {
                /* SYMTAB GET and LAZY METHOD EVAL --> method (procedure) call i think */
                debugParser( " > IDENT FUNC CALL ASSIGN: " + getLastCon().getValue() );
                // Objekt methodCallObj = new Objekt( getLastCon().getValue(), METHOD, null, null, null );
                // SymbolContext methodCallCon = getLastCon(); // save context
                lazyMethodEvaluator.lazyMethodObjektGet( getLastCon().getValue(), getLastCon() );

                InternProcedureCall();

                /* ToDo SYMTAB LAZY METHOD EVAL append parameters */
                // appendParametersToMethodCallObj( methodCallObj );
                // lazyMethodEvaluator.lazyMethodObjektGet( methodCallObj, methodCallCon );

                // Eine Methode hier muss wenn ich sie lazy gette
                    // die richtige anzahl an parametern, aus der ich mit get herausfinde
                    // Naja, sobald du die anz paras hast bekommst du später mit get immer die
                    // richtige methode. Und im späterem type check sollte das ausreichen
                /* Ich will also getMETH nur mit 1. methName und 2. anz paras versorgen */
                /* Gecallte methoden müssen immer return int sein, wir können kein void returnen */

                nextSym();
            }
            else
            {
                /* SYMTAB GET --> simple variable read */
                debugParser( " > IDENT VAR USE: " + getLastCon().getValue() );
                symbolTableCurrent.getObject( getLastCon().getValue(), getLastCon() );
            }
        }
        else if ( symEquals( NUMBER ) )
        {
            // Nothing to do here?
            nextSym();
        }
        else if ( symEquals( LPAREN ) )
        {
            Expression();
            throwErrIfSymNotEqual( RPAREN, ERR_EXPECTED_RP );
            nextSym();
        }
        /* ToDo I entered this else if after the BUG where method calls with 0 paras were faulty. Keep that in mind */
        else if ( symEquals( RPAREN ) )
        {
            // nextSym();
        }
        else if ( !optional )
        {
            syntaxError( ERR_FACTOR_NOT_VALID );
        }
    }

    /**
     * Only legal way to call scanner.getSym()
     */
    private void nextSym()
                    throws EOFException
    {
        lastConList.add( con );
        LAST_CON_IDX++;

        scanner.getSym();
        con = scanner.context;
        if ( isDebugEnabledParser )
        {
            System.out.println( con.toString() );
        }
    }

    private boolean symEquals( final SymConst c )
    {
        return con.getSym() == c;
    }

    private void throwErrIfSymNotEqual( SymConst symConst, SyntaxErrorMsgs em )
    {
        if ( !symEquals( symConst ) )
        {
            syntaxError( em );
        }
    }

    private void nextSymAndThrowErrIfNotEqual( SymConst symConst, SyntaxErrorMsgs em )
                    throws EOFException
    {
        nextSym();
        throwErrIfSymNotEqual( symConst, em );
    }

    /**
     * Counts the number of parameters for method calls.
     * Methods can be parameters
     * Paras must be retrieved inversely
     */
    private void appendParametersToMethodCallObj( Objekt methodCallObj )
    {
        int i = lastConList.size() - 1;
        ArrayList<Objekt> methodCallParaList = new ArrayList<>();

        while ( 0 < i )
        {
            SymbolContext rmme = lastConList.get( i );

            // simple parameter
            if ( lastConList.get( i ).getSym() == LPAREN )
            {
                i--;
                if ( lastConList.get( i ).getSym() == IDENT )
                {
                    // is a method in a method and therefore a para of the next outer method
                    if ( lastConList.get( i - 1 ).getSym() == COMMA || lastConList.get( i - 1 ).getSym() == LPAREN )
                    {
                        // ToDo how do i get the type of this inner method
                        // If have the right para count and can do it lazy
                        methodCallParaList.add( new Objekt( lastConList.get( i ).getValue(), PARA, null, null ) );
                        continue;
                    }
                    // is most outer method
                    else
                    {
                        break;
                    }
                }
                else
                {
                    System.out.println( "ERROR - Sorry" );
                }
            }

            // simple para
            if ( lastConList.get( i ).getSym() == IDENT )
            {
                methodCallParaList.add( new Objekt( lastConList.get( i ).getValue(), PARA, Int, null ) );
            }
            i--;
        }

        // loop reverse through list
        for ( int j = methodCallParaList.size() - 1; j > 0; j-- )
        {
            methodCallObj.appendParaDef( methodCallParaList.get( j ) );
        }
    }

    private void syntaxError( SyntaxErrorMsgs em )
    {
        System.out.println( String.format( "$ SyntaxError at Line %s:%s with problematic symbol %s: \"%s\".\n$ %s",
                        con.getLine(), con.getColumn(), con.getSym(), con.getValue(), em ) );
        System.exit( 1 );
    }

    private SymbolContext getLastCon()
    {
        return getLastCon( 0 );
    }

    private SymbolContext getLastCon( int n )
    {
        return lastConList.get( LAST_CON_IDX - n );
    }

    private void debugParser( String msg )
    {
        if ( isDebugEnabledParser )
        {
            System.out.println( msg );
        }
    }

}
