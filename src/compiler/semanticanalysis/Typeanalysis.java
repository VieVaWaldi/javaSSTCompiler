package compiler.semanticanalysis;

import compiler.abstractsyntaxtree.INode;

/**
 * Traverses AST with depth search, checks types from leaves to root.
 * Some nodes expect their children to be of a certain type, if they are not,
 * the type analysis throws an error aborting the compilation.
 * This ensures every node makes a promise to the node above, that
 * it received the correct types. In other words, each node checks itself.
 * Nodes can also have Rules that must be checked.
 * IFNode and WHILENode expect a boolean, thus ComparisonNodes must be converted
 * to the fake type boolean.
 * (Methods can return void, so every type error must be checked)
 * <br><br>
 * x Checks if operands have the correct types (eg L + R, L and R must be int).
 * x Checks if method call has right parameter types
 * -> para count is checked, so you can remove number nodes and vars
 * -> All linked paras must be int
 * x Checks if method return type is correct.
 * x Checks if conditions take only boolean values.
 * -> If and While can only have bool ops
 * -> bool ops cant take methods that have void
 * -> Does that check every case?
 * x Checks if constants dont get assigned new values.
 * -> Could only happen left from the assignment node.
 * x Check if all code paths return.
 * x local vars must be init first on every usage except assignment
 * ToDo o Cant call more than one return in method scope
 */
public class Typeanalysis
{
    private final INode ast;

    public Typeanalysis( INode root )
    {
        ast = root;
    }

    public void startTypeAnalysis()
    {
        traverseAst( ast );
    }

    /**
     * Traverses the provided tree with depth search recursively,
     * making each node check itself.
     *
     * @param node root
     */
    private void traverseAst( INode node )
    {
        while ( node != null )
        {
            node.checkExpected();
            node.checkRules();

            if ( node.getLeft() != null )
            {
                traverseAst( node.getLeft() );
            }
            if ( node.getRight() != null )
            {
                traverseAst( node.getRight() );
            }
            node = node.getLink();
        }
    }
}
