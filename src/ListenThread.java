import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

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
        while (!socket.isClosed()) {
            Message message = null;
            try {
                message = (Message) objectInputStream.readObject();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
            handleMessage(message);
        }
    }
}