package compiler_tests.scanner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.EOFException;

import compiler.scanner.Input;
import org.junit.jupiter.api.Test;

public class _Input
{
    private Input input;

    final private String sourcePathBadFile = "src/compiler_tests/test_files/bad_file.jst";

    final private String sourcePathSimpleFile = "src/compiler_tests/test_files/simple_file.jst";

    @Test public void test_next()
    {
        input = new Input( sourcePathBadFile );

        try
        {
            assertEquals( input.next(), '1' );
            assertEquals( input.next(), '2' );
            assertEquals( input.next(), '3' );
            assertEquals( input.next(), '\n' );
        }
        catch ( EOFException e )
        {
        }

        assertThrows( EOFException.class, () -> input.next() );
    }

    @Test public void test_printWholeFile()
    {
        input = new Input( sourcePathBadFile );
        final String source = input.printWholeFile();
        assertEquals( source, "123\n" );
    }

    @Test public void test_LineAndCol_badFile()
    {
        input = new Input( sourcePathBadFile );

        try
        {
            char c = input.next();
            assertEquals( input.getCurrLine(), 1 );
            assertEquals( input.getCurrCol(), 1 );

            c = input.next();
            assertEquals( input.getCurrLine(), 1 );
            assertEquals( input.getCurrCol(), 2 );

            c = input.next();
            assertEquals( input.getCurrLine(), 1 );
            assertEquals( input.getCurrCol(), 3 );

            c = input.next();
            assertEquals( input.getCurrLine(), 2 );
            assertEquals( input.getCurrCol(), 1 );

        }
        catch ( EOFException e )
        {
        }

        assertThrows( EOFException.class, () -> input.next() );
    }

    // ToDo: File cant yet start with \n ...

    //    @Test
    //    public void test_LineAndCol_simpleFile()
    //    {
    //        input = new Input( sourcePathSimpleFile );
    //
    //        try
    //        {
    //            Input.CharacterContext con = input.next();
    //            assertEquals( input.getCurrLine(), 1 );
    //            assertEquals( input.CURRENT_COL, 1 );
    //
    //            con = input.next();
    //            assertEquals( input.getCurrLine(), 1 );
    //            assertEquals( input.CURRENT_COL, 2 );
    //
    //            con = input.next();
    //            assertEquals( input.getCurrLine(), 1 );
    //            assertEquals( input.CURRENT_COL, 3 );
    //
    //            for (int i=0; i<24; ++i)
    //                input.next();
    //
    ////            con = input.next();
    //            assertEquals( input.getCurrLine(), 2 );
    //            assertEquals( input.CURRENT_COL, 1 );
    //
    //            con = input.next();
    //            assertEquals( input.getCurrLine(), 3 );
    //            assertEquals( input.CURRENT_COL, 1 );
    //
    //            for (int i=0; i<18; ++i)
    //                input.next();
    //
    //            assertEquals( input.getCurrLine(), 4 );
    //            assertEquals( input.CURRENT_COL, 1 );
    //
    //        }
    //        catch ( EOFException e ) { }
    //
    //    }
}
