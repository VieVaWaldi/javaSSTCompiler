// Frau Heuschild, während dem Sommersemester, Prüfungsamt
// im 2t Versuch, war ein praktische Aufgabe!!!!
// cc sie

package compiler.bytecode;

import static compiler.abstractsyntaxtree.NodeSubClasz.ASSIGNMENT;
import static compiler.abstractsyntaxtree.NodeSubClasz.BO_MINUS;
import static compiler.abstractsyntaxtree.NodeSubClasz.DIV;
import static compiler.abstractsyntaxtree.NodeSubClasz.EQUALS;
import static compiler.abstractsyntaxtree.NodeSubClasz.GREATEREQUALSTHAN;
import static compiler.abstractsyntaxtree.NodeSubClasz.GREATERTHAN;
import static compiler.abstractsyntaxtree.NodeSubClasz.LESSEQUALSTHAN;
import static compiler.abstractsyntaxtree.NodeSubClasz.LESSTHAN;
import static compiler.abstractsyntaxtree.NodeSubClasz.PLUS;
import static compiler.abstractsyntaxtree.NodeSubClasz.TIMES;

import static compiler.bytecode.ByteCodeFlags.MOD_FINAL_STATIC_PUBLIC;
import static compiler.bytecode.ByteCodeFlags.MOD_STATIC_PUBLIC;
import static compiler.bytecode.ByteCodeFlags.MOD_SUPER_PUBLIC;
import static compiler.bytecode.ByteCodeFlags.TYPE_CLASS;
import static compiler.bytecode.ByteCodeFlags.TYPE_FIELREF;
import static compiler.bytecode.ByteCodeFlags.TYPE_INT;
import static compiler.bytecode.ByteCodeFlags.TYPE_METHREF;
import static compiler.bytecode.ByteCodeFlags.TYPE_NANDT;
import static compiler.bytecode.ByteCodeFlags.TYPE_UTF8;
import static compiler.bytecode.ByteCodeFlags.CAFE_BABE;
import static compiler.bytecode.ByteCodeFlags.EMPTY;
import static compiler.bytecode.ByteCodeFlags.MAJOR_VERSION;
import static compiler.bytecode.ByteCodeFlags.MINOR_VERSION;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import compiler.abstractsyntaxtree.INode;
import compiler.abstractsyntaxtree.nodes.AssignmentNode;
import compiler.abstractsyntaxtree.nodes.ConstantNode;
import compiler.abstractsyntaxtree.nodes.FactorNode;
import compiler.abstractsyntaxtree.nodes.MethodNode;
import compiler.abstractsyntaxtree.nodes.MethodVarNode;
import compiler.abstractsyntaxtree.nodes.NumberNode;
import compiler.abstractsyntaxtree.nodes.TermNode;
import compiler.symboltable.Objekt;
import compiler.symboltable.Type;

/**
 * Creates the .class file when provided with a root.
 * Goes through all the links of the AST, and traverses depth first for each method.
 * Writes into the .class file for each node.
 */
public class ByteCodeCompiler
{
    private final INode root;

    private final String fileName;

    private final byte[] code;

    private int idx;

    private final int CODE_MAX = 65536;

    private final int ESCAPE_SEQ = -1;

    public ByteCodeCompiler( INode root )
    {
        this.root = root;
        this.fileName = "src/output/" + this.root.getName() + ".class";

        prepareFile();

        this.code = new byte[CODE_MAX];
        for ( int i = 0; i < CODE_MAX; i++ )
        {
            code[i] = ESCAPE_SEQ;
        }
        this.idx = 0;
    }

    public void compile()
    {
        appendMagic();

        try
        {
            FileOutputStream os = new FileOutputStream( fileName );
            for ( byte b : code )
            {
                if ( b == ESCAPE_SEQ )
                {
                    break;
                }
                os.write( b );
            }
            os.close();
        }
        catch ( IOException e )
        {
            System.out.println( "An error occurred, trying to compile the class file." );
            e.printStackTrace();
        }
    }

    /**
     * Here lies the magic and byte code is generated.
     * FYI:
     * u1: an unsigned 8-bit integer
     * u2: an unsigned 16-bit integer in big-endian byte order (BIG ENDIAN FIRST)
     * u4: an unsigned 32-bit integer in big-endian byte order
     * table: an array of variable-length items of some type.
     */
    private void appendMagic()
    {
        /* MAGIC NUMBER */
        insertCode( CAFE_BABE ); // 4 * u1

        /* VERSION MINOR AND MAJOR */
        insertCode( EMPTY ); // u2
        insertCode( EMPTY );

        insertCode( MINOR_VERSION ); // u2, Version 8
        insertCode( MAJOR_VERSION );

        /* CONSTANT POOL */
        insertConstPool();

        /* MODIFIERS */
        insertCode( EMPTY ); // u2
        insertCode( MOD_SUPER_PUBLIC );

        /* IDX OF CLASS IN CONST POOL */
        insertCode( EMPTY ); // u2
        insertCode( 0x01 );

        /* IDX OF SUPER (Object) IN CONST POOL */
        insertCode( EMPTY ); // u2
        insertCode( 0x03 );

        /* - AMOUNT OF INTERFACES */
        insertCode( EMPTY ); // u2
        insertCode( EMPTY );

        /* FIELD DEFINITIONS */
        insertFields();

        /* METHODS DEFINITIONS */
        insertMethods();

        /* - AMOUNT OF ATTRIBUTES (meta data) */
        insertCode( EMPTY ); // u2
        insertCode( EMPTY );

        /* - ATTRIBUTES */
    }

    //@formatter:off
    private int idxOfIntDesc;
    private int idxOfConstValue;
    private int idxOfMethodPara;
    private int idxOfCode;

    private final ArrayList<Integer> idxListOfFieldConst = new ArrayList<>();
    private final ArrayList<Integer> idxListOfFieldMethod = new ArrayList<>();
    //@formatter:on

    /**
     * Here lies code to create the constant pool.
     * Indexes in the pool start at #1. This only holds for the constant pool.
     * Every entry starts with a tag, so the JVM knows how long the information of the entry is.
     */
    private void insertConstPool()
    {
        int constPoolLength = 1;

        /* CONST LENGTH */
        int lazyIdx_ConstPoolLength_U2 = idx;
        insertEmptyU2(); // u2 -> EVAL LATER

        /* #1 CONST_Class {1tag, 2idx} */
        insertCode( TYPE_CLASS ); // u1
        insertCode( toU2( 2 ) ); // u2
        constPoolLength++;

        /* #2 CONST_Utf8 {1tag, 2len, tab} */
        insertCode( TYPE_UTF8 ); // u1
        var bName = root.getName().getBytes( StandardCharsets.UTF_8 );
        insertCode( toU2( bName.length ) ); // u2
        insertCode( bName ); // tab
        constPoolLength++;

        /* #3 CONST_Class Object {1tag, 2idx} */
        insertCode( TYPE_CLASS ); // u1
        insertCode( toU2( 4 ) ); // u2
        constPoolLength++;

        /* #4 CONST_Utf8 {1tag, 2len, tab} */
        insertCode( TYPE_UTF8 ); // u1
        bName = "java/lang/Object".getBytes( StandardCharsets.UTF_8 );
        insertCode( toU2( bName.length ) ); // u2
        insertCode( bName ); // tab
        constPoolLength++;

        /* #5+ FIELDS */
        INode fieldNode = root.getLeft();

        /* #5.0 CONST_UTF8 DESC { 1tag, 2len, tab = I} */
        insertCode( TYPE_UTF8 ); // u1
        bName = "I".getBytes( StandardCharsets.UTF_8 );
        insertCode( toU2( bName.length ) ); // u2
        insertCode( bName ); // tab
        idxOfIntDesc = constPoolLength;
        constPoolLength++;

        while ( fieldNode != null )
        {
            if ( fieldNode instanceof ConstantNode || fieldNode instanceof AssignmentNode )
            {
                /* #5.1 CONST_FIELDREF {1tag, 2class_idx, 2nAt_idx} */
                insertCode( TYPE_FIELREF ); // u1
                insertCode( toU2( 1 ) ); // u2
                insertCode( toU2( constPoolLength + 1 ) ); // u2
                constPoolLength++;

                /* #5.2 CONST_NAMEAndTYPE {1tag, 2name_idx, 2desc_idx} */
                insertCode( TYPE_NANDT ); // u1
                insertCode( toU2( constPoolLength + 1 ) ); // u2 = idx of object_string (always +1)
                insertCode( toU2( idxOfIntDesc ) ); // u2
                constPoolLength++;

                /* #5.3 CONST_UTF8 {1tag, 2name_idx, tab} // DESCRIPTOR u2: I */
                idxListOfFieldConst.add( constPoolLength );
                insertCode( TYPE_UTF8 ); // u1
                if ( fieldNode instanceof AssignmentNode )
                {
                    bName = fieldNode.getLeft().getName().getBytes( StandardCharsets.UTF_8 );
                }
                else
                {
                    bName = fieldNode.getName().getBytes( StandardCharsets.UTF_8 );
                }
                insertCode( toU2( bName.length ) ); // u2
                insertCode( bName ); // tab
                constPoolLength++;

                /* #5.4 CONST_INT {1tag, 4value} */
                insertCode( TYPE_INT ); // u1
                if ( fieldNode instanceof AssignmentNode )
                {
                    insertCode( toU4( getValueFromExpression( fieldNode ) ) );
                }
                else
                {
                    insertCode( toU4( fieldNode.getObj().getValue() ) );
                }
                constPoolLength++;
            }

            /* #6+ METHODS */

            else if ( fieldNode instanceof MethodNode )
            {
                /* #6.0 CONST_UTF8 DESC {(*PARA)RETURN} */
                insertCode( TYPE_UTF8 ); // u1
                bName = returnMethodDescCheckMain( fieldNode ).getBytes( StandardCharsets.UTF_8 );
                insertCode( toU2( bName.length ) ); // u2
                insertCode( bName ); // tab
                constPoolLength++;

                /* #6.1 CONST_METHODREF {1tag, 2class_idx, 2nAt_idx} */
                insertCode( TYPE_METHREF ); // u1
                insertCode( toU2( 1 ) ); // u2
                insertCode( toU2( constPoolLength + 1 ) ); // u2
                constPoolLength++;

                /* #6.2 CONST_NAMEAndTYPE {1tag, 2name_idx, 2desc_idx} */
                insertCode( TYPE_NANDT ); // u1
                insertCode( toU2( constPoolLength + 1 ) ); // u2 = idx of object_string (always +1)
                insertCode( toU2( constPoolLength - 2 ) ); // u2 = idx of method desc (always -2)
                constPoolLength++;

                /* #6.3 CONST_UTF8 {1tag, 2name_idx, tab} */
                idxListOfFieldMethod.add( constPoolLength );
                insertCode( TYPE_UTF8 ); // u1
                bName = fieldNode.getName().getBytes( StandardCharsets.UTF_8 );
                insertCode( toU2( bName.length ) ); // u2
                insertCode( bName ); // tab
                constPoolLength++;

                if ( nodeIsMain( fieldNode ) )
                {
                    /* #6.4 CONST_UTF8 {1tag, 2name_idx, tab} // One Main Para */
                    insertCode( TYPE_UTF8 ); // u1
                    bName = "args".getBytes( StandardCharsets.UTF_8 );
                    insertCode( toU2( bName.length ) ); // u2
                    insertCode( bName ); // tab
                    constPoolLength++;
                }
                else
                {
                    Objekt para = fieldNode.getObj().getParameter();
                    while ( para != null )
                    {
                        /* #6.4 CONST_UTF8 {1tag, 2name_idx, tab} // Each Para */
                        insertCode( TYPE_UTF8 ); // u1
                        bName = para.getName().getBytes( StandardCharsets.UTF_8 );
                        insertCode( toU2( bName.length ) ); // u2
                        insertCode( bName ); // tab
                        constPoolLength++;
                        para = para.getNextObj();
                    }
                }
            }

            fieldNode = fieldNode.getLink();
        }

        /* #7 EXTRA */

        /* #7.1.0 CLASS SYSTEM  */
        insertCode( TYPE_CLASS ); // u1
        insertCode( toU2( constPoolLength + 1 ) ); // u2
        constPoolLength++;

        /* #7.1.1 UTF8 SYSTEM  */
        insertCode( TYPE_UTF8 ); // u1
        bName = "java/lang/System".getBytes( StandardCharsets.UTF_8 );
        insertCode( toU2( bName.length ) ); // u2
        insertCode( bName ); // tab
        constPoolLength++;

        /* #7.1.2 CLASS PRINT_STREAM */
        insertCode( TYPE_CLASS ); // u1
        insertCode( toU2( constPoolLength + 1 ) ); // u2
        constPoolLength++;

        /* #7.1.1 PRINT_STREAM */
        insertCode( TYPE_UTF8 ); // u1
        bName = "java/io/PrintStream".getBytes( StandardCharsets.UTF_8 );
        insertCode( toU2( bName.length ) ); // u2
        insertCode( bName ); // tab
        constPoolLength++;

        /* #7.2 CONSTANT VALUE */
        insertCode( TYPE_UTF8 );
        bName = "ConstantValue".getBytes( StandardCharsets.UTF_8 );
        insertCode( toU2( bName.length ) );
        insertCode( bName );
        idxOfConstValue = constPoolLength;
        constPoolLength++;

        /* #7.3 METHOD PARA */
        insertCode( TYPE_UTF8 );
        bName = "MethodParameters".getBytes( StandardCharsets.UTF_8 );
        insertCode( toU2( bName.length ) );
        insertCode( bName );
        idxOfMethodPara = constPoolLength;
        constPoolLength++;

        /* #7.4 CODE */
        insertCode( TYPE_UTF8 );
        bName = "Code".getBytes( StandardCharsets.UTF_8 );
        insertCode( toU2( bName.length ) );
        insertCode( bName );
        idxOfCode = constPoolLength;
        constPoolLength++;

        /* #END eval const pool length */
        insertCodeAt( toU2( constPoolLength ), lazyIdx_ConstPoolLength_U2 );
    }

    /**
     * Here lies code to create the field pool.
     * Indexes in the pool start at #0 again.
     * Every entry looks like this:
     * field_info {
     * u2             access_flags;
     * u2             name_index;
     * u2             descriptor_index;
     * u2             attributes_count;
     * attribute_info attributes[attributes_count];
     * }
     * ConstantValue_attribute {
     * u2             attribute_name_index;
     * u4             attribute_length;
     * u2             constantvalue_index;
     * }
     */
    private void insertFields()
    {
        insertCode( toU2( idxListOfFieldConst.size() ) );

        for ( int i : idxListOfFieldConst )
        {
            /* ACCESS FLAG */
            insertCode( toU2( MOD_FINAL_STATIC_PUBLIC ) );

            /* NAME INDEX */
            insertCode( toU2( i ) );

            /* DESC INDEX */
            insertCode( toU2( idxOfIntDesc ) );

            /* ATTRIBUTE COUNT */
            insertCode( toU2( 1 ) );

            /* ATTRIBUTES: CONSTANT VALUE */
            insertCode( toU2( idxOfConstValue ) );
            insertCode( toU4( 2 ) );
            insertCode( toU2( i + 1 ) ); // const always +1
        }
    }

    private int methodCodeAttLength_Bytes = 0;

    private int codeLength_Bytes = 0;

    /**
     * Here lies code to create the method pool.
     * Indexes in the pool start at #0 again.
     * Every method has its own method descriptor, regardless if it is already in the const pool.
     * Every entry looks like this:
     * method_info {
     * u2             access_flags;
     * u2             name_index;
     * u2             descriptor_index;
     * u2             attributes_count;
     * attribute_info attributes[attributes_count];
     * }
     * attribute_info {
     * u2             attribute_name_index;
     * u4             attribute_length;
     * u1             info[attribute_length];
     * }
     */
    private void insertMethods()
    {
        /* Find first method */
        INode methodNode = root.getLeft();
        while ( methodNode.getLink() != null )
        {
            if ( methodNode instanceof MethodNode )
            {
                break;
            }
            methodNode = methodNode.getLink();
        }

        insertCode( toU2( idxListOfFieldMethod.size() ) );

        for ( int i : idxListOfFieldMethod )
        {
            /* ACCESS FLAG */
            insertCode( toU2( MOD_STATIC_PUBLIC ) );

            /* NAME INDEX */
            insertCode( toU2( i ) );

            /* DESC INDEX */
            insertCode( toU2( i - 3 ) );

            /* ATT COUNT and ATTRIBUTE MethodParameters ***************/

            if ( nodeIsMain( methodNode ) )
            {
                /* ATTRIBUTE COUNT */
                insertCode( toU2( 2 ) );

                /* #1 IDX of MethodParameters */
                insertCode( toU2( idxOfMethodPara ) );

                /* #2 LENGTH */
                insertCode( toU4( 0x05 ) ); // para count and para entry

                /* #3 PARA COUNT */
                insertCode( 0x01 );

                /* #4.1 PARA IDX */
                insertCode( toU2( i + 1 ) );

                /* #4.2 PARA MODIFIER */
                insertCode( toU2( 0x00 ) );
            }
            else if ( methodNode.getObj().getParaCount() > 0 )
            {
                /* ATTRIBUTE COUNT */
                insertCode( toU2( 2 ) );

                /* ATTRIBUTE MethodParameters */

                /* #1 IDX of MethodParameters */
                insertCode( toU2( idxOfMethodPara ) );

                /* #2 LENGTH */
                int lazyIdx_MethodParaAttLength_U4 = idx;
                int methodParaAttLength_Bytes = 0;
                insertEmptyU4();

                /* #3 PARA COUNT */
                insertCode( methodNode.getObj().getParaCount() );
                methodParaAttLength_Bytes++;

                Objekt para = methodNode.getObj().getParameter();
                int paraIdxOffset = 1;
                while ( para != null )
                {
                    /* #4.1 PARA IDX */
                    insertCode( toU2( i + paraIdxOffset++ ) );
                    methodParaAttLength_Bytes += 2;

                    /* #4.2 PARA MODIFIER */
                    insertCode( toU2( 0x00 ) );
                    methodParaAttLength_Bytes += 2;

                    para = para.getNextObj();
                }
                insertCodeAt( toU4( methodParaAttLength_Bytes ), lazyIdx_MethodParaAttLength_U4 );
            }
            else
            {
                /* ATTRIBUTE COUNT */
                insertCode( toU2( 1 ) );
            }
            /* **************************************** */

            /* ****************************** ATTRIBUTE: CODE ****************************** */

            /* #1 IDX of Code */
            insertCode( toU2( idxOfCode ) );

            /* #2 Length of ATT (in bytes) */
            int lazyIdx_MethodCodeAttLength_U4 = idx;
            insertEmptyU4();

            /* #3 MAX STACK u2 */ // ToDo
            insertEmptyU2();
            methodCodeAttLength_Bytes += 2;

            /* #4 MAX LOCALS u2 */
            int localsCount = 0;
            if ( nodeIsMain( methodNode ) )
            {
                localsCount++;
            }
            Objekt paraObj = methodNode.getObj().getSymTable().getHead();
            while ( paraObj != null )
            {
                localsCount++;
                paraObj = paraObj.getNextObj();
            }
            insertCode( toU2( localsCount ) );
            methodCodeAttLength_Bytes += 2;

            /* #5 CODE LENGTH u4 */
            int lazyIdx_codeLength_U4 = idx;
            methodCodeAttLength_Bytes += 4;

            /* #6 CODE ENTRIES u1 */ // ToDo
            produceCode( methodNode, methodNode );
            insertCodeAt( toU4( codeLength_Bytes ), lazyIdx_codeLength_U4 );

            /* - #7 EXCEPTIONS LENGTH u2 */
            insertEmptyU2();
            methodCodeAttLength_Bytes += 2;

            /* - #8 ATT COUNT u2 */
            insertEmptyU2();
            methodCodeAttLength_Bytes += 2;

            insertCodeAt( toU4( methodCodeAttLength_Bytes ), lazyIdx_MethodCodeAttLength_U4 );
            /* ***************************************************************************** */

            methodNode = methodNode.getLink();
        }
    }

    private void produceCode( INode node, INode methodNode )
    {
        if ( node == null )
        {
            return;
        }
        produceCode( node.getLeft(), methodNode );
        produceCode( node.getRight(), methodNode );

        int numOfInstructions = 0;

        switch ( node.getNodeClasz() )
        {
            case BINOPS:
                if ( node.subClaszEquals( ASSIGNMENT ) )
                {

                }
                else if ( node.subClaszEquals( EQUALS ) )
                {
                    insertCode( 0xA5 ); // if_acmpeq
                    numOfInstructions = 1;
                }
                else if ( node.subClaszEquals( LESSTHAN ) )
                {
                    insertCode( 0xA1 ); // if_icmplt
                    numOfInstructions = 1;
                }
                else if ( node.subClaszEquals( LESSEQUALSTHAN ) )
                {
                    insertCode( 0xA4 ); // if_icmple
                    numOfInstructions = 1;
                }
                else if ( node.subClaszEquals( GREATERTHAN ) )
                {
                    insertCode( 0xA3 ); // if_icmpgt
                    numOfInstructions = 1;
                }
                else if ( node.subClaszEquals( GREATEREQUALSTHAN ) )
                {
                    insertCode( 0xA2 ); // if_icmpge
                    numOfInstructions = 1;
                }
                else if ( node.subClaszEquals( TIMES ) )
                {
                    insertCode( 0x68 ); // imul
                    numOfInstructions = 1;
                }
                else if ( node.subClaszEquals( DIV ) )
                {
                    insertCode( 0x6c ); // idiv
                    numOfInstructions = 1;
                }
                else if ( node.subClaszEquals( PLUS ) )
                {
                    insertCode( 0x60 ); // iadd
                    numOfInstructions = 1;
                }
                else if ( node.subClaszEquals( BO_MINUS ) )
                {
                    insertCode( 0x64 ); // isub
                    numOfInstructions = 1;
                }
                break;
            case METHOD:
            {

                break;
            }
            case METHOD_VAR:
            {
                if ( node instanceof NumberNode )
                {
                    if ( node.hasConstant() )
                    {
                        insertCode( 0x11 ); // bipush, there is no ipush
                        insertCode( toU2( node.getConstant() ) );
                        numOfInstructions = 3;
                    }
                    // TODO: else?
                }
                else if ( node instanceof MethodVarNode )
                {
                    if ( node.getObj().hasValue() )
                    {
                        insertCode( 0x15 ); // iload
                        insertCode( toU2( node.getObj().getValue() ) );
                        numOfInstructions = 3;
                    }
                    // TODO: else?
                }
                break;
            }
            case IF:
            {

                break;
            }
            case WHILE:
            {

                break;
            }
            case RETURN:
            {
                if ( methodNode.getObj().getReturnType().equals( Type.Int ) )
                {
                    insertCode( 0xAC ); // ireturn
                    numOfInstructions = 1;
                }
                else
                {
                    insertCode( 0xB1 ); // return
                    numOfInstructions = 1;
                }

                break;
            }
            default:
            {
                compileError( "Dev made a mistake generating method code :( Maybe he missed implementing all cases" );
            }

        }

        methodCodeAttLength_Bytes += numOfInstructions;
        codeLength_Bytes += numOfInstructions;
    }

    /**
     * For assignment nodes only this evaluates the expression at compile time.
     * This got a bit ugly, but it works and i have to submit in less than 8 hours.
     *
     * @param node AssignmentNode to evaluate
     * @return evaluated value or -1 when something went wrong
     */
    private int getValueFromExpression( INode node )
    {
        traverseExpression( node.getRight() );
        if ( node.getRight().getExpressionConst() != null )
        {
            return node.getRight().getExpressionConst();
        }
        else
        {
            return -1;
        }
    }

    private void traverseExpression( INode node )
    {
        if ( node.getLeft() != null )
        {
            traverseExpression( node.getLeft() );
        }
        if ( node.getRight() != null )
        {
            traverseExpression( node.getRight() );
        }

        if ( node instanceof TermNode )
        {
            if ( node.getRight().getExpressionConst() != null )
            {
                int val;
                if ( node.getName().equals( "+" ) )
                {
                    val = node.getLeft().getExpressionConst() + node.getRight().getExpressionConst();
                }
                else
                {
                    val = node.getLeft().getExpressionConst() - node.getRight().getExpressionConst();
                }
                node.setExpressionConst( val );
            }
        }
        if ( node instanceof FactorNode )
        {
            if ( node.getRight().getExpressionConst() != null )
            {
                int val;
                if ( node.getName().equals( "*" ) )
                {
                    val = node.getLeft().getExpressionConst() * node.getRight().getExpressionConst();
                }
                else
                {
                    val = node.getLeft().getExpressionConst() / node.getRight().getExpressionConst();
                }
                node.setExpressionConst( val );
            }
        }
        if ( node instanceof MethodVarNode )
        {
            node.setExpressionConst( node.getObj().getValue() );
        }
        if ( node instanceof NumberNode )
        {
            node.setExpressionConst( Integer.parseInt( node.getName() ) );
        }
    }

    /************************************** HELPER *************************************/

    /**
     * Always uses BIG ENDIAN. Undefined behaviour when int is bigger than 2 bytes.
     *
     * @param value int value that should be converted to a byte array
     */
    private byte[] toU2( int value )
    {
        final ByteBuffer bb = ByteBuffer.allocate( 2 );
        bb.putShort( (short) value );
        return bb.array();
    }

    /**
     * Always uses BIG ENDIAN. Undefined behaviour when int is bigger than 4 bytes.
     *
     * @param value int value that should be converted to a byte array
     */
    private byte[] toU4( int value )
    {
        final ByteBuffer bb = ByteBuffer.allocate( 4 );
        bb.putInt( value );
        return bb.array();
    }

    private void insertEmptyU2()
    {
        insertCode( EMPTY );
        insertCode( EMPTY );
    }

    private void insertEmptyU4()
    {
        insertEmptyU2();
        insertEmptyU2();
    }

    private void insertCode( int iin )
    {
        byte in = (byte) iin;
        if ( idx < CODE_MAX )
            code[idx++] = in;
        else
            compileError( "Your code is too long." );
    }

    private void insertCode( byte[] in )
    {
        if ( idx < CODE_MAX )
        {
            for ( byte b : in )
            {
                if ( b == ESCAPE_SEQ )
                {
                    break;
                }
                code[idx++] = b;
            }
        }
        else
            compileError( "Your code is too long." );
    }

    private void insertCode( int[] in )
    {
        if ( idx < CODE_MAX )
        {
            for ( int b : in )
            {
                if ( b == ESCAPE_SEQ )
                {
                    break;
                }
                code[idx++] = (byte) b;
            }
        }
        else
            compileError( "Your code is too long." );
    }

    private void insertCodeAt( byte[] in, int at )
    {
        if ( idx < CODE_MAX )
        {
            for ( byte b : in )
            {
                if ( b == ESCAPE_SEQ )
                {
                    break;
                }
                code[at++] = b;
            }
        }
        else
            compileError( "Your code is too long." );
    }

    /**
     * Returns the right method descriptor.
     * For "()V main" only, creates the method descriptor for the entry point.
     */
    private String returnMethodDescCheckMain( INode node )
    {
        Type type = node.getObj().getReturnType();

        StringBuilder sb = new StringBuilder();
        sb.append( "(" );
        if ( nodeIsMain( node ) )
        {
            sb.append( "[Ljava/lang/String;" );
        }
        else
        {
            sb.append( "I".repeat( node.getObj().getParaCount() ) );
        }
        sb.append( ")" );

        if ( type.equals( Type.Int ) )
        {
            sb.append( "I" );
        }
        else if ( type.equals( Type.Void ) )
        {
            sb.append( "V" );
        }
        else
        {
            compileError( "Dev made a mistake parsing the return type for descriptor of methods." );
        }

        return sb.toString();
    }

    public boolean nodeIsMain( INode node )
    {
        return node.getName().equalsIgnoreCase( "main" ) && node.getObj().getReturnType().equals( Type.Void )
                        && node.getObj().getParaCount() == 0;
    }

    private void compileError( String em )
    {
        System.out.printf( "$ CompileError: %s", em );
        System.exit( 1 );
    }

    private void prepareFile()
    {
        try
        {
            File classFile = new File( fileName );
            if ( classFile.createNewFile() )
            {
                System.out.println( "File created: " + classFile.getName() );
            }
            else
            {
                classFile.delete();
                classFile.createNewFile();
                System.out.println( "Old file deleted and new file created: " + classFile.getName() );
            }
        }
        catch ( IOException e )
        {
            System.out.println( "An error occurred, trying to compile the class file." );
            e.printStackTrace();
        }
    }
}
