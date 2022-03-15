package compiler.scanner;

/**
 * Enum that represents all possible symbols (tokens) of JavaSST.
 */
public enum SymConst
{
    IDENT, NUMBER, CLASS, PUBLIC, FINAL, VOID, INT,

    RETURN, IF, ELSE, WHILE,

    LPAREN, RPAREN, LBRACK, RBRACK, EQUAL,

    SEMICO, COMMA,

    PLUS, MINUS, TIMES, QUOT,

    EQUALS, LTHAN, GTHAN, LTHANOR, GTHANOR,

    OTHER
}

/**
 * Following should be all possible termination symbols
 * ident
 * class
 * public
 * final
 * void
 * int
 * return
 * if
 * else
 * while
 * (
 * )
 * {
 * }
 * =
 * ;
 * ,
 * +
 * -
 * *
 * /
 * ==
 * <
 * >
 * <=
 * >=
 * 0 | ...| 9
 * a | … | z | A| … |Z
 */


