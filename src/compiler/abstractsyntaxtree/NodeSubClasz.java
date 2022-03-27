package compiler.abstractsyntaxtree;

public enum NodeSubClasz
{
    // === ASSIGN ================= //
    // DONE Expects int
    // DONE Rule: if l == const && l.hasVal -> err
    ASSIGNMENT,

    // === COMPARE ================= //
    // DONE CompareNode expects int
    EQUALS,
    LESSTHAN,
    LESSEQUALSTHAN,
    GREATERTHAN,
    GREATEREQUALSTHAN,

    // === FACTOR && TERM ========== //
    // DONE Expects int
    TIMES,
    DIV,
    PLUS,
    BO_MINUS

}
