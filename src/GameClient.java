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

    public String userName;
    public int room = -1;
    public boolean isAllowedToDraw = false;

    public GameClient() {

        userName = GameUtils.generateNewRoomCode();

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

            messagesToSend.add(new Message(111, Message.mType.JOIN, userName));

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
                Message<String[]> m = (Message<String[]>) message;
                // Temporarily only allow first join to draw.
                if (m.data.length == 1) {
                    isAllowedToDraw = true;
                }

                if (room == -1) {
                    room = message.room;
                }
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
