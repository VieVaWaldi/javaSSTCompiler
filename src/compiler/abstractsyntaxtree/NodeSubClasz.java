package compiler.abstractsyntaxtree;

public enum NodeSubClasz
{
    // === ASSIGN ================= //
    ASSIGNMENT,

    // === COMPARE ================= //
    EQUALS,
    LESSTHAN,
    LESSEQUALSTHAN,
    GREATERTHAN,
    GREATEREQUALSTHAN,

    // === FACTOR ================= //
    TIMES,
    DIV,

    // === TERM ================= //
    PLUS,
    BO_MINUS

}
