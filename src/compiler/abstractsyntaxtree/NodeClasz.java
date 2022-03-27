package compiler.abstractsyntaxtree;

public enum NodeClasz
{
    // @formatter:off
    CLASZ,      // Expects nothing
    CONSTANT,   // Expects nothing
    CLASS_VAR,  // Expects nothing
    METHOD,     // DONE Expects all linked parameters to be int
    PARA,       // Expects nothing
    METHOD_VAR, // Expects nothing

    BINOPS,     // DONE see in Node.SubClasz

    IFELSE,     // Expects nothing
    IF,         // DONE Expects l = bool
    WHILE,      // DONE Expects l = bool
    RETURN      // Expects x = ret.obj.getST.getTypeOfMethod()
    // @formatter:on
}
