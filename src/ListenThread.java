import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.net.SocketException;

public class ListenThread extends Thread {
    Socket socket;
    ObjectInputStream objectInputStream;

    public ListenThread(Socket socket, ObjectInputStream objectInputStream) {
        this.socket = socket;
        this.objectInputStream = objectInputStream;
    }

    public void handleMessage(Message message) {
    }

    @Override
    public void run() {
        System.out.println("- Listening thread started");
        while (objectInputStream != null && !socket.isClosed()) {
            Message message = null;
            try {
                message = (Message) objectInputStream.readObject();
            } catch (IOException | ClassNotFoundException e) {
                if (e instanceof SocketException) {
                    System.out.println("Resolving dropped socket, peacefully leaving.");
                    message = new Message<>(-1, Message.mType.LEAVE, "");
                }
            }
            handleMessage(message);
        }
    }
}