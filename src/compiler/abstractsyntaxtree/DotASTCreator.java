package compiler.abstractsyntaxtree;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class DotASTCreator
{
    private String fileName;

    public DotASTCreator( String fileName )
    {
        this.fileName = fileName;

        try
        {
            File dotFile = new File( this.fileName );
            if ( dotFile.createNewFile() )
            {
                System.out.println( "File created: " + dotFile.getName() );
            }
            else
            {
                dotFile.delete();
                dotFile.createNewFile();
                System.out.println( "Old file deleted and new file created: " + dotFile.getName() );
            }
        }
        catch ( IOException e )
        {
            System.out.println( "An error occurred." );
            e.printStackTrace();
        }
    }

    public void createDotTree( Node root )
    {
        try
        {
            FileWriter fileWrite = new FileWriter( this.fileName );
            StringBuilder sb = new StringBuilder();

            sb.append( "digraph D {\n" );
            sb.append( "graph [ dpi = 150 ]\n" );
            sb.append( "nodesep=0.3;\n" );
            sb.append( "ranksep=0.2;\n" );
            sb.append( "margin=0.1;\n" );
            sb.append( "node [shape=circle];\n" );
            sb.append( "edge [arrowsize=0.8];\n\n" );

            StringBuilder sbTree = traversePrint( root, new StringBuilder() );

            sb.append( sbTree );
            sb.append( "\n}" );

            fileWrite.write( sb.toString() );
            fileWrite.close();
        }
        catch ( IOException e )
        {
            System.out.println( "An error occurred." );
            e.printStackTrace();
        }

    }

    /* Traverses the provided tree with deep search */
    private StringBuilder traversePrint( Node node, StringBuilder sb )
    {
        StringBuilder sbRank = new StringBuilder();

        while ( node != null )
        {
            sb.append( String.format( "%s [label=\"%s\"];\n", node.name, node.name ) );

            if ( node.left != null )
            {
                sb.append( String.format( "%s -> %s;\n", node.name, node.left.name ) );
                traversePrint( node.getLeft(), sb );
            }
            if ( node.right != null )
            {
                sb.append( String.format( "%s -> %s;\n", node.name, node.right.name ) );
                traversePrint( node.getRight(), sb );
            }
            if ( node.link != null )
            {
                sb.append( String.format( "%s -> %s;\n", node.name, node.link.name ) );
                sbRank.append( String.format( "\n{ rank=same; %s; %s; }", node.name, node.link.name ) );
            }
            node = node.getLink();
        }

        sb.append( sbRank );

        return sb;
    }
}
