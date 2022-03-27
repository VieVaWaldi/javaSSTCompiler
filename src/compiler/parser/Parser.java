package compiler.parser;

import static compiler.parser.SyntaxErrorMsgs.*;
import static compiler.scanner.SymConst.*;
import static compiler.symboltable.ObjektConst.*;
import static compiler.symboltable.Type.*;

import java.io.EOFException;
import java.util.ArrayList;

import compiler.abstractsyntaxtree.INode;
import compiler.abstractsyntaxtree.nodes.*;
import compiler.helper.SymbolContext;
import compiler.scanner.Scanner;
import compiler.scanner.SymConst;
import compiler.symboltable.LazyMethodEvaluator;
import compiler.symboltable.Objekt;
import compiler.symboltable.Symboltable;

/**
 * 2. Phase: syntax analysis.
 * Phase 2.1. Richtige Symbole an richtiger Stelle überprüfen, checkt dadurch ob die Grammatik passt.
 * Hat für jedes NT eine eigene Methode.
 * Methods can perform a lookahead, meaning the next sym is already in memory. And methods can expect
 * one look before, meaning methods expect a look ahead.
 * Phase 2.2. Symboltabelle. See more in @(Symboltable).
 * Phase 2.3. Abstract syntax tree. See more in @(Node).
 * Phase 3. Semantic analysis. See more in (@Typeanalysis).
 * Phase 4. Compilation to byte code.
 */
public class Parser
{
    final private Scanner scanner;

    private SymbolContext con;

    private final ArrayList<SymbolContext> lastConList = new ArrayList<>();

    private int LAST_CON_IDX = -1;

    private final Symboltable symbolTableHead;

    private Symboltable symbolTableCurrent;

    private Objekt lastMethodObj;

    private LazyMethodEvaluator lazyMethodEvaluator;

    private final boolean isDebugEnabledParser = true;

    private final boolean isDebugEnabledSymTab = true;

    public Parser( Scanner scanner )
    {
        this.scanner = scanner;
        symbolTableHead = new Symboltable( null );
        symbolTableCurrent = symbolTableHead;
    }

    /**
     * Entry point for the parser because every file needs to start with "class".
     */
    public INode Class()
    {
        try
        {
            INode claszNode;

            nextSym();
            if ( symEquals( CLASS ) )
            {
                nextSymThrowErrIfNotEqual( IDENT, ERR_CLASS_NAME );
                claszNode = new ClaszNode( con.getValue() );

                /* SYMTAB SET and SYMTAB BLOCK */
                Symboltable symTabClass = new Symboltable( symbolTableCurrent );
                symbolTableCurrent.putObjekt( new Objekt( con.getValue(), CLASZ, Clasz, symTabClass ), con );
                symbolTableCurrent = symTabClass;
                lazyMethodEvaluator = new LazyMethodEvaluator( symTabClass );

                claszNode.appendLeft( Classbody() );

                lazyMethodEvaluator.startMethodObjektEvaluation();

                if ( isDebugEnabledSymTab )
                    symbolTableHead.debugSymTable();

                return claszNode;
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

        return null;
    }

    /**
     * Expects one look before.
     */
    private INode Classbody()
                    throws EOFException
    {
        nextSymThrowErrIfNotEqual( LBRACK, ERR_EXPECTED_LB );
        INode classbodyNode = Declarations();
        throwErrIfSymNotEqual( RBRACK, ERR_EXPECTED_RB );

        return classbodyNode;
    }

    /**
     * Performs one look ahead.
     * The three cases can't be mixed.
     */
    private INode Declarations()
                    throws EOFException
    {
        INode declarationNode = null;

        nextSym();
        while ( symEquals( FINAL ) )
        {
            nextSymThrowErrIfNotEqual( INT, ERR_TYPE_MUST_INT );
            nextSymThrowErrIfNotEqual( IDENT, ERR_IDENT_NOT_VALID );

            /* SYMTAB SET --> constant */
            Objekt constantObj = new Objekt( con.getValue(), CONSTANT, Int, null );
            symbolTableCurrent.putObjekt( constantObj, con );

            /* NODE CONSTANT CREATE */
            ConstantNode constNode = new ConstantNode( con.getValue(), Int, constantObj );

            nextSymThrowErrIfNotEqual( EQUAL, ERR_EXPECTED_EQUAL );

            /* NODE ASSIGNMENT --> FROM PARSER */
            AssignmentNode assignmentNode = new AssignmentNode( constNode, Expression( false ), getLastCon() );

            /* SYMTAB SET --> If no expression get and set value of constant */
            if ( getLastCon( 1 ).getSym() == EQUAL && getLastCon().getSym() == NUMBER )
            {
                long value = Long.parseLong( getLastCon().getValue() );
                constantObj.setValue( value );

                /* NODE CONSTANT AND SYMTAB SET */
                constNode.setConstantValue( value );
            }

            throwErrIfSymNotEqual( SEMICO, ERR_EXPECTED_SEMICO );
            nextSym();

            /* NODE LINK, when there was an expression link assignment */
            if ( assignmentNode.getRight() instanceof NumberNode )
            {
                declarationNode = linkNewNode( declarationNode, constNode );
            }
            else
            {
                declarationNode = linkNewNode( declarationNode, assignmentNode );
            }
        }

        while ( symEquals( INT ) )
        {
            nextSymThrowErrIfNotEqual( IDENT, ERR_IDENT_NOT_VALID );

            /* NODE CONSTANT CREATE */
            ClassVarNode classVarNode = new ClassVarNode( con.getValue(), Int );

            /* SYMTAB SET --> class var */
            symbolTableCurrent.putObjekt( new Objekt( con.getValue(), CLASS_VAR, Int, null ), con );

            nextSymThrowErrIfNotEqual( SEMICO, ERR_EXPECTED_SEMICO );
            nextSym();

            /* NODE LINK --> Doesnt have to be in AST, but in ST */
            // declarationNode = linkNewNode( declarationNode, classVarNode );
        }

        while ( symEquals( PUBLIC ) )
        {
            /* NODE METHOD CREATE */
            INode methodeNode = MethodDeclaration();
            nextSym();

            /* NODE LINK */
            declarationNode = linkNewNode( declarationNode, methodeNode );
        }

        return declarationNode;
    }

    private INode MethodDeclaration()
                    throws EOFException
    {
        /* NODE METHOD HEAD AND BODY left */
        INode methodNode = MethodHead();
        methodNode.appendLeft( MethodBody() );

        /* SYMTAB BLOCK EXIT --> method exit */
        symbolTableCurrent = symbolTableCurrent.getEnclosure();

        return methodNode;
    }

    /**
     * Assumes PUBLIC has already been checked!
     */
    private INode MethodHead()
                    throws EOFException
    {
        MethodType();
        nextSymThrowErrIfNotEqual( IDENT, ERR_IDENT_NOT_VALID );

        /* SYMTAB SET and METHOD OVERLOAD --> method void or int type */
        // ADD TO list of methodDef of objekt class --> Is not needed though, only for method paras i think
        Symboltable symTabMethod = new Symboltable( symbolTableCurrent );
        lastMethodObj = new Objekt( con.getValue(), METHOD, null, symTabMethod, getLastCon().getSym() );
        lazyMethodEvaluator.lazyMethodObjektPut( lastMethodObj, con );

        /* SYMTAB BLOCK --> method body always comes after */
        symbolTableCurrent = symTabMethod;

        /* NODE METHOD CREATE */
        MethodNode methodNode = new MethodNode( con.getValue(), getLastCon().getSym(), lastMethodObj );
        methodNode.appendLeft( FormalParameters() );

        return methodNode;
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
    private ParameterNode FormalParameters()
                    throws EOFException
    {
        nextSymThrowErrIfNotEqual( LPAREN, ERR_EXPECTED_LP );
        ParameterNode paraNode = FpSection();
        throwErrIfSymNotEqual( RPAREN, ERR_EXPECTED_RP );
        return paraNode;
    }

    /**
     * Performs one look ahead.
     * Recursively checks if is a FpSection.
     */
    private ParameterNode FpSection()
                    throws EOFException
    {
        ParameterNode paraNode;

        nextSym();
        if ( symEquals( INT ) )
        {
            nextSymThrowErrIfNotEqual( IDENT, ERR_IDENT_NOT_VALID );

            /* SYMTAB SET and SYMTAB PARA LIST --> first para if not null, must be int */
            Objekt paraObj = new Objekt( con.getValue(), PARA, Int, null );
            symbolTableCurrent.putObjekt( paraObj, con );
            lastMethodObj.appendParaDef( new Objekt( con.getValue(), PARA, Int, null ) );

            /* NODE PARAMETER */
            paraNode = new ParameterNode( con.getValue(), paraObj );

            nextSym();
            while ( symEquals( COMMA ) )
            {
                nextSymThrowErrIfNotEqual( INT, ERR_TYPE_MUST_INT );
                nextSymThrowErrIfNotEqual( IDENT, ERR_IDENT_NOT_VALID );

                /* SYMTAB SET and SYMTAB PARA LIST --> n paras if not null, must be int */
                paraObj = new Objekt( con.getValue(), PARA, Int, null );
                symbolTableCurrent.putObjekt( paraObj, con );
                lastMethodObj.appendParaDef( new Objekt( con.getValue(), PARA, Int, null ) );

                /* NODE PARAMETER */
                paraNode.appendLink( new ParameterNode( con.getValue(), paraObj ) );

                nextSym();
            }
        }

        return null; /* NODE We don't need parameters in the ast */
    }

    private INode MethodBody()
                    throws EOFException
    {
        nextSymThrowErrIfNotEqual( LBRACK, ERR_EXPECTED_LB );
        LocalDeclaration();

        /* NODE STATEMENT IN METHOD */
        INode statementNode = StatementSequence();

        throwErrIfSymNotEqual( RBRACK, ERR_EXPECTED_RB );

        return statementNode;
    }

    /**
     * Performs one look ahead.
     * Calls itself recursively if a local declaration was found.
     * <p>
     * Local method variables are not welcome in the AST.
     * </p>
     */
    private void LocalDeclaration()
                    throws EOFException
    {
        nextSym();
        while ( symEquals( INT ) )
        {
            nextSymThrowErrIfNotEqual( IDENT, ERR_IDENT_NOT_VALID );

            /* SYMTAB SET --> first var def in method */
            Objekt classVarObj = new Objekt( con.getValue(), METHOD_VAR, Int, null );
            symbolTableCurrent.putObjekt( classVarObj, con );

            nextSymThrowErrIfNotEqual( SEMICO, ERR_EXPECTED_SEMICO );
            nextSym();
        }
    }

    /**
     * Expects one look ahead.
     * Performs one look ahead.
     * Expects at least one statement plus infinite more optional ones.
     */
    private INode StatementSequence()
                    throws EOFException
    {
        INode statementNode = null;

        // these are called again with no sym.next in statement
        if ( symEquals( IDENT ) || symEquals( IF ) || symEquals( WHILE ) || symEquals(
                        RETURN ) ) // checks double but i dont know how else to do it
        {
            statementNode = Statement();
        }
        else
        {
            syntaxError( ERR_NO_STATE );
        }

        return statementNode;
    }

    /**
     * Expects one look before.
     * Calls itself again on a valid statement --> This kinda makes it my statement sequence
     */
    private INode Statement()
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
                Objekt methodVarObj = symbolTableCurrent.getObject( getLastCon().getValue(), getLastCon() );

                /* NODE VAR ASSIGN */
                MethodVarNode methodVarNode = new MethodVarNode( getLastCon().getValue(), methodVarObj, getLastCon() );
                AssignmentNode assignmentNode = new AssignmentNode( methodVarNode, Assignment(), getLastCon( 1 ) );

                nextSym();
                assignmentNode.appendLink( Statement() );
                return assignmentNode;
            }
            else if ( symEquals( LPAREN ) ) /* Must be procedure_call,  assumes ident and lparen were already called! */
            {
                /* SYMTAB GET and LAZY METHOD EVAL --> method (procedure) call */
                debugParser( " > IDENT SIMPLE CALL: " + getLastCon().getValue() );

                /* Get return type in type analysis by retrieving unique method (see para count) from ST */
                /* Here! We must
                 * 1. Build method signature with right para count
                 * 2. If para is an expression add it to AST */
                Objekt methodSignature = new Objekt( getLastCon().getValue(), METHOD, null, symbolTableCurrent, null );
                SymbolContext methodCallCon = getLastCon();

                MethodCallNode methodCallNode =
                                new MethodCallNode( getLastCon().getValue(), methodSignature, getLastCon() );

                /* PROCEDURE CALL NODE is just a link of expressions, count of para is num of expressions */
                INode procedureCallNode = ProcedureCall();
                addFakeParametersToMethodSignature( methodSignature, procedureCallNode );
                methodCallNode.appendLeft( procedureCallNode );

                lazyMethodEvaluator.lazyMethodObjektGet( methodSignature, methodCallCon );

                nextSym();
                methodCallNode.appendLink( Statement() );
                return methodCallNode;
            }
            else
            {
                syntaxError( ERR_EXPECTED_EQUAL_OR_LB );
            }
        }
        else if ( symEquals( IF ) )
        {
            IfElseNode ifElseNode = If();
            nextSym();
            ifElseNode.appendLink( Statement() );
            return ifElseNode;
        }
        else if ( symEquals( WHILE ) )
        {
            WhileNode whileNode = While();
            nextSym();
            whileNode.appendLink( Statement() );
            return whileNode;
        }
        else if ( symEquals( RETURN ) )
        {
            ReturnNode returnNode = Return();
            nextSym();
            returnNode.appendLink( Statement() );
            return returnNode;
        }

        return null; /*If we arrive here the recursive statement sequence is over*/
    }

    /**
     * Assumes IDENT has already been checked! AND
     * Assumes EQUAL has already been checked!
     */
    private INode Assignment()
                    throws EOFException
    {
        INode expression = Expression( false );
        throwErrIfSymNotEqual( SEMICO, ERR_EXPECTED_SEMICO );
        return expression;
    }

    /**
     * Assumes IDENT has already been checked! AND
     * Assumes LPAREN has already been checked!
     * Because so far only shows up when ident only was the other option.
     */
    private INode ProcedureCall()
                    throws EOFException
    {
        INode procedureCallNode = InternProcedureCall();
        nextSymThrowErrIfNotEqual( SEMICO, ERR_EXPECTED_SEMICO );
        return procedureCallNode;
    }

    private INode InternProcedureCall()
                    throws EOFException
    {
        return ActualParameters();
    }

    private INode ActualParameters()
                    throws EOFException
    {
        INode expressionNode = Expression( true ); // optional true, everywhere else false?

        while ( symEquals( COMMA ) )
        {
            expressionNode.appendLink( Expression( false ) );
        }

        throwErrIfSymNotEqual( RPAREN, ERR_EXPECTED_RP );
        return expressionNode;
    }

    /**
     * Assumes IF has already been checked!
     */
    private IfElseNode If()
                    throws EOFException
    {
        nextSymThrowErrIfNotEqual( LPAREN, ERR_EXPECTED_LP );
        IfNode ifNode = new IfNode( Expression( false ), getLastCon() );

        throwErrIfSymNotEqual( RPAREN, ERR_EXPECTED_RP );
        nextSymThrowErrIfNotEqual( LBRACK, ERR_EXPECTED_LB );

        /* SYMTAB BLOCK --> Cant declare vars in here */

        nextSym();
        ifNode.appendRight( StatementSequence() );
        IfElseNode ifElseNode = new IfElseNode( ifNode );

        throwErrIfSymNotEqual( RBRACK, ERR_EXPECTED_RB );
        nextSymThrowErrIfNotEqual( ELSE, ERR_EXPECTED_LB );
        nextSymThrowErrIfNotEqual( LBRACK, ERR_EXPECTED_LB );

        /* SYMTAB BLOCK --> Cant declare vars in here */

        nextSym();
        ifElseNode.appendRight( StatementSequence() );
        throwErrIfSymNotEqual( RBRACK, ERR_EXPECTED_RB );

        return ifElseNode;
    }

    /**
     * Assumes WHILE has already been checked!
     */
    private WhileNode While()
                    throws EOFException
    {
        nextSymThrowErrIfNotEqual( LPAREN, ERR_EXPECTED_LP );
        WhileNode whileNode = new WhileNode( Expression( false ), getLastCon() );

        throwErrIfSymNotEqual( RPAREN, ERR_EXPECTED_RP );
        nextSymThrowErrIfNotEqual( LBRACK, ERR_EXPECTED_LB );

        /* SYMTAB BLOCK --> Cant declare vars in here */

        nextSym();
        whileNode.appendRight( StatementSequence() );
        throwErrIfSymNotEqual( RBRACK, ERR_EXPECTED_RB );

        return whileNode;
    }

    /**
     * Assumes RETURN has already been checked!
     */
    private ReturnNode Return()
                    throws EOFException
    {
        ReturnNode returnNode = new ReturnNode( SimpleExpression( true ), getLastCon(), lastMethodObj );
        throwErrIfSymNotEqual( SEMICO, ERR_EXPECTED_SEMICO );
        return returnNode;
    }

    /**
     * Performs one look ahead.
     */
    private INode Expression( boolean optional )
                    throws EOFException
    {
        INode expressionNode = SimpleExpression( optional );

        while ( symEquals( EQUALS ) || symEquals( LTHAN ) || symEquals( LTHANOR ) || symEquals( GTHAN ) || symEquals(
                        GTHANOR ) )
        {
            /* NODE SIMPLE STATEMENT  */
            expressionNode = new CompareNode( con.getSym(), expressionNode, SimpleExpression( false ), getLastCon() );
        }

        return expressionNode;
    }

    /**
     * Performs one look ahead.
     */
    private INode SimpleExpression( boolean optional )
                    throws EOFException
    {
        INode simpleExpressionNode = Term( optional );

        while ( symEquals( PLUS ) || symEquals( MINUS ) )
        {
            simpleExpressionNode = new TermNode( con.getSym(), simpleExpressionNode, Term( false ), getLastCon() );
        }

        return simpleExpressionNode;
    }

    /**
     * Performs one look ahead.
     */
    private INode Term( boolean optional )
                    throws EOFException
    {
        INode factorNode = Factor( optional );

        while ( symEquals( TIMES ) || symEquals( QUOT ) )
        {
            factorNode = new FactorNode( con.getSym(), factorNode, Factor( false ), getLastCon() );
        }

        return factorNode;
    }

    private INode Factor( boolean optional )
                    throws EOFException
    {
        nextSym();
        if ( symEquals( IDENT ) )
        {
            nextSym();
            if ( symEquals( LPAREN ) ) /* assumes ident and lparen were already called! */
            {
                /* SYMTAB GET and LAZY METHOD EVAL --> method (procedure) call i think */
                debugParser( " > IDENT FUNC CALL: ASSIGN, RETURN, METHOD AS PARA: " + getLastCon().getValue() );

                /* Get return type in type analysis by retrieving unique method (see para count) from ST*/
                /* Here! We must
                 * 1. Build method signature with right para count
                 * 2. If para is an expression add it to AST */
                Objekt methodSignature = new Objekt( getLastCon().getValue(), METHOD, null, symbolTableCurrent, null );
                SymbolContext methodCallCon = getLastCon();

                MethodCallNode methodCallNode =
                                new MethodCallNode( getLastCon().getValue(), methodSignature, getLastCon() );

                /* PROCEDURE CALL NODE is just a link of expressions, count of para is num of expressions */
                INode procedureCallNode = InternProcedureCall();
                addFakeParametersToMethodSignature( methodSignature, procedureCallNode );
                methodCallNode.appendLeft( procedureCallNode );

                nextSym();

                lazyMethodEvaluator.lazyMethodObjektGet( methodSignature, methodCallCon );

                return methodCallNode;
            }
            else
            {
                /* SYMTAB GET --> simple variable read */
                debugParser( " > IDENT VAR USE: " + getLastCon().getValue() );
                Objekt methodVarObj = symbolTableCurrent.getObject( getLastCon().getValue(), getLastCon() );

                return new MethodVarNode( getLastCon().getValue(), methodVarObj, getLastCon() );
            }
        }
        else if ( symEquals( NUMBER ) )
        {
            // Nothing to do here?
            NumberNode numberNode = new NumberNode( con.getValue() );
            nextSym();
            return numberNode;
        }
        else if ( symEquals( LPAREN ) )
        {
            INode expressionNode = Expression( false );
            throwErrIfSymNotEqual( RPAREN, ERR_EXPECTED_RP );
            nextSym();
            return expressionNode;
        }
        else if ( !optional )
        {
            syntaxError( ERR_FACTOR_NOT_VALID );
        }

        return null;
    }

    // === PARSER =========================================================================== //

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

    private void nextSymThrowErrIfNotEqual( SymConst symConst, SyntaxErrorMsgs em )
                    throws EOFException
    {
        nextSym();
        throwErrIfSymNotEqual( symConst, em );
    }

    private void syntaxError( SyntaxErrorMsgs em )
    {
        System.out.printf( "$ SyntaxError at Line %s:%s with %s: \"%s\".\n$ %s%n", con.getLine(), con.getColumn(),
                        con.getSym(), con.getValue(), em );
        System.exit( 1 );
    }

    /**
     * This method attaches the parameters to the method signature.
     */
    // ToDo purge loose numbers and vars from this part of the ast
    private void addFakeParametersToMethodSignature( Objekt methodSignature, INode procedureCallNode )
    {
        INode currentNode = procedureCallNode; // Should be an expression

        while ( currentNode != null )
        {
            /* Variable has already been checked down the ProcedureCall() Stack */
            /* I only need to check if the para count in here is correct */
            methodSignature.appendParaDef( new Objekt( null, PARA, Int, null ) );

            currentNode = currentNode.getLink();
        }
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

    // === AST ============================================================================== //

    private INode linkNewNode( INode topNode, INode insertNode )
    {
        /* Set empty or link new Node */
        if ( topNode == null )
        {
            topNode = insertNode;
        }
        else
        {
            topNode.appendLink( insertNode );
        }
        return topNode;
    }

}
