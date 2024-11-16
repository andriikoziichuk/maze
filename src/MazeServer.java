import java.io.*;
import java.net.*;
import java.util.*;

/*
    Written by andriikoziichuk
 */
public class MazeServer {
    public static final int PORT = 8001;
    public static final int WIDTH = 40;
    public static final int HEIGHT = 20;
    public static final char WALL = '#';
    public static final char EMPTY = '.';
    public static final char DIAMOND = '*';

    public boolean running = true;
    private final char[][] maze;
    private final Map<Character, Player> players;
    private final List<ClientHandler> clients = new ArrayList<>();

    public static void main(String[] args) throws IOException {
        new MazeServer().start();
    }

    public MazeServer() {
        maze = new char[HEIGHT][WIDTH];
        players = new HashMap<>();
        generateMaze();
    }

    private void generateMaze() {
        // Заповнення лабіринту порожніми комірками
        for (int i = 0; i < HEIGHT; i++) {
            for (int j = 0; j < WIDTH; j++) {
                maze[i][j] = EMPTY;
            }
        }
        // Додавання стін та діамантів
        Random random = new Random();
        int walls = 80 + random.nextInt(41); // Від 80 до 120 стін
        for (int i = 0; i < walls; i++) {
            int x = random.nextInt(WIDTH);
            int y = random.nextInt(HEIGHT);
            maze[y][x] = WALL;
        }
        int diamonds = 10; // Кількість діамантів
        for (int i = 0; i < diamonds; i++) {
            int x = random.nextInt(WIDTH);
            int y = random.nextInt(HEIGHT);
            if (maze[y][x] == EMPTY) {
                maze[y][x] = DIAMOND;
            }
        }
    }

    public void start() throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("Сервер запущено на порту " + PORT);
        while (true) {
            Socket clientSocket = serverSocket.accept();
            var clientHandler = new ClientHandler(this, clientSocket);
            clients.add(clientHandler);
            clientHandler.start();

            if (!running) break;
        }
    }

    public Map<Character, Player> getPlayers() {
        return players;
    }

    public char[][] getMaze() {
        return maze;
    }

    public List<ClientHandler> getClients() {
        return clients;
    }
}
