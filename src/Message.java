import java.io.Serializable;
import java.util.Arrays;

public class Message implements Serializable {

    public enum mType {
        JOIN,
        PLAYERS,
        LEAVE,
        DRAW,
        CHAT
    }

    public int room;
    public mType type;
    public String text;
    public int[] arrayData;

    public Message(int room, mType type, String text) {
        this.room = room;
        this.type = type;
        this.text = text;
    }

    public Message(int room, mType type, int[] arrayData) {
        this.room = room;
        this.type = type;
        this.arrayData = arrayData;
        this.text = Arrays.toString(arrayData);
    }

    @Override
    public String toString() {
        return "[" + this.room + ", " + this.type + ", " + this.text + "]";
    }
}
