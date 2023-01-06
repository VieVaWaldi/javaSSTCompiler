package compiler.bytecode;

public class ByteCodeFlags
{
    //@formatter:off
    static byte EMPTY = 0x00;

    static int[] CAFE_BABE = { 0xCA, 0xFE, 0xBA, 0xBE };

    static byte MINOR_VERSION = 0x00;
    static byte MAJOR_VERSION = 0x34;

    static byte TYPE_UTF8 = 0x01;
    static byte TYPE_INT = 0x03;
    static byte TYPE_CLASS = 0x07;
    static byte TYPE_FIELREF = 0x09;
    static byte TYPE_METHREF = 0x0A;
    static byte TYPE_NANDT = 0x0C;

    static byte MOD_STATIC_PUBLIC = 0x09;
    static byte MOD_FINAL_STATIC_PUBLIC = 0x19;
    static byte MOD_SUPER_PUBLIC = 0x21;
    //@formatter:on
}
