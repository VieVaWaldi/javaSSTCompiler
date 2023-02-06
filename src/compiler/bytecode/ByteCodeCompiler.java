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
import static compiler.bytecode.ByteCodeFlags.TYPE_FIELDREF;
import static compiler.bytecode.ByteCodeFlags.TYPE_INT;
import static compiler.bytecode.ByteCodeFlags.TYPE_METHODREF;
import static compiler.bytecode.ByteCodeFlags.TYPE_NAMEANDTYPE;
import static compiler.bytecode.ByteCodeFlags.TYPE_STRING;
import static compiler.bytecode.ByteCodeFlags.TYPE_UTF8;
import static compiler.bytecode.ByteCodeFlags.CAFE_BABE;
import static compiler.bytecode.ByteCodeFlags.EMPTY;
import static compiler.bytecode.ByteCodeFlags.MAJOR_VERSION;
import static compiler.bytecode.ByteCodeFlags.MINOR_VERSION;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import compiler.abstractsyntaxtree.INode;
import compiler.abstractsyntaxtree.NodeClasz;
import compiler.abstractsyntaxtree.nodes.AssignmentNode;
import compiler.abstractsyntaxtree.nodes.ClassVarNode;
import compiler.abstractsyntaxtree.nodes.ConstantNode;
import compiler.abstractsyntaxtree.nodes.FactorNode;
import compiler.abstractsyntaxtree.nodes.MethodNode;
import compiler.abstractsyntaxtree.nodes.MethodVarNode;
import compiler.abstractsyntaxtree.nodes.NumberNode;
import compiler.abstractsyntaxtree.nodes.TermNode;
import compiler.symboltable.Objekt;
import compiler.symboltable.ObjektConst;
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

    private final int CODE_MAX = 65536;

    private final int ESCAPE_SEQ = -1;

    private int idx;

    private int constPoolLength;

    private boolean useTempStack;

    private byte[] tempStack;

    private int idxTempStack;

    public ByteCodeCompiler( INode root )
    {
        this.root = root;
        this.fileName = "src/output/" + this.root.getName() + ".class";

        prepareFile();

        this.code = new byte[CODE_MAX];
        this.tempStack = new byte[CODE_MAX];
        for ( int i = 0; i < CODE_MAX; i++ )
        {
            code[i] = ESCAPE_SEQ;
            tempStack[i] = ESCAPE_SEQ;
        }
        this.idx = 0;
        this.idxTempStack = 0;
        useTempStack = false;
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

        /* CONSTANT COUNT AND POOL */
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

        /* FIELD COUNT AND DEFINITIONS */
        insertFields();

        /* METHODS COUNT AND DEFINITIONS */
        insertCode( toU2( idxListOfFieldMethod.size() ) );
        insertMethodPool();

        /* - AMOUNT OF ATTRIBUTES (meta data) */
        insertCode( EMPTY ); // u2
        insertCode( EMPTY );

        /* - ATTRIBUTES */
    }

    //@formatter:off
    /* To reference the indexes of fields and methods in the constant_pool later on. */

    private int idxOfAttIntDesc;
    private int idxOfAttConstValue;
    private int idxOfAttMethodPara;
    private int idxOfAttCode;

    private int idxOfCallOut;
    private int idxOfCallPrintln;

    private int idxOfConstHello;

    private int idxOfFuckYouTempVariable;

    private final ArrayList<Integer> idxListOfFieldConst = new ArrayList<>();
    private final ArrayList<Integer> idxListOfFields = new ArrayList<>();
    private final ArrayList<Integer> idxListOfFieldMethod = new ArrayList<>();
    //@formatter:on

    /**
     * Here lies code to create the constant pool.
     * Indexes in the pool start at #1. This only holds true for the constant pool.
     * Every entry starts with a tag, so the JVM knows how long the information of the entry is.
     */
    private void insertConstPool()
    {
        constPoolLength = 1;

        /* CONST LENGTH */
        int lazyIdx_ConstPoolLength_U2 = idx;
        insertEmptyU2(); // u2 -> EVAL LATER

        /* #1 CONST_Class {1tag, 2idx} */
        cp_insertBlock_U1_U2( TYPE_CLASS, 2 );

        /* #2 CONST_Utf8 {1tag, 2len, tab} */
        cp_insertBlock_U1_U2_Tab( TYPE_UTF8, root.getName() );

        /* #3 CONST_Class Object {1tag, 2idx} */
        cp_insertBlock_U1_U2( TYPE_CLASS, 4 );

        /* #4 CONST_Utf8 {1tag, 2len, tab} */
        cp_insertBlock_U1_U2_Tab( TYPE_UTF8, "java/lang/Object" );

        /* #5.1 Consts and 5.2 FIELDS */
        INode fieldNode = root.getLeft();

        /* #5.0 CONST_UTF8 DESC { 1tag, 2len, tab = I} */
        idxOfAttIntDesc = constPoolLength;
        cp_insertBlock_U1_U2_Tab( TYPE_UTF8, "I" );

        while ( fieldNode != null )
        {
            if ( fieldNode instanceof ConstantNode || fieldNode instanceof AssignmentNode )
            {
                /* #5.1.1 CONST_FIELDREF {1tag, 2class_idx, 2nAt_idx} */
                cp_insertBlock_U1_U2_U2( TYPE_FIELDREF, 1, constPoolLength + 1 );

                /* #5.1.2 CONST_NAMEAndTYPE {1tag, 2name_idx, 2desc_idx} */
                cp_insertBlock_U1_U2_U2( TYPE_NAMEANDTYPE, constPoolLength + 1, idxOfAttIntDesc );

                /* #5.1.3 CONST_UTF8 {1tag, 2name_idx, tab} // DESCRIPTOR u2: I */
                idxListOfFieldConst.add( constPoolLength );
                final String bName;
                if ( fieldNode instanceof AssignmentNode )
                {
                    bName = fieldNode.getLeft().getName();
                }
                else
                {
                    bName = fieldNode.getName();
                }
                cp_insertBlock_U1_U2_Tab( TYPE_UTF8, bName );

                /* #5.1.4 CONST_INT {1tag, 4value} */
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
            else if ( fieldNode instanceof ClassVarNode )
            {
                /* #5.2.1 CONST_FIELDREF {1tag, 2class_idx, 2nAt_idx} */
                cp_insertBlock_U1_U2_U2( TYPE_FIELDREF, 1, constPoolLength + 1 );

                /* #5.2.2 CONST_NAMEAndTYPE {1tag, 2name_idx, 2desc_idx} */
                cp_insertBlock_U1_U2_U2( TYPE_NAMEANDTYPE, constPoolLength + 1, idxOfAttIntDesc );

                /* #5.2.3 CONST_UTF8 {1tag, 2name_idx, tab} // DESCRIPTOR u2: I */
                idxListOfFields.add( constPoolLength );
                cp_insertBlock_U1_U2_Tab( TYPE_UTF8, fieldNode.getName() );

                /* #5.2.4 CONST_INT {1tag, 4value} */
                insertCode( TYPE_INT ); // u1
                insertCode( toU4( 0 ) );

                constPoolLength++;
            }

            /* #6+ METHODS */
            else if ( fieldNode instanceof MethodNode )
            {
                /* #6.0 CONST_UTF8 DESC {(*PARA)RETURN} */
                cp_insertBlock_U1_U2_Tab( TYPE_UTF8, returnMethodDescCheckMain( fieldNode ) );

                /* #6.1 CONST_METHODREF {1tag, 2class_idx, 2nAt_idx} */
                cp_insertBlock_U1_U2_U2( TYPE_METHODREF, 1, constPoolLength + 1 );

                /* #6.2 CONST_NAMEAndTYPE {1tag, 2name_idx, 2desc_idx} */
                cp_insertBlock_U1_U2_U2( TYPE_NAMEANDTYPE, constPoolLength + 1, constPoolLength - 2 );
                // u2 = idx of object_string (always +1)
                // u2 = idx of method desc (always -2)

                /* #6.3 CONST_UTF8 {1tag, 2name_idx, tab} */
                idxListOfFieldMethod.add( constPoolLength );
                cp_insertBlock_U1_U2_Tab( TYPE_UTF8, fieldNode.getName() );

                if ( nodeIsMain( fieldNode ) )
                {
                    /* #6.4 CONST_UTF8 {1tag, 2name_idx, tab} // One Main Para */
                    cp_insertBlock_U1_U2_Tab( TYPE_UTF8, "args" );
                }
                else
                {
                    Objekt para = fieldNode.getObj().getParameter();
                    while ( para != null )
                    {
                        /* #6.4 CONST_UTF8 {1tag, 2name_idx, tab} // Each Para */
                        cp_insertBlock_U1_U2_Tab( TYPE_UTF8, para.getName() );
                        para = para.getNextObj();
                    }
                }
            }
            fieldNode = fieldNode.getLink();
        }

        /* Special temporary Variable for my shenanigans */
        /* #5.3.1 CONST_FIELDREF {1tag, 2class_idx, 2nAt_idx} */
        idxOfFuckYouTempVariable = constPoolLength;
        cp_insertBlock_U1_U2_U2( TYPE_FIELDREF, 1, constPoolLength + 1 );

        /* #5.3.2 CONST_NAMEAndTYPE {1tag, 2name_idx, 2desc_idx} */
        cp_insertBlock_U1_U2_U2( TYPE_NAMEANDTYPE, constPoolLength + 1, idxOfAttIntDesc );

        /* #5.3.3 CONST_UTF8 {1tag, 2name_idx, tab} // DESCRIPTOR u2: I */
        cp_insertBlock_U1_U2_Tab( TYPE_UTF8, "FuckYourTemp" );

        /* #5.3.4 CONST_INT {1tag, 4value} */
        insertCode( TYPE_INT ); // u1
        insertCode( toU4( 0 ) );

        constPoolLength++;

        /* #7 EXTRA */
        insertPrintlnCode();

        /* Special Attributes */

        /* #7.2 CONSTANT VALUE */
        idxOfAttConstValue = constPoolLength;
        cp_insertBlock_U1_U2_Tab( TYPE_UTF8, "ConstantValue" );

        /* #7.3 METHOD PARA */
        idxOfAttMethodPara = constPoolLength;
        cp_insertBlock_U1_U2_Tab( TYPE_UTF8, "MethodParameters" );

        /* #7.4 CODE */
        idxOfAttCode = constPoolLength;
        cp_insertBlock_U1_U2_Tab( TYPE_UTF8, "Code" );

        /* #END eval const pool length */
        insertCodeAt( toU2( constPoolLength ), lazyIdx_ConstPoolLength_U2 );
    }

    private int insertPrintlnCode()
    {
        /* #7.1.0 CLASS SYSTEM  */
        cp_insertBlock_U1_U2( TYPE_CLASS, constPoolLength + 1 );

        /* #7.1.1 UTF8 SYSTEM  */
        cp_insertBlock_U1_U2_Tab( TYPE_UTF8, "java/lang/System" );

        /* #7.1.2 CLASS PRINT_STREAM */
        cp_insertBlock_U1_U2( TYPE_CLASS, constPoolLength + 1 );

        /* #7.1.3 PRINT_STREAM */
        cp_insertBlock_U1_U2_Tab( TYPE_UTF8, "java/io/PrintStream" );

        /* #7.1.4 FIELDREF {1tag, 2class_idx, 2nAt_idx} */
        idxOfCallOut = constPoolLength;
        cp_insertBlock_U1_U2_U2( TYPE_FIELDREF, constPoolLength - 4, constPoolLength + 1 );
        // u2 = idx of System (always -4)
        // u2 = idx of nAt reference (always +1)

        /* #7.1.5 NAMEAndTYPE {1tag, 2name_idx, 2desc_idx} */
        cp_insertBlock_U1_U2_U2( TYPE_NAMEANDTYPE, constPoolLength + 1, constPoolLength + 2 );
        // u2 = idx of utf8 out reference (always +1)
        // u2 = idx of utf8 PrintStream reference (always +2)

        /* #7.1.6 UTF8 OUT  */
        cp_insertBlock_U1_U2_Tab( TYPE_UTF8, "out" );

        /* #7.1.7 UTF8 SYSTEM  */
        cp_insertBlock_U1_U2_Tab( TYPE_UTF8, "Ljava/io/PrintStream;" );

        /* #7.1.8 METHODREF Println {1tag, 2class_idx, 2nAt_idx} */
        idxOfCallPrintln = constPoolLength;
        cp_insertBlock_U1_U2_U2( TYPE_METHODREF, constPoolLength - 6, constPoolLength + 1 );
        // u2 = idx of PrintStream (always -6)
        // u2 = idx of name and Type (always +1)

        /* #7.1.9 NAMEAndTYPE Println {1tag, 2name_idx, 2desc_idx} */
        cp_insertBlock_U1_U2_U2( TYPE_NAMEANDTYPE, constPoolLength + 1, constPoolLength + 2 );
        // u2 = idx of utf println (always +)
        // u2 = idx of utf println type (always +)

        /* #7.1.6 UTF8 println  */
        cp_insertBlock_U1_U2_Tab( TYPE_UTF8, "println" );

        /* #7.1.7 UTF8 println type  */
        //        cp_insertBlock_U1_U2_Tab( TYPE_UTF8, "(Ljava/lang/String;)V" );
        cp_insertBlock_U1_U2_Tab( TYPE_UTF8, "(I)V" );

        /* #7.2 UTF8 Constant string  */
        idxOfConstHello = constPoolLength;
        cp_insertBlock_U1_U2( TYPE_STRING, constPoolLength + 1 );
        cp_insertBlock_U1_U2_Tab( TYPE_UTF8, "Hello and fuck you" );

        return constPoolLength;
    }

    /**
     * Only to be used in the constant pool for readability.
     */
    private void cp_insertBlock_U1_U2( byte flag, int idx )
    {
        insertCode( flag ); // u1
        insertCode( toU2( idx ) ); // u2
        constPoolLength++;
    }

    /**
     * Only to be used in the constant pool for readability.
     */
    private void cp_insertBlock_U1_U2_U2( final byte flag, final int idx1, final int idx2 )
    {
        insertCode( flag ); // u1
        insertCode( toU2( idx1 ) ); // u2
        insertCode( toU2( idx2 ) ); // u2
        constPoolLength++;
    }

    /**
     * Only to be used in the constant pool for readability.
     */
    private void cp_insertBlock_U1_U2_Tab( final byte flag, final String descriptor )
    {
        insertCode( flag ); // u1
        byte[] bName = descriptor.getBytes( StandardCharsets.UTF_8 );
        insertCode( toU2( bName.length ) ); // u2
        insertCode( bName ); // tab
        constPoolLength++;
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
        /* Number of fields: Consts + Fields + 1 Temp */
        insertCode( toU2( idxListOfFieldConst.size() + idxListOfFields.size() + 1 ) );

        /* 1. Constants first */
        for ( int i : idxListOfFieldConst )
        {
            /* ACCESS FLAG */
            insertCode( toU2( MOD_FINAL_STATIC_PUBLIC ) );

            /* NAME INDEX */
            insertCode( toU2( i ) );

            /* DESC INDEX */
            insertCode( toU2( idxOfAttIntDesc ) );

            /* ATTRIBUTE COUNT */
            insertCode( toU2( 1 ) );

            /* ATTRIBUTES: CONSTANT VALUE */
            insertCode( toU2( idxOfAttConstValue ) );
            insertCode( toU4( 2 ) );
            insertCode( toU2( i + 1 ) ); // const always +1
        }

        /* 2. Fields second */
        for ( int i : idxListOfFields )
        {
            /* ACCESS FLAG */
            insertCode( toU2( MOD_STATIC_PUBLIC ) );

            /* NAME INDEX */
            insertCode( toU2( i ) );

            /* DESC INDEX */
            insertCode( toU2( idxOfAttIntDesc ) );

            /* ATTRIBUTE COUNT */
            insertCode( toU2( 0 ) );
        }

        /* 3. Temp Variable for myself */

        /* ACCESS FLAG */
        insertCode( toU2( MOD_STATIC_PUBLIC ) );

        /* NAME INDEX */
        insertCode( toU2( idxOfFuckYouTempVariable + 2 ) );

        /* DESC INDEX */
        insertCode( toU2( idxOfAttIntDesc ) );

        /* ATTRIBUTE COUNT */
        insertCode( toU2( 0 ) );
    }

    /**
     * Here lies code to create the method pool.
     * Indexes in the pool start at #0 again.
     * Every method has its own method descriptor, regardless if it is already in the const pool.
     * Every entry looks like this:
     * .
     * method_info {
     * u2             access_flags;
     * u2             name_index;
     * u2             descriptor_index;
     * u2             attributes_count;
     * attribute_info attributes[attributes_count];
     * }
     * .
     * attribute_info {
     * u2             attribute_name_index;
     * u4             attribute_length;
     * u1             *_info[attribute_length]; // Is dependent on the attribute
     * }
     * .
     * MethodParameters_attribute {
     * u2             attribute_name_index;
     * u4             attribute_length;
     * u1             parameters_count;
     * {
     * u2 name_index;
     * u2 access_flags;
     * } parameters[parameters_count];
     * }
     */
    private void insertMethodPool()
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

        /* Method idxs in idxListOfFieldMethod were iterated like here */
        for ( int i : idxListOfFieldMethod )
        {
            /* ** Method_info *****************************************/

            /* ACCESS FLAG */
            insertCode( toU2( MOD_STATIC_PUBLIC ) );

            /* NAME INDEX */
            insertCode( toU2( i ) );

            /* DESC INDEX */
            insertCode( toU2( i - 3 ) );
            // desc_idx always 3 below name_idx

            if ( nodeIsMain( methodNode ) )
            {
                /* ATTRIBUTE COUNT: 1. MethodParas 2. Code */
                insertCode( toU2( 2 ) );

                /* ** 1. ATTRIBUTE MethodParameters **********************/

                /* #1 IDX of Attribute MethodParameters */
                insertCode( toU2( idxOfAttMethodPara ) );

                /* #2 LENGTH */
                insertCode( toU4( 0x05 ) ); // para count u1 and para entry 2*u2

                /* #3 PARA COUNT */
                insertCode( 0x01 );

                /* #4.1 PARA IDX */
                insertCode( toU2( i + 1 ) );

                /* #4.2 PARA MODIFIER */
                insertCode( toU2( 0x00 ) );
                /* ****************************************************/
            }
            else if ( methodNode.getObj().getParaCount() > 0 )
            {
                /* ATTRIBUTE COUNT: 1. MethodParas 2. Code */
                insertCode( toU2( 2 ) );

                /* ** 1. ATTRIBUTE MethodParameters **********************/

                /* #1 IDX of MethodParameters */
                insertCode( toU2( idxOfAttMethodPara ) );

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
                /* ATTRIBUTE COUNT: 1. CodeAtt, no MethodParameters */
                insertCode( toU2( 1 ) );
            }

            /* ** 2. ATTRIBUTE Code ***********************************/
            insertCodeAttribute( methodNode );
            /* ********************************************************/

            methodNode = methodNode.getLink();
        }
    }

    private int methodCodeAttLength_Bytes = 0;

    private int codeLength_Bytes = 0;

    private int lazyStack_Count = 0;

    /**
     * Inserts the code attribute into the byte code for one method.
     * Code_attribute {
     * u2             attribute_name_index;
     * u4             attribute_length;
     * u2             max_stack;
     * u2             max_locals;
     * u4             code_length;
     * u1             code[code_length];
     * u2             exception_table_length;
     * {
     * u2 start_pc;
     * u2 end_pc;
     * u2 handler_pc;
     * u2 catch_type;
     * } exception_table[exception_table_length];
     * u2             attributes_count;
     * attribute_info attributes[attributes_count];
     * }
     */
    private void insertCodeAttribute( final INode methodNode )
    {
        /* #1 IDX of Attribute Code */
        insertCode( toU2( idxOfAttCode ) ); //

        /* #2 Length of ATT (in bytes), EXCLUDING the initial 6 bytes */
        final int lazyIdx_MethodCodeAttLength_U4 = idx;
        insertEmptyU4();

        /* #3 MAX STACK u2 */
        final int lazyIdx_StackCount_U2 = idx;
        insertEmptyU2();
        methodCodeAttLength_Bytes += 2;

        /* #4 MAX LOCALS u2 */
        insertCode( toU2( calculateMaxLocals( methodNode ) ) );
        methodCodeAttLength_Bytes += 2;

        /* #5.1 CODE LENGTH u4 */
        int lazyIdx_codeLength_U4 = idx;
        insertEmptyU4();
        methodCodeAttLength_Bytes += 4;

        /* #6 CODE ENTRIES u1 */
        // produceFakeWorkingCode();
        produceCode( methodNode.getLeft(), methodNode );
        //        INode methodEntry = methodNode.getLeft();
        //        do
        //        {
        //            // There always has to be at least one statement.
        //            produceCode( methodEntry, methodNode );
        //        }
        //        while ( ( methodEntry = methodEntry.getLink() ) != null );
        if ( codeLength_Bytes == 0 )
        {
            compileError( String.format(
                            "Method content of \"%s\" can not be empty. Undefined local variables do not count.",
                            methodNode.getName() ) );
        }
        methodCodeAttLength_Bytes += codeLength_Bytes;

        /* #5.2 LAZY code length insert */
        insertCodeAt( toU4( codeLength_Bytes ), lazyIdx_codeLength_U4 );

        /* #7 EXCEPTIONS LENGTH u2 */
        insertEmptyU2();
        methodCodeAttLength_Bytes += 2;

        /* #8 ATT COUNT u2 */
        insertEmptyU2();
        methodCodeAttLength_Bytes += 2;

        /* Lazy CodeLength insert and reset tracker variables */
        insertCodeAt( toU4( methodCodeAttLength_Bytes ), lazyIdx_MethodCodeAttLength_U4 );
        insertCodeAt( toU2( lazyStack_Count ), lazyIdx_StackCount_U2 );

        methodCodeAttLength_Bytes = 0;
        codeLength_Bytes = 0;
        lazyStack_Count = 0;
    }

    /**
     * Returns the number of parameters and local variables for a method.
     * .
     * The value of the max_locals item gives the number of local variables
     * in the local variable array allocated upon invocation of this method (§2.6.1),
     * including the local variables used to pass parameters to the method on its invocation.
     * .
     * The greatest local variable index for a value of type long or double is max_locals - 2.
     * The greatest local variable index for a value of any other type is max_locals - 1.
     * --> int should be counted as 1 local?
     */
    private int calculateMaxLocals( INode methodNode )
    {
        int localsCount = 0;

        if ( nodeIsMain( methodNode ) )
        {
            localsCount = 1;
        }

        Objekt paraObj = methodNode.getObj().getSymTable().getHead();
        while ( paraObj != null )
        {
            localsCount++;
            paraObj = paraObj.getNextObj();
        }

        return localsCount;
    }

    /**
     * Fake function to generate predefined code for verification.
     * Prints out the first constant.
     */
    private void produceFakeWorkingCode()
    {
        codeLength_Bytes = 9;
        insertCode( 0xb2 ); // get static
        insertCode( toU2( idxOfCallOut ) ); // idx of out in const pool
        insertCode( 0x12 ); // ldc (load constant)
        insertCode( idxListOfFieldConst.get( 0 ) + 1 ); // idx of constant +1
        // insertCode( idxOfConstHello ); // Must change the method signature of println
        insertCode( 0xb6 ); // invoke virtual
        insertCode( toU2( idxOfCallPrintln ) ); // idx of println
        insertCode( 0xb1 ); // return void
    }

    /**
     * Produces the actual machine instructions for a given method
     * within the code attribute recursively.
     * Returns length of produced code in bytes.
     * Adds number of bytes produced to @(codeLength_Bytes).
     */
    private void produceCode( INode node, INode currMethodNode )
    {
        if ( node == null )
        {
            return;
        }

        checkFirstVisit( node );

        produceCode( node.getLeft(), currMethodNode );
        produceCode( node.getRight(), currMethodNode );

        switch ( node.getNodeClasz() )
        {
            case BINOPS:
                if ( node.subClaszEquals( ASSIGNMENT ) )
                {
                    /* If a class var */
                    if ( node.getLeft().getObj().objClazEquals( ObjektConst.CLASS_VAR ) )
                    {
                        insertCode( 0xb3 ); // putstatic: put var in field
                        insertCode( toU2( findIdxOfField( node.getLeft() ) ) ); // idx of field in const pool
                        codeLength_Bytes += 3;
                    }
                    /* If a class var */
                    else
                    {
                        insertCode( 0x36 ); // istore: stores int at local var_idx
                        insertCode( getIdxOfLocalVariableInMethod( currMethodNode, node.getLeft().getName() ) );
                        codeLength_Bytes += 2;
                    }
                }
                /* For cmp operators, remember idx of goto in tmpStack */
                else if ( node.subClaszEquals( EQUALS ) )
                {
                    insertCode( 0xA5 ); // if_acmpeq
                    lazy_idxOfElse_U2 = idxTempStack;
                    idxOfElse_In_code = codeLength_Bytes;
                    insertEmptyU2();
                    codeLength_Bytes += 3;
                }
                else if ( node.subClaszEquals( LESSTHAN ) )
                {
                    insertCode( 0xA1 ); // if_icmplt
                    lazy_idxOfElse_U2 = idxTempStack;
                    idxOfElse_In_code = codeLength_Bytes;
                    insertEmptyU2();
                    codeLength_Bytes += 3;
                }
                else if ( node.subClaszEquals( LESSEQUALSTHAN ) )
                {
                    insertCode( 0xA4 ); // if_icmple
                    lazy_idxOfElse_U2 = idxTempStack;
                    idxOfElse_In_code = codeLength_Bytes;
                    insertEmptyU2();
                    codeLength_Bytes += 3;
                }
                else if ( node.subClaszEquals( GREATERTHAN ) )
                {
                    insertCode( 0xA3 ); // if_icmpgt
                    lazy_idxOfElse_U2 = idxTempStack;
                    idxOfElse_In_code = codeLength_Bytes;
                    insertEmptyU2();
                    codeLength_Bytes += 3;
                }
                else if ( node.subClaszEquals( GREATEREQUALSTHAN ) )
                {
                    insertCode( 0xA2 ); // if_icmpge
                    lazy_idxOfElse_U2 = idxTempStack;
                    idxOfElse_In_code = codeLength_Bytes;
                    insertEmptyU2();
                    codeLength_Bytes += 3;
                }
                else if ( node.subClaszEquals( TIMES ) )
                {
                    insertCode( 0x68 ); // imul
                    codeLength_Bytes += 1;
                }
                else if ( node.subClaszEquals( DIV ) )
                {
                    insertCode( 0x6c ); // idiv
                    codeLength_Bytes += 1;
                }
                else if ( node.subClaszEquals( PLUS ) )
                {
                    insertCode( 0x60 ); // iadd
                    codeLength_Bytes += 1;
                }
                else if ( node.subClaszEquals( BO_MINUS ) )
                {
                    insertCode( 0x64 ); // isub
                    codeLength_Bytes += 1;
                }
                break;
            /* Is not identified as a constant within the method, is a METHOD_VAR. */
            case CONSTANT:
                /* Is not identified as a class_var within the method, is a METHOD_VAR. */
            case CLASS_VAR:
            {
                break;
            }
            case METHOD:
            {
                /* Special handling for println because we aint linking shit */
                if ( node.getObj().getName().equals( "println" ) && node.getObj().getParaCount() == 1
                                && node.getObj().getReturnType() != Type.Int )
                {

                    /* Store last var on stack temporarily */
                    insertCode( 0xb3 ); // putstatic: put var in tmp_var
                    insertCode( toU2( idxOfFuckYouTempVariable ) ); // idx of tmp_var in const pool

                    insertCode( 0xb2 ); // getstatic: get from constpool
                    insertCode( toU2( idxOfCallOut ) ); // idx of out in const pool
                    lazyStack_Count++;

                    insertCode( 0xb2 ); // getstatic: get from constpool
                    insertCode( toU2( idxOfFuckYouTempVariable ) ); // idx of tmp_var in const pool
                    lazyStack_Count++;

                    insertCode( 0xb6 ); // invoke virtual
                    insertCode( toU2( idxOfCallPrintln ) ); // idx of println
                    lazyStack_Count++;

                    codeLength_Bytes += 12;
                }
                /* Normal methods just get invoked with the right paras on the stack */
                else
                {
                    insertCode( 0xb8 ); // invokestatic: call static method
                    insertCode( toU2( getMethodIdxInPoolFromName( node ) ) );
                    lazyStack_Count++;
                    codeLength_Bytes += 3;
                }
                break;
            }
            /* Is a local variable. */
            case PARA:
            {
                /* Is not identified as a class_var within the method, is a METHOD_VAR. */
                break;
            }
            /* Is also a local variable, sadly can be a number or a constant. */
            case METHOD_VAR:
            {
                /* If a number: Place on stack */
                if ( node instanceof NumberNode )
                {
                    try
                    {
                        insertCode( 0x10 );
                        insertCode( Integer.parseInt( node.getName() ) );
                        lazyStack_Count++;
                        codeLength_Bytes += 2;
                    }
                    catch ( Exception e )
                    {
                        compileError( "Dev made a mistake: Shouldn't all numbers have a value?!" );
                    }
                }
                /* Else if a constant: Place on stack, cant be assigned */
                else if ( node.getObj().objClazEquals( ObjektConst.CONSTANT ) )
                {
                    insertCode( 0x12 ); // ldc: Load val from const_pool at idx
                    insertCode( findIdxOfConst( node ) );
                    lazyStack_Count++;
                    codeLength_Bytes += 2;
                    break;
                }
                else if ( node.getObj().objClazEquals( ObjektConst.CLASS_VAR ) )
                {
                    /* Ignore if left of the assignment, store else */
                    if ( !node.isPartOfAssignment() )
                    {
                        insertCode( 0xb2 ); // getstatic: load class_var on stack
                        insertCode( toU2( findIdxOfField( node ) ) ); // idx of class_var in const pool
                        lazyStack_Count++;
                        codeLength_Bytes += 3;
                    }
                    break;
                }
                /* Else if a local variable or a parameter */
                else if ( !node.isPartOfAssignment() )
                {
                    insertCode( 0x15 ); // iload
                    insertCode( getIdxOfLocalVariableInMethod( currMethodNode, node.getName() ) );
                    lazyStack_Count++;
                    codeLength_Bytes += 2;
                }
                break;
            }
            case IF:
            {
                /* On exiting if remember: [goto ?idxOfFin] = idxTmpStack, insertEmptyU2() */
                insertCode( 0xA7 ); // goto idx
                lazy_idxOfIfElseEnd_U2 = idxTempStack;
                idxOfIfElseEnd_In_code = codeLength_Bytes;
                insertEmptyU2();
                codeLength_Bytes += 3;

                /* On second enter ifElse (always after exit if): lazy insert codeLength_Byte+1 at lazy_idxOfElse_U2 */
                //                insertCodeAt( toU2( codeLength_Bytes - idxOfElse_In_code ), lazy_idxOfElse_U2 );

                int idx = 2;
                /* Offset of jump is current idx in bytecode */
                insertCodeAt( toU2( codeLength_Bytes - idxOfElse_In_code ), lazy_idxOfElse_U2 );
                break;
            }
            case IFELSE:
            {
                /* On exit ifElse (switch case): lazy insert codeLength_Byte+1 at lazy_idxOfIfElseEnd_U2 */
                insertCodeAt( toU2( codeLength_Bytes - idxOfIfElseEnd_In_code ), lazy_idxOfIfElseEnd_U2 );

                /* Insert tmpStack in code */
                flushTempStackInCode();
                break;
            }
            case WHILE:
            {

                break;
            }
            case RETURN:
            {
                if ( currMethodNode.getObj().getReturnType().equals( Type.Int ) )
                {
                    insertCode( 0xAC ); // ireturn
                    codeLength_Bytes += 1;
                }
                else
                {
                    insertCode( 0xB1 ); // return
                    codeLength_Bytes += 1;
                }

                break;
            }
            default:
            {
                compileError( "Dev made a mistake generating method code :( Maybe he missed implementing all cases" );
            }
        }

        if ( node.getLink() != null )
            produceCode( node.getLink(), currMethodNode );
    }

    /* Idx in tempStack for goto conditions. */

    private int lazy_idxOfElse_U2 = -1; // if condition is false -> goto else

    private int idxOfElse_In_code = -1;

    private int lazy_idxOfIfElseEnd_U2 = -1; // if condition is true and finished -> goto end of else

    private int idxOfIfElseEnd_In_code = -1;

    /**
     * Should ve used the visitor pattern but whatever by now.
     * .
     * Catches ifElse node, everything node below will be handled here.
     * 0. On first enter ifElse (not switch case): useTempStack = true
     * 1. On compare remember: [comp ?idxOfElse] = idxTmpStack, insertEmptyU2()
     * 2. On exiting if (in switch case) remember: [goto ?idxOfFin] = idxTmpStack, insertEmptyU2()
     * 2.1. On second enter ifElse  (not switch case): idxTmpStack[idxOfElse] = u2(codeLength_Bytes+1)
     * 3. On exit ifElse (switch case): idxTmpStack[idxOfFin] = u2(codeLength_Bytes)
     * 3.1 insert tmpStack in code
     * .
     * While?
     */
    private void checkFirstVisit( INode node )
    {
        if ( node.classEquals( NodeClasz.IFELSE ) )
        {
            useTempStack = true;
        }
    }

    /* Honestly i should use a better data structure for storing the idxs of consant pool items
     * but i dont care anymore, i am so close to finishing.
     * Its idx -2 because thats the realtive index in the constant pool, yeah i know ... */

    private int findIdxOfConst( INode node )
    {
        /* Find first Const */
        INode constNode = root.getLeft();
        while ( constNode.getLink() != null )
        {
            if ( constNode instanceof ConstantNode || constNode instanceof AssignmentNode )
            {
                break;
            }
            constNode = constNode.getLink();
        }

        for ( int i : idxListOfFieldConst )
        {
            if ( constNode instanceof AssignmentNode )
            {
                if ( node.getName().equals( constNode.getLeft().getName() ) )
                {
                    return i + 1;
                }
            }
            if ( node.getName().equals( constNode.getName() ) )
            {
                return i + 1;
            }
            constNode = constNode.getLink();
        }

        compileError( "ups 1" );
        return -1;
    }

    private int findIdxOfField( INode node )
    {
        /* Find first Const */
        INode fieldNode = root.getLeft();
        while ( fieldNode.getLink() != null )
        {
            if ( fieldNode instanceof ClassVarNode )
            {
                break;
            }
            fieldNode = fieldNode.getLink();
        }

        for ( int i : idxListOfFields )
        {
            if ( node.getName().equals( fieldNode.getName() ) )
            {
                return i - 2;
            }
            fieldNode = fieldNode.getLink();
        }

        compileError( "ups 2" );
        return -1;
    }

    private int getMethodIdxInPoolFromName( INode node )
    {
        /* Find first Method */
        INode methodNode = root.getLeft();
        while ( methodNode.getLink() != null )
        {
            if ( methodNode instanceof MethodNode )
            {
                break;
            }
            methodNode = methodNode.getLink();
        }

        for ( int i : idxListOfFieldMethod )
        {
            if ( node.getName().equals( methodNode.getName() ) && node.getObj().getParaCount() == methodNode.getObj()
                            .getParaCount() )
            {
                /* BUG cant find return type for method_call: void, no parameters */
                if ( node.getObj().getReturnType() == null || methodNode.getObj().getReturnType() == node.getObj()
                                .getReturnType() )
                {
                    return i - 2;
                }
            }
            methodNode = methodNode.getLink();
        }

        compileError( "ups 3" );
        return -1;
    }

    /**
     * Returns the numbered place of the parameter or local variable of a method.
     */
    private int getIdxOfLocalVariableInMethod( final INode methodNode, final String varName )
    {
        int varCnt = 0;
        if ( nodeIsMain( methodNode ) )
        {
            varCnt++;
        }
        Objekt variable = methodNode.getObj().getSymTable().getHead();
        while ( variable != null )
        {
            if ( variable.getName().equals( varName ) )
            {
                return varCnt;
            }
            varCnt++;
            variable = variable.getNextObj();
        }
        compileError( "Dev made a mistake, searching for a variable that is not in the method." );
        return -1;
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

    private void flushTempStackInCode()
    {
        useTempStack = false;
        insertCode( tempStack );
        this.tempStack = new byte[CODE_MAX];
        for ( int i = 0; i < CODE_MAX; i++ )
        {
            tempStack[i] = ESCAPE_SEQ;
        }
        this.idxTempStack = 0;
    }

    private void insertCode( int iin )
    {
        byte in = (byte) iin;
        if ( idx < CODE_MAX )
        {
            if ( useTempStack )
                tempStack[idxTempStack++] = in;
            else
                code[idx++] = in;
        }
        else
            compileError( "Your code is too long." );
    }

    private void insertCodeAt( int in, int at )
    {
        if ( idx < CODE_MAX )
        {
            if ( useTempStack )
                tempStack[at++] = (byte) in;
            else
                code[at++] = (byte) in;

        }
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
                if ( useTempStack )
                    tempStack[idxTempStack++] = b;
                else
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
                if ( useTempStack )
                    tempStack[at++] = b;
                else
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
