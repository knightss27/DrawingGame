import java.io.Serializable;
import java.util.Arrays;

public class Message<T> implements Serializable {

    public enum mType {
        JOIN,
        PLAYERS,
        LEAVE,
        DRAW,
        CHAT,
        ROUND,
        START,
        HINT
    }

    public int room;
    public mType type;
    public T data;

    public Message(int room, mType type, T data) {
        this.room = room;
        this.type = type;
        this.data = data;
    }

    @Override
    public String toString() {
        return "[" + this.room + ", " + this.type + ", " + this.data + "]";
    }
}
