import javax.swing.*;
import java.io.*;
import java.net.ConnectException;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.LinkedBlockingQueue;

public class GameClient {
    public static void main(String[] args)
    {
        new GameClient();
    }

    public LinkedBlockingQueue<Message> messagesToSend = new LinkedBlockingQueue<>();
    DrawingGUI gui;

    public Player player;
    public int room = -1;
    public boolean isAllowedToDraw = false;

    public Player[] players = new Player[]{};

    public int currentRound = -1;
    public String currentHint = "";
    public Player currentDrawer;

    boolean isFirstPlayer = false;
    boolean startedGame = false;

    public GameClient() {
        player = new Player(-1, GameUtils.generateNewRoomCode());
        startClient();
    }

    private void startClient() {
        try {
            Socket socket = new Socket("cs.catlin.edu", 8007);
            InputStream inputStream = socket.getInputStream();
            OutputStream outputStream = socket.getOutputStream();

            BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);

            ObjectOutputStream objectOutputStream = new ObjectOutputStream(bufferedOutputStream);
            objectOutputStream.flush();
            ObjectInputStream objectInputStream = new ObjectInputStream(bufferedInputStream);

            gui = new DrawingGUI(this);

            SendThread sendThread = new SendThread(messagesToSend, objectOutputStream);
            sendThread.start();

            ListenThread listenThread = new ClientListenThread(socket, objectInputStream);
            listenThread.start();

            player.name = JOptionPane.showInputDialog("Set your username!");
//            player.name = Integer.toString(((int) (1000 * Math.random())));

            messagesToSend.add(new Message(111, Message.mType.JOIN, player));
        }
        catch (IOException ioe) {
            if (ioe instanceof ConnectException) {
                System.out.println("Failed to connect to server. Retrying in 10 seconds");
                try {
                    Thread.sleep(10000);
                    startClient();
                } catch (InterruptedException e) {
                    System.out.println("Exited while sleeping for retry");
                }
            } else {
                System.out.println("ACK! ACK!! It's an Exception!!");
                System.out.println(ioe);
            }
        }
    }

    private class ClientListenThread extends ListenThread {
        public ClientListenThread(Socket socket, ObjectInputStream objectInputStream) {
            super(socket, objectInputStream);
        }

        @Override
        public void handleMessage(Message message) {
            System.out.println("CLIENT RECEIVED: " + message);

            if (message.type == Message.mType.PLAYERS) {
                Message<Player[]> m = (Message<Player[]>) message;

                if (player.id == -1) {
                    player = m.data[m.data.length-1];
                }

                if (room == -1) {
                    room = message.room;
                }

                players = m.data;

                // Temporarily only allow second join to start the game.
                if (m.data.length == 1 && !isFirstPlayer) {
                    isFirstPlayer = true;
                } else if (m.data.length == 2 && isFirstPlayer && !startedGame) {
                    messagesToSend.add(new Message<>(m.room, Message.mType.START, player));
                    startedGame = true;
                }

                gui.updatePlayers();
            }

            if (message.type == Message.mType.ROUND) {
                Message<Player> m = (Message<Player>) message;
                currentRound++;
                currentDrawer = m.data;
                gui.handleRound(m.data);
            }

            if (message.type == Message.mType.HINT) {
                Message<String> m = (Message<String>) message;
                currentHint = m.data;
                gui.handleHint(m.data);
            }

            if (message.type == Message.mType.DRAW) {
                Message<int[]> m = (Message<int[]>) message;
                gui.handleNetworkedDrawing(m.data);
            }

            if (message.type == Message.mType.CHAT) {
                gui.handleChat(message);
            }

            if (message.type == Message.mType.BRUSH) {
                Message<int[]> m = (Message<int[]>) message;
                gui.handleBrush(m.data);
            }

            if (message.type == Message.mType.LEAVE) {
                if (message.room != -1) {
                    System.out.println("Should handle leave.");
                } else {
                    objectInputStream = null;
                }
            }
        }
    }
}
