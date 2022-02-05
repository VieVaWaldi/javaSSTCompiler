import java.io.EOFException;

import compiler.abstractsyntaxtree.DotASTCreator;
import compiler.abstractsyntaxtree.Node;
import compiler.parser.Parser;
import compiler.scanner.Input;
import compiler.scanner.Scanner;

public class Main
{
    final static String source_1 = "src/compiler_tests/test_files/complicated_file.jst";

    final static String source_2 = "src/compiler_tests/test_files/real_file.jst";

    public static void main( String[] args )
    {
        // testRun_Scanner();
        testRun_Parser();
    }

    private static void testRun_Parser()
    {
        Parser parser = new Parser( new Scanner( new Input( source_2 ) ) );

        Node root = parser.Class();

        DotASTCreator dotASTCreator = new DotASTCreator( "./ast.dot" );
        dotASTCreator.createDotTree( root );
    }

    private static void testRun_Scanner()
    {
        Scanner scanner = new Scanner( new Input( source_1 ) );

        try
        {
            while ( true )
            {
                scanner.getSym();
                if ( scanner.context != null )
                {
                    System.out.println( scanner.context );
                }
            }
        }
        catch ( EOFException e )
        {
            System.out.print( "End of file." );
        }
    }
}
