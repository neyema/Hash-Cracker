package SharedData;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class Message {
    public static final byte discover = 1;
    public static final byte offer = 2;
    public static final byte request = 3;
    public static final byte acknowledge = 4;
    public static final byte negative_acknowledge = 5;
    public static final String ourTeamName = "        insert team name        ";
    private String teamName;   //32 bytes.there can be other messages with other teamName
    private byte type;
    private String hash;  //hash length is 40 chars
    private byte originalLength;
    private String originalStringStart;
    private String originalStringEnd;


    public Message(String teamName,byte type, String hash, byte originalLength, String originalStringStart, String originalStringEnd) throws Exception {
        if (teamName.length() > 32)
            throw new Exception("team name more tahn 32 chars");
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
        teamName = new String(Arrays.copyOfRange(messageAsBytes, 0, 32));
        type = messageAsBytes[32];
        hash = new String(Arrays.copyOfRange(messageAsBytes, 33, 73));
        originalLength = messageAsBytes[73];
        int lengthInInt = originalLength & 0xff;
        originalStringStart = new String(Arrays.copyOfRange(messageAsBytes, 74, 74+lengthInInt));
        originalStringEnd = new String(Arrays.copyOfRange(messageAsBytes, 74+lengthInInt, 74+2*lengthInInt));
    }

    public String getHash() {
        return hash;
    }

    public byte getType() { return type; }
    public void setType(byte newType){
        this.type = newType;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public String getOriginalStringEnd() {
        return originalStringEnd;
    }

    public String getOriginalStringStart() {
        return originalStringStart;
    }
    public void setOriginalStringStart(String originalStringStart) {
        this.originalStringStart = originalStringStart;
    }

    public byte[] getBytes() {
        String s = "";
        s += teamName;
        s += (char)type;
        s += hash;
        s += (char)originalLength;
        s += originalStringStart;
        s += originalStringEnd;
        return s.getBytes(StandardCharsets.UTF_8);
    }

}
