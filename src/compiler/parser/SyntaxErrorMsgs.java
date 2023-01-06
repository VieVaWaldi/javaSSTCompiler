package compiler.parser;

/**
 * Enum that represents all possible symbols (tokens) of JavaSST.
 */
public enum SyntaxErrorMsgs
{
    //@formatter:off
    ERR_CLASS_START_WITH( "File must start with \"class\" identifier." ),
    ERR_CLASS_NAME( "Class needs a valid identifier." ),

    ERR_METHOD_TYPE( "\"This language only allows type \"int\" or \"void\" as a return type." ),

    ERR_NO_STATE( "Must have at least one statement." ),

    ERR_EXPECTED_EQUAL( "Expected equals sign \"=\"." ),
    ERR_EXPECTED_EQUAL_OR_LB( "Expected equals sign \"=\" or left parenthesis \"(\"." ),
    ERR_EXPECTED_LB( "Expected left bracket \"{\"." ),
    ERR_EXPECTED_RB( "Expected right bracket \"}\"." ),
    ERR_EXPECTED_LP( "Expected left parenthesis \"(\"." ),
    ERR_EXPECTED_RP( "Expected right parenthesis \")\"." ),
    ERR_EXPECTED_SEMICO( "Expected semicolon \";\"." ),
    ERR_EXPECTED_PUBLIC( "Expected \"public\"." ),

    ERR_FACTOR_NOT_VALID( "This is not a valid factor." ),
    ERR_IDENT_NOT_VALID( "This is not a valid identifier." ),

    ERR_TYPE_MUST_INT( "This language only allows type \"int\"." ),

    ERR_REACHED_EOF( "Wrong placement of brackets, reached end of file." ),

    ERR_EXP_MIGHT_PUT_LOCAL_NOT_ALLOWED(" You might be trying to use local declarations after statement sequences which is illegal in this language.");
    //@formatter:on

    private final String msg;

    SyntaxErrorMsgs( final String msg )
    {
        this.msg = msg;
    }

    @Override public String toString()
    {
        return msg;
    }

}
