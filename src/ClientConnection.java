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

    LinkedBlockingQueue<Message> messagesToSend = new LinkedBlockingQueue<>();

    Player player = new Player((int) (Math.random() * 100000), "Username");

    int currentRoom = -1;
    int currentRound = -1;
    String currentWord = null;
    Player currentPlayer;

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

        this.listenThread = new ServerListenThread(socket, objectInputStream);
        listenThread.start();

        this.sendThread = new SendThread(messagesToSend, objectOutputStream);
        sendThread.start();
    }

    public void sendMessage(Message message) {
        messagesToSend.add(message);
    }

    public void sendToNextRound(Player playerDrawing, String word) {
        currentRound++;
        currentWord = word;
        currentPlayer = playerDrawing;
        sendMessage(new Message<>(currentRoom, Message.mType.ROUND, playerDrawing));
        sendMessage(new Message<>(currentRoom, Message.mType.HINT, playerDrawing.equals(player) ? word : "_".repeat(word.length())));
    }

    private void sendToRoom(Message message, boolean includeSender) {
        List<ClientConnection> messages = rooms.get(message.room);
        for (ClientConnection c : messages) {
            if (!includeSender && c.player.equals(this.player)) {
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
                Message<Player> m = (Message<Player>) message;

                // Add ourselves to the room
                if (!rooms.containsKey(message.room)) {
                    rooms.put(m.room, new LinkedList<>());
                }
                List<ClientConnection> room = rooms.get(m.room);
                room.add(ClientConnection.this);
                hasJoinedRoom = true;

                player.name = m.data.name;

                // Send out a new message about the room
                Player[] ids = new Player[room.size()];
                for (int i = 0; i < room.size(); i++) {
                    ids[i] = room.get(i).player;
                }
                sendToRoom(new Message<>(message.room, Message.mType.PLAYERS, ids), true);

                currentRoom = m.room;
            }

            if (message.type == Message.mType.LEAVE) {
                List<ClientConnection> room = rooms.get(message.room);
                for (ClientConnection c : room) {
                    if (c.player.equals(player)) {
                        room.remove(c);
                        break;
                    }
                }
                sendToRoom(new Message<>(message.room, Message.mType.LEAVE, player), true);
            }

            if (message.type == Message.mType.START) {
                Message<Player> m = (Message<Player>) message;
                List<ClientConnection> clients = rooms.get(message.room);
                if (m.data.equals(clients.get(1).player)) {
                    // Set all Client Connections to have updated round data.
                    String newWord = GameUtils.generateNewWord();
                    for (ClientConnection c : clients) {
                        c.sendToNextRound(clients.get((currentRound + 1) % clients.size()).player, newWord);
                    }
                }
            }

            if (message.type == Message.mType.DRAW) {
                sendToRoom(message, false);
            }

            if (message.type == Message.mType.CHAT) {
                Message<String> m = (Message<String>) message;

                if (currentRound >= 0) {
                    if (currentPlayer.equals(player)) {
                        m.data = ": I'm dumb!";
                    } else {
                        if (m.data.equals(currentWord)) {
                            m.data = " guessed the word!";
                        } else {
                            m.data = ": " + m.data;
                        }
                    }
                } else {
                    m.data = ": " + m.data;
                }

                m.data = player.name + m.data;
                sendToRoom(m, true);
            }
        }
    }


}
