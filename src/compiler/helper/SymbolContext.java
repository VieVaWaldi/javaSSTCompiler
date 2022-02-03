package compiler.helper;

import compiler.scanner.SymConst;

/**
 * Contains information about a symbols (token) line and column.
 * Is used to be accessed from the outside.
 */
public class SymbolContext
{
    private SymConst sym;

    private String value;

    private int line;

    private int column;

    public SymbolContext( SymConst sym, String value, int line, int column )
    {
        this.sym = sym;
        this.value = value;
        this.line = line;
        this.column = column;
    }

    public String toString()
    {
        return String.format( "%-10s %-12s %3d:%-3d", sym.toString(), value, line, column );
    }

    public SymConst getSym()
    {
        return sym;
    }

    public String getValue()
    {
        return value;
    }

    public Object getLine()
    {
        return line;
    }

    public Object getColumn()
    {
        return column;
    }
}
