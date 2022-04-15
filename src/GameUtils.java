import java.util.Random;

public class GameUtils {
    public static String generateNewRoomCode() {
        // Generate six digit random number.
        Random rnd = new Random();
        int number = rnd.nextInt(999999);

        // Convert to six digit string.
        return String.format("%06d", number);
    }
}
