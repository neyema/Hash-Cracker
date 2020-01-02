package SharedData;

import java.util.ArrayList;
import java.util.Arrays;

public class Message {
    public static final byte discover = 1;
    public static final byte offer = 2;
    public static final byte request = 3;
    public static final byte acknowledge = 4;
    public static final byte negative_acknowledge = 5;
    private String teamName;   //32 bytes.there can be other messages with other teamName
    private byte type;
    private char[] hash;  //hash length is 40 chars
    private byte originalLength;
    private String originalStringStart;
    private String originalStringEnd;


    public Message(String teamName,byte type, char[] hash, byte originalLength, String originalStringStart, String originalStringEnd) {
        this.teamName = teamName;
        this.type = type;
        this.hash = hash;
        this.originalLength = originalLength;
        this.originalStringStart = originalStringStart;
        this.originalStringEnd = originalStringEnd;
    }

    /*
     0-31 is team name
     32 is message type
     33-72 is hash
     73 is length of originalString
     74-(74+l) is the startSearching word
     (74+l+1)-(74+2l+1) is the endSearching word
    this function build the message from bytes
    */
    public Message(byte[] messageAsBytes){
        teamName = new String(Arrays.copyOfRange(messageAsBytes, 0, 31));
        type = messageAsBytes[32];
        String hashAsString = new String(Arrays.copyOfRange(messageAsBytes, 33, 72));
        hash = hashAsString.toCharArray();
        originalLength = messageAsBytes[73];
        originalStringStart = new String(Arrays.copyOfRange(messageAsBytes, 74, 74+originalLength - 1));
        originalStringEnd = new String(Arrays.copyOfRange(messageAsBytes, 74+originalLength, 74+2*originalLength - 1));
    }

    public void setType(byte newType){
        this.type = newType;
    }

    public byte getType() { return type; }

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
