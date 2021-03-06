import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

public class ClientConnection {
    ListenThread listenThread;
    SendThread sendThread;

    GameTimer gameTimer;

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
    boolean guessedCorrectly = false;
    int totalCorrectGuessesReceived = 0;
    String currentHint;
    int hintLength = 0;
    boolean isGameOver = false;

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


    public void startTimer(String newWord) {
        if (gameTimer != null) {
            gameTimer.interrupt();
        }

        gameTimer = new GameTimer(newWord.length(), () -> {
            System.out.println("Hint update callback.");

            currentHint = generateNextHint();

            System.out.println("New hint created: " + currentHint);
            sendToRoom(new Message<>(currentRoom, Message.mType.HINT, currentHint), true);
        }, () -> {
            System.out.println("Game end callback.");
            isGameOver = true;
            sendToRoom(new Message<>(currentRoom, Message.mType.HINT, currentWord), true);
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            sendToNewRound(currentRoom);
        });
        gameTimer.start();
    }

    private String generateNextHint() {
        int randomSpot = (int) (Math.random() * currentWord.length() - hintLength);
        int space = 0;

        for (int i = 0; i < currentHint.length(); i++) {
            String character = Character.toString(currentHint.charAt(i));
            if (character.equals("_")) {
                if (space == randomSpot) {
                    currentHint = currentHint.substring(0, i) + currentWord.charAt(i) + currentHint.substring(i+1);
                }
                space++;
            }
        }
        hintLength++;
        return currentHint;
    }

    public void sendMessage(Message message) {
        messagesToSend.add(message);
    }

    public void sendToNextRound(Player playerDrawing, String word) {
        currentRound++;
        currentWord = word;
        currentHint = word.replaceAll("[a-zA-Z0-9_]", "_");
        currentPlayer = playerDrawing;
        guessedCorrectly = false;
        totalCorrectGuessesReceived = 0;
        hintLength = 0;

        System.out.println("Sending to next round. Is drawing? " + currentPlayer.equals(player));

        sendMessage(new Message<>(currentRoom, Message.mType.CHAT, playerDrawing.name + " is now drawing!"));
        sendMessage(new Message<>(currentRoom, Message.mType.ROUND, playerDrawing));
        sendMessage(new Message<>(currentRoom, Message.mType.HINT, playerDrawing.equals(player) ? word : currentHint));

        if (playerDrawing.equals(player)) {
            startTimer(currentWord);
        }
    }

    private void sendToRoom(Message message, boolean includeSender) {
        List<ClientConnection> messages = rooms.get(message.room);
        for (ClientConnection c : messages) {
            if (!includeSender && c.player.equals(this.player)) {
                continue;
            }
            if (message.type == Message.mType.HINT && currentPlayer.equals(c.player)) {
                continue;
            }
            c.sendMessage(message);
        }
    }

    private Player[] getPlayersInRoom() {
        List<ClientConnection> room = rooms.get(currentRoom);
        Player[] ids = new Player[room.size()];
        for (int i = 0; i < room.size(); i++) {
            ids[i] = room.get(i).player;
        }
        return ids;
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
                List<ClientConnection> room = rooms.get(currentRoom);
                for (ClientConnection c : room) {
                    if (c.player.equals(player)) {
                        room.remove(c);
                        break;
                    }
                }

                if (message.room == -1) {
                    objectInputStream = null;
                } else {
                    sendToRoom(new Message<>(currentRoom, Message.mType.LEAVE, player), true);
                }
            }

            if (message.type == Message.mType.START) {
                Message<Player> m = (Message<Player>) message;
                List<ClientConnection> clients = rooms.get(message.room);
                if (m.data.equals(clients.get(0).player)) {
                    sendToNewRound(m.room);
                }
            }

            if (message.type == Message.mType.DRAW) {
                sendToRoom(message, false);
            }

            if (message.type == Message.mType.BRUSH) {
                sendToRoom(message, false);
            }

            if (message.type == Message.mType.CHAT && !getCurrentPlayerConnection().isGameOver) {
                Message<String> m = (Message<String>) message;

                boolean justGuessedCorrect = false;

                if (currentRound >= 0) {
                    if (currentPlayer.equals(player)) {
                        m.data = ": I *love* this game!";
                    } else {
                        if (m.data.equalsIgnoreCase(currentWord)) {
                            m.data = " guessed the word!";
                            justGuessedCorrect = true;
                        } else {
                            m.data = ": " + m.data;
                        }
                    }
                } else {
                    m.data = ": " + m.data;
                }

                m.data = player.name + m.data;
                sendToRoom(m, true);

                if (justGuessedCorrect) {
                    guessedCorrectly = true;
                    ClientConnection current = getCurrentPlayerConnection();
                    System.out.println("Thinks that " + current.player.name + " is timer.");
                    int currentTime = getCurrentPlayerConnection().gameTimer.getCurrentTime();
                    System.out.println("Guessed at time: " + currentTime);
                    player.points += 300 * ((double) currentTime/GameTimer.GAME_LENGTH) + 100;
                    getCurrentPlayerConnection().checkWithNewCorrectGuess();
                }
            }
        }
    }

    public void checkWithNewCorrectGuess() {
        totalCorrectGuessesReceived++;
        System.out.println("Total correct guesses: " + totalCorrectGuessesReceived);
        if (totalCorrectGuessesReceived == rooms.get(currentRoom).size()-1) {

            Player[] players = getPlayersInRoom();
            System.out.println("Sending players: " + Arrays.toString(players));

            sendToRoom(new Message<>(currentRoom, Message.mType.PLAYERS, getPlayersInRoom()), true);
            sendToNewRound(currentRoom);
        }
    }

    private ClientConnection getCurrentPlayerConnection() {
        for (ClientConnection c : rooms.get(currentRoom)) {
            if (c.player.equals(currentPlayer)) {
                return c;
            }
        }
        throw new NoSuchElementException("No matching current player found!");
    }

    private void sendToNewRound(int room) {
        System.out.println(player.id + ": Attempting to send to new round.");
        String newWord = GameUtils.generateNewWord();
        List<ClientConnection> clients = rooms.get(room);
        Player nextPlayer = clients.get((currentRound + 1) % clients.size()).player;

        if (gameTimer != null) {
            gameTimer.interrupt();
        }
        for (ClientConnection c : clients) {
            c.isGameOver = false;
            c.sendToNextRound(nextPlayer, newWord);
        }
    }
}
