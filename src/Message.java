import java.io.Serializable;
import java.util.Arrays;

public class Message<T> implements Serializable {

    /**
     *  JOIN - Player <br>
     *  PLAYERS - Player[] <br>
     *  LEAVE <br>
     *  DRAW - int[] <br>
     *  CHAT - String <br>
     *  ROUND - Player <br>
     *  START <br>
     *  HINT - String <br>
     *  BRUSH - int[]
     */
    public enum mType {
        JOIN,
        PLAYERS,
        LEAVE,
        DRAW,
        CHAT,
        ROUND,
        START,
        HINT,
        BRUSH // [width, color.getRGB()]
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
        if (this.type == mType.PLAYERS) {
            return "[" + this.room + ", " + this.type + ", " + Arrays.toString((Player[]) this.data) + "]";
        }
        return "[" + this.room + ", " + this.type + ", " + this.data + "]";
    }
}
