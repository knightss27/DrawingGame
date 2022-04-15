import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

public class ClientConnection {
    ListenThread listenThread;
    SendThread sendThread;
    Socket socket;
    ObjectInputStream objectInputStream;
    ObjectOutputStream objectOutputStream;
    HashMap<Integer, List<ClientConnection>> rooms;
    boolean hasJoinedRoom = false;
    String clientUsername = "UserName";
    int clientId = (int) (Math.random() * 100000);

    LinkedBlockingQueue<Message> messagesToSend = new LinkedBlockingQueue<>();

    public ClientConnection(Socket socket, HashMap<Integer, List<ClientConnection>> rooms) {
        System.out.println("- Creating new client connection");
        this.socket = socket;
        this.rooms = rooms;

        try {
            InputStream inputStream = socket.getInputStream();
            BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
            this.objectInputStream = new ObjectInputStream(bufferedInputStream);

            OutputStream outputStream = socket.getOutputStream();
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);
            this.objectOutputStream = new ObjectOutputStream(bufferedOutputStream);
            objectOutputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // TODO
        this.listenThread = new ServerListenThread(socket, objectInputStream);
        listenThread.start();

        this.sendThread = new SendThread(messagesToSend, objectOutputStream);
        sendThread.start();
    }

    public void sendMessage(Message message) {
        messagesToSend.add(message);
    }

    private void sendToRoom(Message message, boolean includeSender) {
        List<ClientConnection> messages = rooms.get(message.room);
        for (ClientConnection c : messages) {
            if (!includeSender && c.clientId == this.clientId) {
                continue;
            }
            c.sendMessage(message);
        }
    }

    private class ServerListenThread extends ListenThread {

        public ServerListenThread(Socket socket, ObjectInputStream objectInputStream) {
            super(socket, objectInputStream);
        }

        @Override
        public void handleMessage(Message message) {
            System.out.println("SERVER RECEIVED: " + message);
            if (message.type == Message.mType.JOIN) {
                Message<String> m = (Message<String>) message;

                // Add ourselves to the room
                if (!rooms.containsKey(message.room)) {
                    rooms.put(m.room, new LinkedList<>());
                }
                List<ClientConnection> room = rooms.get(m.room);
                room.add(ClientConnection.this);
                hasJoinedRoom = true;
                if (m.data.length() > 0) {
                    clientUsername = m.data;
                }

                // Send out a new message about the room
                String[] ids = new String[room.size()];
                for (int i = 0; i < room.size(); i++) {
                    ids[i] = room.get(i).clientUsername;
                }
                sendToRoom(new Message<>(message.room, Message.mType.PLAYERS, ids), true);
            }

            if (message.type == Message.mType.LEAVE) {
                List<ClientConnection> room = rooms.get(message.room);
                for (ClientConnection c : room) {
                    if (c.clientId == clientId) {
                        room.remove(c);
                        break;
                    }
                }
                sendToRoom(new Message<>(message.room, Message.mType.LEAVE, "" + clientId), true);
            }

            if (message.type == Message.mType.DRAW || message.type == Message.mType.CHAT) {
                sendToRoom(message, false);
            }
        }
    }


}
