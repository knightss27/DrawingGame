import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.concurrent.LinkedBlockingQueue;

public class SendThread extends Thread {

    LinkedBlockingQueue<Message> messagesToSend;
    ObjectOutputStream objectOutputStream;

    SendThread(LinkedBlockingQueue<Message> messagesToSend, ObjectOutputStream objectOutputStream) {
        this.messagesToSend = messagesToSend;
        this.objectOutputStream = objectOutputStream;
    }

    @Override
    public void run() {
        while (true) {
            try {
                Message message = messagesToSend.take();
                objectOutputStream.writeObject(message);
                objectOutputStream.flush();
                objectOutputStream.reset();
            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
            }
        }
    }
}