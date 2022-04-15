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

    DrawingGUI gui;
//    boolean isAllowedToDraw = false;

    public GameClient() {
        try {
            // Connect to server
            Socket socket = new Socket("localhost", 12345);
            InputStream inputStream = socket.getInputStream();
            OutputStream outputStream = socket.getOutputStream();

            BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);

            ObjectOutputStream objectOutputStream = new ObjectOutputStream(bufferedOutputStream);
            objectOutputStream.flush();

            ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);

            LinkedBlockingQueue<Message> messagesToSend = new LinkedBlockingQueue<>();

            gui = new DrawingGUI(messagesToSend);

            SendThread sendThread = new SendThread(messagesToSend, objectOutputStream);
            sendThread.start();

            ListenThread listenThread = new ClientListenThread(socket, objectInputStream);
            listenThread.start();

            messagesToSend.add(new Message(111, Message.mType.JOIN, GameUtils.generateNewRoomCode()));

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
                // Temporarily only allow second join to draw.
                if (message.arrayData.length == 1) {
                    gui.isAllowedToDraw = true;
                }
            }

            if (message.type == Message.mType.DRAW) {
                gui.handleNetworkedDrawing(message.arrayData);
            }

            if (message.type == Message.mType.CHAT) {
                gui.handleChat(message);
            }
        }
    }
}
