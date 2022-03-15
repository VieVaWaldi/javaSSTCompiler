package compiler.scanner;

import java.io.EOFException;

import compiler.helper.SymbolContext;

/**
 * 1. Phase: Responsible for the lexical analysis. Generates symbols (tokens) from
 * the character provided by @(Input). Keeps context for the symbols
 * in regard to the line and column in the source code.
 * </br>
 * Cleans the source code of all unnecessary characters like space, tab,
 * control characters etc. And throws an error on unexpected states.
 * </br>
 * Is used by the parser that will check the syntax of the generated
 * symbol sequence.
 */
public class Scanner
{
    public SymbolContext context;

    final private Input input;

    private char lastCharacter;

    private String lastNumber = "";

    private String lastIdentifier = "";

    private int LAST_LINE;

    private int LAST_COLUMN;

    private boolean alreadyHasNextSym = false;

    public Scanner( Input in )
    {
        input = in;
    }

    /**
     * Takes the next n characters from Input and translates them into tokens.
     * If something was parsed already parse the next char.
     * Updates the current @(symContext).
     */
    public void getSym()
                    throws EOFException
    {
        /* 1. Get first char */
        if ( !alreadyHasNextSym )
            lastCharacter = input.next();

        /* 1.1 Remove ctrl characters */
        while ( lastCharacter <= ' ' )
            lastCharacter = input.next();

        LAST_LINE = input.getCurrLine();
        LAST_COLUMN = input.getCurrCol();

        if ( tryParseNumber() )
        {
            alreadyHasNextSym = true;
            return;
        }

        if ( tryParseIdentifier() )
        {
            alreadyHasNextSym = true;
            return;
        }

        tryParseSpecialCharacter();
    }

    /**
     * 2. Build a number
     */
    private boolean tryParseNumber()
                    throws EOFException
    {
        if ( Character.isDigit( lastCharacter ) )
        {
            do
            {
                lastNumber += lastCharacter;
                lastCharacter = input.next();
            }
            while ( Character.isDigit( lastCharacter ) );

            setSymbolContext( SymConst.NUMBER, lastNumber );
            lastNumber = "";
            return true;
        }
        return false;
    }

    /**
     * 3. If not a number check if its an identifier
     */
    private boolean tryParseIdentifier()
                    throws EOFException
    {
        if ( Character.isAlphabetic( lastCharacter ) )
        {
            do
            {
                lastIdentifier += lastCharacter;
                lastCharacter = input.next();
            }
            while ( Character.isDigit( lastCharacter ) || Character.isAlphabetic( lastCharacter ) );

            switch ( lastIdentifier )
            {
                case "class" -> setSymbolContext( SymConst.CLASS, lastIdentifier );
                case "public" -> setSymbolContext( SymConst.PUBLIC, lastIdentifier );
                case "final" -> setSymbolContext( SymConst.FINAL, lastIdentifier );
                case "void" -> setSymbolContext( SymConst.VOID, lastIdentifier );
                case "int" -> setSymbolContext( SymConst.INT, lastIdentifier );
                case "return" -> setSymbolContext( SymConst.RETURN, lastIdentifier );
                case "if" -> setSymbolContext( SymConst.IF, lastIdentifier );
                case "else" -> setSymbolContext( SymConst.ELSE, lastIdentifier );
                case "while" -> setSymbolContext( SymConst.WHILE, lastIdentifier );
                default -> setSymbolContext( SymConst.IDENT, lastIdentifier );
            }
            lastIdentifier = "";
            return true;
        }
        return false;
    }

    /**
     * 4. Is it a special character?
     */
    private void tryParseSpecialCharacter()
                    throws EOFException
    {
        switch ( lastCharacter )
        {
            case '/' -> {
                tryParseComments();
                return;
            }
            case '(' -> setSymbolContext( SymConst.LPAREN, "(" );
            case ')' -> setSymbolContext( SymConst.RPAREN, ")" );
            case '{' -> setSymbolContext( SymConst.LBRACK, "{" );
            case '}' -> setSymbolContext( SymConst.RBRACK, "}" );
            case ';' -> setSymbolContext( SymConst.SEMICO, ";" );
            case ',' -> setSymbolContext( SymConst.COMMA, "," );
            case '+' -> setSymbolContext( SymConst.PLUS, "+" );
            case '-' -> setSymbolContext( SymConst.MINUS, "-" );
            case '*' -> setSymbolContext( SymConst.TIMES, "*" );
            case '=' -> {
                if ( ( lastCharacter = input.next() ) == '=' )
                    setSymbolContext( SymConst.EQUALS, "==" );
                else
                {
                    setSymbolContext( SymConst.EQUAL, "=" );
                    alreadyHasNextSym = true;
                    return;
                }
            }
            case '<' -> {
                if ( ( lastCharacter = input.next() ) == '=' )
                    setSymbolContext( SymConst.LTHANOR, "<=" );
                else
                {
                    setSymbolContext( SymConst.LTHAN, "<" );
                    alreadyHasNextSym = true;
                    return;
                }
            }
            case '>' -> {
                if ( ( lastCharacter = input.next() ) == '=' )
                    setSymbolContext( SymConst.GTHANOR, ">=" );
                else
                {
                    setSymbolContext( SymConst.GTHAN, ">" );
                    alreadyHasNextSym = true;
                    return;
                }
            }
            default -> setSymbolContext( SymConst.OTHER, "?" );
        }
        alreadyHasNextSym = false;
    }

    /**
     * 5. Is it a comment?
     */
    private void tryParseComments()
                    throws EOFException
    {
        lastCharacter = input.next();

        if ( lastCharacter == '/' )
        {
            while ( ( lastCharacter = input.next() ) != '\n' )
            { /* noop */ }
            alreadyHasNextSym = false;
            this.getSym();
        }
        /* 5.1 Is it a special comment? */
        else if ( lastCharacter == '*' )
        {
            while ( true )
            {
                lastCharacter = input.next();
                if ( lastCharacter == '*' && ( lastCharacter = input.next() ) == '/' )
                {
                    alreadyHasNextSym = false;
                    this.getSym();
                    break;
                }
            }
        }
        /* 5.2 Or the missing special character? */
        else
        {
            setSymbolContext( SymConst.QUOT, "/" );
            alreadyHasNextSym = true;
        }
    }

    private void setSymbolContext( SymConst symConst, String value )
    {
        this.context = new SymbolContext( symConst, value, LAST_LINE, LAST_COLUMN );
    }

}
