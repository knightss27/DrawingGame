import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

public class GameServer {
    public static void main(String[] args) {
        GameServer server = new GameServer();
        server.run();
    }

    public void run() {
        try {
            // Server listening
            int port = 12345;
            ServerSocket serverSocket = new ServerSocket(port);
            System.out.println("Server listening on port " + port);

            LinkedBlockingQueue<Socket> sockets = new LinkedBlockingQueue<>();
            ConnectionThread connectionThread = new ConnectionThread(serverSocket, sockets);
            connectionThread.start();

            HashMap<Integer, List<ClientConnection>> rooms = new HashMap<>();

            while (true) {
                Socket socket = sockets.take();
                new ClientConnection(socket, rooms);
                System.out.println("Server created new connection");
            }
        } catch (IOException ioe) {
            System.out.println("ACK! ACK!! It's an Exception!!");
            System.out.println(ioe);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static class ConnectionThread extends Thread {
        ServerSocket serverSocket;
        LinkedBlockingQueue<Socket> connections;

        ConnectionThread(ServerSocket serverSocket, LinkedBlockingQueue<Socket> connections) {
            this.serverSocket = serverSocket;
            this.connections = connections;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    Socket socket = serverSocket.accept();

                    connections.add(socket);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
