package compiler.abstractsyntaxtree;

import compiler.helper.SymbolContext;
import compiler.symboltable.Objekt;
import compiler.symboltable.Type;

/**
 * Every node needs a nodeClasz, name and type.
 * Constants can have values.
 * Declared variables have an Object.
 */
public abstract class INode
{
    protected String name;

    protected String dotName;

    protected INode left;

    protected INode right;

    protected INode link;

    protected NodeClasz nodeClasz;

    protected NodeSubClasz nodeSubClasz;

    protected Type type;

    /**
     * Only specific nodes need the following members.
     */

    protected Objekt obj;

    protected Long constant;

    protected SymbolContext context;

    /**
     * The expected type of child nodes for semantic analysis.
     */
    protected Type expected;

    public void appendLeft( INode node )
    {
        this.left = node;
    }

    public void appendRight( INode node )
    {
        this.right = node;
    }

    public void appendLink( INode node )
    {
        if ( this.link == null )
        {
            this.link = node;
        }
        else
        {
            this.link.appendLink( node );
        }
    }

    /**
     * Check left and rights node type.
     * Should only do something when implemented in sublclass.
     */
    public void checkExpected()
    {
    }

    /**
     * Rules for semantic analysis.
     * Should only do something when implemented in sublclass.
     */
    public void checkRules()
    {
    }

    /****************************** GETTERs and SETTERs ******************************/

    public INode getLeft()
    {
        return this.left;
    }

    public INode getRight()
    {
        return this.right;
    }

    public INode getLink()
    {
        return this.link;
    }

    public String getName()
    {
        return this.name;
    }

    /**
     * Needs type as input in case getType() of methodNodes is called.
     */
    public Type getType( Type type )
    {
        return this.type;
    }

    public Objekt getObj()
    {
        return this.obj;
    }

    public Long getConstant()
    {
        return this.constant;
    }

    public String getDotName()
    {
        if ( dotName != null )
        {
            return this.dotName;
        }
        else
        {
            return this.name;
        }
    }

    public String toDotString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append( this.name );

        if ( this.type != null )
        {
            sb.append( "\\nType: " + this.type );
            if ( this.constant != null )
            {
                sb.append( ", Value: " + this.constant );
            }
        }
        if ( this.obj != null )
        {
            sb.append( ", hasST" );
        }
        return sb.toString();
    }

    /**
     * Error and abort for type analysis.
     */
    protected void typeError( String msg )
    {
        if ( this.context != null )
        {
            System.out.printf( "$ TypeError at Line %s:%s with %s: \"%s\".\n$ %s%n", this.context.getLine(),
                            this.context.getColumn(), this.context.getSym(), this.context.getValue(), msg );

        }
        else
        {
            System.out.printf( "$ TypeError. Mistakes were made and i dont know where: %s", msg );
        }
        System.exit( 1 );
    }

    protected void error( String msg )
    {
        System.out.println( "Sorry but the dev made a mistake ..." );
        System.out.println( msg );
        System.exit( 1 );
    }
}
