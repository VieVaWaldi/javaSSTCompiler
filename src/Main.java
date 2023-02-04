import compiler.abstractsyntaxtree.DotASTCreator;
import compiler.abstractsyntaxtree.INode;
import compiler.bytecode.ByteCodeCompiler;
import compiler.parser.Parser;
import compiler.scanner.Input;
import compiler.scanner.Scanner;
import compiler.semanticanalysis.Typeanalysis;

public class Main
{
    final static String source_1 = "src/compiler_tests/test_files/simple_file.jst";

    final static String source_2 = "src/compiler_tests/test_files/real_file.jst";

    final static String source_3 = "src/compiler_tests/test_files/compiler_file.jst";

    public static void main( String[] args )
    {
        Parser parser = new Parser( new Scanner( new Input( source_3 ) ) );

        INode root = parser.Class();

        DotASTCreator dotASTCreator = new DotASTCreator( "./ast.dot", false );
        dotASTCreator.createDotTree( root );

        Typeanalysis ts = new Typeanalysis( root );
        ts.startTypeAnalysis();

        ByteCodeCompiler btc = new ByteCodeCompiler( root );
        btc.compile();
    }
}
