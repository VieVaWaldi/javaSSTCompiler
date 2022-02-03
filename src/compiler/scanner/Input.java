package compiler.scanner;

import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Takes a File and prepares it for the scanner.
 */
public class Input
{
    private FileInputStream fileReader;

    private int CURRENT_LINE = 1;

    private int CURRENT_COL = 0;

    private final char NEW_LINE = '\n';

    public Input( String sourcePath )
    {
        prepareFileReader( sourcePath );
    }

    /**
     * Reads and returns the next character from file.
     * Always starts from first character.
     *
     * @return CharacterContext, contains character, line and col
     * @throws EOFException on file end
     */
    public char next()
                    throws EOFException
    {
        try
        {
            int r;
            if ( ( r = fileReader.read() ) != -1 )
            {
                char c = (char) r;
                if ( c == NEW_LINE )
                {
                    CURRENT_LINE++;
                    CURRENT_COL = 1;
                }
                else if ( c == '\t' )
                {
                    CURRENT_COL += 4;
                }
                else
                {
                    CURRENT_COL++;
                }
                return c;
            }
            else
            {
                throw new EOFException( "Reached end of file." );
            }
        }
        catch ( EOFException e )
        {
            throw e;
        }
        catch ( IOException e )
        {
            System.out.println( "A problem occurred when reading the file." );
            e.printStackTrace();
            System.exit( 1 );
            return ' '; // real fishy, but compiler needs it
        }
    }

    /**
     * Prints the file for debugging purposes.
     * ToDo: Reset the fileReader.
     */
    public String printWholeFile()
    {
        String source = "";
        try
        {
            int r;
            while ( ( r = fileReader.read() ) != -1 )
            {
                char c = (char) r;
                source = source + c;
                System.out.print( c );
            }
            //            fileReader.reset();
        }
        catch ( IOException e )
        {
            System.out.println( "A problem occurred when reading the file." );
            e.printStackTrace();
        }

        return source;
    }

    public int getCurrLine()
    {
        return CURRENT_LINE;
    }

    public int getCurrCol()
    {
        return CURRENT_COL - 1;
    }

    private void prepareFileReader( String sourcePath )
    {
        try
        {
            fileReader = new FileInputStream( sourcePath );
        }
        catch ( FileNotFoundException e )
        {
            System.out.println( "File could not be read." );
            e.printStackTrace();
        }

    }
}
