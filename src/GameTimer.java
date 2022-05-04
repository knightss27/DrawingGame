public class GameTimer extends Thread {

    public static final int GAME_LENGTH = 60;

    public Runnable hintUpdateCallback;
    public Runnable gameEndCallback;
    public int hintLength;
    public int currentTime = 0;

    public GameTimer(int hintLength, Runnable hintUpdateCallback, Runnable gameEndCallback) {
        this.hintLength = hintLength;
        this.hintUpdateCallback = hintUpdateCallback;
        this.gameEndCallback = gameEndCallback;
    }

    public int getCurrentTime() {
        return GAME_LENGTH-currentTime;
    }

    @Override
    public void run() {
        try {
            int interval = GAME_LENGTH / (hintLength - 1);
            for (currentTime = 0; currentTime < GAME_LENGTH; currentTime++) {
                Thread.sleep(1000);
                if (currentTime % interval == 0) {
                    hintUpdateCallback.run();
                }
            }
            gameEndCallback.run();
        } catch (InterruptedException e) {
            System.out.println("GameTimer ended.");
        }
    }
}
