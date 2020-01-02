package SharedData;

import java.util.ArrayList;

public class Message {
    public static final int offer = 2;
    private final String teamName = "     <insert-team-name-here>    " ;   //32 bytes
    private byte type;
    private char[] hash;  //hash length is 40 chars
    private byte originalLength;
    private String originalStringStart;
    private String originalStringEnd;


    public Message(byte type, char[] hash, byte originalLength, String originalStringStart, String originalStringEnd) {
        this.type = type;
        this.hash = hash;
        this.originalLength = originalLength;
        this.originalStringStart = originalStringStart;
        this.originalStringEnd = originalStringEnd;
    }

    public byte[] getBytes() {
        String s = "";
        s += teamName;
        s += (char)type;
        s += hash.toString();
        s += (int)originalLength;
        s += originalStringStart + '\0' + originalStringEnd;
        return s.getBytes();
    }
}
