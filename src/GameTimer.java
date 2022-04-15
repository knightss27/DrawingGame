public class GameTimer extends Thread {

    public static final int GAME_LENGTH = 20;

    public Runnable hintUpdateCallback;
    public Runnable gameEndCallback;
    public int hintLength;

    public GameTimer(int hintLength, Runnable hintUpdateCallback, Runnable gameEndCallback) {
        this.hintLength = hintLength;
        this.hintUpdateCallback = hintUpdateCallback;
        this.gameEndCallback = gameEndCallback;
    }

    @Override
    public void run() {
        try {
            int interval = GAME_LENGTH * 1000 / (hintLength - 2);
            for (int i = 0; i < hintLength-2; i++) {
                Thread.sleep(interval);
                hintUpdateCallback.run();
            }
            gameEndCallback.run();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
