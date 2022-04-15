import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
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

    public Player[] players;

    public int currentRound = -1;
    public String currentHint = "";
    public Player currentDrawer;

    boolean isFirstPlayer = false;

    public GameClient() {

        player = new Player(-1, GameUtils.generateNewRoomCode());

        try {
            // Connect to server
            Socket socket = new Socket("localhost", 12345);
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

            messagesToSend.add(new Message(111, Message.mType.JOIN, player));

//            objectInputStream.close();
//            objectOutputStream.close();
//            socket.close();
        }
        catch (IOException ioe) {
            System.out.println("ACK! ACK!! It's an Exception!!");
            System.out.println(ioe);
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


                // Temporarily only allow third join to start the game.
                if (m.data.length == 1) {
                    isFirstPlayer = true;
                } else if (m.data.length == 2 && isFirstPlayer) {
                    messagesToSend.add(new Message<>(m.room, Message.mType.START, player));
                }
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
        }
    }
}
