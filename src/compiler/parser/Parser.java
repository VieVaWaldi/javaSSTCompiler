package compiler.parser;

import static compiler.parser.SyntaxErrorMsgs.*;
import static compiler.scanner.SymConst.*;
import static compiler.symboltable.ObjektConst.*;
import static compiler.symboltable.Type.*;

import java.io.EOFException;
import java.util.ArrayList;

import compiler.abstractsyntaxtree.Node;
import compiler.abstractsyntaxtree.nodes.*;
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
 * Phase 2.3. Abstract syntax tree. See more in @(Node).
 * </p>
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
    public Node Class()
    {
        try
        {
            Node claszNode;

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
    private Node Classbody()
                    throws EOFException
    {
        nextSymThrowErrIfNotEqual( LBRACK, ERR_EXPECTED_LB );
        Node classbodyNode = Declarations();
        throwErrIfSymNotEqual( RBRACK, ERR_EXPECTED_RB );

        return classbodyNode;
    }

    /**
     * Performs one look ahead.
     * The three cases can't be mixed.
     */
    private Node Declarations()
                    throws EOFException
    {
        Node declarationNode = null;

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
            AssignmentNode assignmentNode = new AssignmentNode( constNode, Expression() );

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
            /* ToDo Class_Var can be ignored, not needed in tree  */
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
            Node methodeNode = MethodDeclaration();
            nextSym();

            /* NODE LINK */
            declarationNode = linkNewNode( declarationNode, methodeNode );
        }

        return declarationNode;
    }

    private Node MethodDeclaration()
                    throws EOFException
    {
        /* NODE METHOD HEAD AND BODY left */
        Node methodNode = MethodHead();
        /* ToDo Node append left from first para or link to last para. I choose left for now */
        methodNode.appendLeft( MethodBody() );

        /* SYMTAB BLOCK EXIT --> method exit */
        symbolTableCurrent = symbolTableCurrent.getEnclosure();

        return methodNode;
    }

    /**
     * Assumes PUBLIC has already been checked!
     */
    private Node MethodHead()
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
        ParameterNode paraNode = null;

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

        return null; /* NODE We dont need parameters in the ast */
    }

    private Node MethodBody()
                    throws EOFException
    {
        nextSymThrowErrIfNotEqual( LBRACK, ERR_EXPECTED_LB );
        LocalDeclaration();

        /* NODE STATEMENT IN METHOD */
        Node statementNode = StatementSequence();

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
    private Node StatementSequence()
                    throws EOFException
    {
        Node statementNode = null;

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
    private Node Statement()
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
                MethodVarNode methodVarNode = new MethodVarNode( getLastCon().getValue(), methodVarObj );
                AssignmentNode assignmentNode = new AssignmentNode( methodVarNode, Assignment() );

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

                MethodCallNode methodCallNode = new MethodCallNode( getLastCon().getValue(), methodSignature );

                /* PROCEDURE CALL NODE is just a link of expressions, count of para is num of expressions */
                Node procedureCallNode = ProcedureCall();
                Node cleanedProcedureCallNode = addFakeParametersToMethodSignatureAndReturnParaNodes( methodSignature,
                                procedureCallNode );
                methodCallNode.appendLeft( procedureCallNode ); // ToDo take cleaned

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
    private Node Assignment()
                    throws EOFException
    {
        Node expression = Expression();
        throwErrIfSymNotEqual( SEMICO, ERR_EXPECTED_SEMICO );
        return expression;
    }

    /**
     * Assumes IDENT has already been checked! AND
     * Assumes LPAREN has already been checked!
     * Because so far only shows up when ident only was the other option.
     */
    private Node ProcedureCall()
                    throws EOFException
    {
        Node procedureCallNode = InternProcedureCall();
        nextSymThrowErrIfNotEqual( SEMICO, ERR_EXPECTED_SEMICO );
        return procedureCallNode;
    }

    private Node InternProcedureCall()
                    throws EOFException
    {
        return ActualParameters();
    }

    private Node ActualParameters()
                    throws EOFException
    {
        Node expressionNode = Expression();

        while ( symEquals( COMMA ) )
        {
            expressionNode.appendLink( Expression() );
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
        IfNode ifNode = new IfNode( Expression() );

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
        WhileNode whileNode = new WhileNode( Expression() );

        throwErrIfSymNotEqual( RPAREN, ERR_EXPECTED_RP );
        nextSymThrowErrIfNotEqual( LBRACK, ERR_EXPECTED_LB );

        /* SYMTAB BLOCK --> Cant declare vars in here */

        nextSym();
        whileNode.appendLink( StatementSequence() );
        throwErrIfSymNotEqual( RBRACK, ERR_EXPECTED_RB );

        return whileNode;
    }

    /**
     * Assumes RETURN has already been checked!
     */
    private ReturnNode Return()
                    throws EOFException
    {
        ReturnNode returnNode = new ReturnNode( SimpleExpression( true ) );
        throwErrIfSymNotEqual( SEMICO, ERR_EXPECTED_SEMICO );
        return returnNode;
    }

    /**
     * Performs one look ahead.
     */
    private Node Expression()
                    throws EOFException
    {
        Node expressionNode = SimpleExpression( false );
        CompareNode compNode = null;

        while ( symEquals( EQUALS ) || symEquals( LTHAN ) || symEquals( LTHANOR ) || symEquals( GTHAN ) || symEquals(
                        GTHANOR ) )
        {
            /* NODE SIMPLE STATEMENT  */
            compNode = new CompareNode( con.getSym(), expressionNode, SimpleExpression( false ) );
        }

        return returnNewNodeIfNotNull( expressionNode, compNode );
    }

    /**
     * Performs one look ahead.
     */
    private Node SimpleExpression( boolean optional )
                    throws EOFException
    {
        Node simpleExpressionNode = Term( optional );
        TermNode termNode = null;

        while ( symEquals( PLUS ) || symEquals( MINUS ) )
        {
            termNode = new TermNode( con.getSym(), returnNewNodeIfNotNull( simpleExpressionNode, termNode ),
                            Term( false ) );
        }

        return returnNewNodeIfNotNull( simpleExpressionNode, termNode );
    }

    /**
     * Performs one look ahead.
     */
    private Node Term( boolean optional )
                    throws EOFException
    {
        Node termNode = Factor( optional );
        FactorNode factorNode = null;

        while ( symEquals( TIMES ) || symEquals( QUOT ) )
        {
            factorNode = new FactorNode( con.getSym(), termNode, Factor( false ) );
        }

        return returnNewNodeIfNotNull( termNode, factorNode );
    }

    private Node Factor( boolean optional )
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

                MethodCallNode methodCallNode = new MethodCallNode( getLastCon().getValue(), methodSignature );

                /* PROCEDURE CALL NODE is just a link of expressions, count of para is num of expressions */
                Node procedureCallNode = InternProcedureCall();
                Node cleanedParaNode = addFakeParametersToMethodSignatureAndReturnParaNodes( methodSignature,
                                procedureCallNode );
                // ToDo: append paras (like methodCall or expression) of method call in ast, left to methodCallNode
                methodCallNode.appendLeft( procedureCallNode ); // ???

                nextSym();

                lazyMethodEvaluator.lazyMethodObjektGet( methodSignature, methodCallCon );

                return methodCallNode;
            }
            else
            {
                /* SYMTAB GET --> simple variable read */
                debugParser( " > IDENT VAR USE: " + getLastCon().getValue() );
                Objekt methodVarObj = symbolTableCurrent.getObject( getLastCon().getValue(), getLastCon() );

                return new MethodVarNode( getLastCon().getValue(), methodVarObj );
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
            Node expressionNode = Expression();
            throwErrIfSymNotEqual( RPAREN, ERR_EXPECTED_RP );
            nextSym();
            return expressionNode;
        }
        /* with nextSym BUGGY, for methodCalls without parameters as a return */
        else if ( symEquals( RPAREN ) )
        {
            // nextSym();
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
        System.out.printf( "$ SyntaxError at Line %s:%s with problematic symbol %s: \"%s\".\n$ %s%n", con.getLine(),
                        con.getColumn(), con.getSym(), con.getValue(), em );
        System.exit( 1 );
    }

    // ToDo purge loose numbers and vars from this part of the ast
    private Node addFakeParametersToMethodSignatureAndReturnParaNodes( Objekt methodSignature, Node procedureCallNode )
    {
        Node retNode = null;
        Node currentNode = procedureCallNode; // Should be an expression

        while ( currentNode != null )
        {
            /* ToDo */
            /* Variable has already been checked down the ProcedureCall() Stack */
            /* I only need Int and PARA for methodSignature */
            /* If its a void method OR a bool expression, must be checked in type analysis i think */
            methodSignature.appendParaDef( new Objekt( null, PARA, Int, null ) );

            //            if ( !( currentNode instanceof MethodVarNode ) )
            //            {
            //                if ( retNode == null )
            //                {
            //                    retNode = currentNode;
            //                }
            //                else
            //                {
            //                    retNode.appendLink( currentNode );
            //                }
            //            }

            currentNode = currentNode.getLink();
        }
        return retNode;
    }

    /**
     * Counts the number of parameters for method calls.
     * Methods are counted parameters.
     * <p>
     * DOES ONLY WORK for methods and idents as paras.
     * </p>
     */
    /* ToDo add method a para as type PARA_METHOD */
    /* ToDo add expression as para */
    /* ToDo add expression as para as type PARA_EXPRESSION */
    //    private void appendParametersToMethodSignature( Objekt methodSignature )
    //    {
    //        int i = lastConList.size() - 2; // start right from last rParen
    //        ArrayList<Objekt> methodCallParaList = new ArrayList<>();
    //
    //        SymbolContext rmme = lastConList.get( i );
    //
    //        int rParen = 0;
    //        int lParen = 0;
    //
    //        while ( 0 < i )
    //        {
    //            /* Must have reached  */
    //            if ( lastConList.get( i ).getSym().equals( LPAREN ) )
    //            {
    //                i--;
    //                if ( lastConList.get( i ).getSym().equals( IDENT ) )
    //                {
    //                    break;
    //                }
    //            }
    //
    //            /* Method as a parameter */
    //            if ( lastConList.get( i ).getSym().equals( RPAREN ) )
    //            {
    //                i--;
    //                rParen++;
    //                while ( true )
    //                {
    //                    if ( lParen == rParen )
    //                    {
    //                        break;
    //                    }
    //                    if ( lastConList.get( i ).getSym().equals( RPAREN ) )
    //                    {
    //                        rParen++;
    //                    }
    //                    if ( lastConList.get( i ).getSym().equals( LPAREN ) )
    //                    {
    //                        lParen++;
    //                    }
    //                    i--;
    //                }
    //                /* ToDo should this be a method para? */
    //                methodCallParaList.add( new Objekt( lastConList.get( i ).getValue(), PARA, Int, null ) );
    //                i--; // eat ident
    //                continue;
    //            }
    //
    //            /* Simple Para */
    //            if ( lastConList.get( i ).getSym().equals( IDENT ) )
    //            {
    //                methodCallParaList.add( new Objekt( lastConList.get( i ).getValue(), PARA, Int, null ) );
    //            }
    //
    //            /* Simple Para Number */
    //            if ( lastConList.get( i ).getSym().equals( NUMBER ) )
    //            {
    //                methodCallParaList.add( new Objekt( lastConList.get( i ).getValue(), PARA, Int, null ) );
    //            }
    //
    //            /* Skip commas */
    //
    //            i--;
    //        }
    //
    //        // loop reverse through list
    //        i = methodCallParaList.size() - 1;
    //        while ( i > -1 )
    //        {
    //            methodSignature.appendParaDef( methodCallParaList.get( i ) );
    //            i--;
    //        }
    //    }
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

    private Node linkNewNode( Node topNode, Node insertNode )
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

    private Node returnNewNodeIfNotNull( Node firstNode, Node newNode )
    {
        if ( newNode != null )
        {
            return newNode;
        }
        else
        {
            return firstNode;
        }
    }

}
