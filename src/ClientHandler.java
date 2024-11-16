import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.stream.IntStream;

class ClientHandler extends Thread {
    private final MazeServer mazeServer;
    private final Socket socket;
    private PrintWriter out;
    private char playerSymbol;
    private String playerName;
    private final long startTime;
    private boolean isMagicMode;

    public ClientHandler(MazeServer mazeServer, Socket socket) {
        this.mazeServer = mazeServer;
        this.socket = socket;
        this.startTime = System.currentTimeMillis();
    }

    public void run() {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            // Обробка повідомлень від клієнта
            while (true) {
                String message = in.readLine();
                if (message == null) break;
                handleClientMessage(message);
            }
        } catch (IOException e) {
            System.err.println("Помилка з'єднання: " + e.getMessage());
        } finally {
            try {
                socket.close();
                mazeServer.getClients().remove(this);
            } catch (IOException e) {
                System.err.println("Помилка при закритті сокету: " + e.getMessage());
            }
        }
    }

    private void handleClientMessage(String message) {
        if (message.startsWith("NAME ")) {
            // Обробка команди NAME
            String[] parts = message.split(" ");
            if (parts.length == 3) {
                playerName = parts[1];
                playerSymbol = parts[2].charAt(0);
                if (playerSymbol == MazeServer.WALL || playerSymbol == MazeServer.DIAMOND || mazeServer.getPlayers().containsKey(playerSymbol)) {
                    playerSymbol = getNextAvailableSymbol();
                    playerName = "Гравець " + playerSymbol;
                }
                var player = new Player(playerName, playerSymbol);
                mazeServer.getPlayers().put(playerSymbol, player);
                mazeServer.getMaze()[player.getPosY()][player.getPosX()] = playerSymbol;
                out.println("Привіт, " + playerName + "! Вас позначено символом " + playerSymbol);
            }
        } else if (message.equals("STAT")) {
            // Обробка команди STAT
            sendGameState();
        } else if (message.startsWith("MESG ")) {
            // Обробка команди MESG
            String chatMessage = message.substring(5);
            broadcastMessage(chatMessage);
        } else if (message.equals("RIGHT") || message.equals("DOWN") || message.equals("LEFT") || message.equals("UP")) {
            // Обробка команд руху
            movePlayer(message);
        } else if (message.equals("QUIT")) {
            // Обробка команди QUIT
            mazeServer.getPlayers().remove(playerSymbol);
            out.println("Ви покинули гру. До зустрічі!");
            try {
                socket.close();
            } catch (IOException e) {
                System.err.println("Помилка при закритті сокету: " + e.getMessage());
            }
        } else if (message.equals("CLOS")) {
            // Обробка команди CLOS
            List<ClientHandler> clients = mazeServer.getClients();
            clients.forEach(client -> {
                mazeServer.getPlayers().remove(client.getPlayerSymbol());
                try {
                    out.println("CLOS: Сервер завершує свою роботу!");
                    client.socket.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            mazeServer.getClients().removeAll(clients);
            mazeServer.running = false;
            stop();
        } else if (message.startsWith("KILL ")) {
            // Обробка команди KILL
            String[] parts = message.split(" ");
            if (parts.length == 2) {
                char targetName = parts[1].charAt(0);
                killPlayer(targetName);
            }
        } else if (message.startsWith("BOMB ")) {
            // Обробка команди BOMB
            int radius = Integer.parseInt(message.substring(5));
            bombArea(radius);
        } else if (message.startsWith("MAGIC")) {
            // Обробка команди BOMB
            isMagicMode = !isMagicMode;
            out.println("Magic mode is: " + (isMagicMode ? "ON" : "OFF"));
        } else if (message.startsWith("TIME ")) {
            // Обробка команди TIME
            var playerName = message.substring(5);
            mazeServer.getClients().stream()
                    .filter(cl -> cl.getPlayerName().equals(playerName))
                    .forEach(cl -> {
                        out.println((System.currentTimeMillis() - cl.getStartTime()) / 1000);
                    });
        }

    }

    private void sendGameState() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < MazeServer.HEIGHT; i++) {
            for (int j = 0; j < MazeServer.WIDTH; j++) {
                sb.append(mazeServer.getMaze()[i][j]);
            }
            sb.append('\n');
        }
        for (Player player : mazeServer.getPlayers().values()) {
            sb.append(player.getSymbol()).append(" ").append(player.getName()).append(" ").append(player.getScore()).append('\n');
        }
        out.println(sb);
    }

    private void broadcastMessage(String message) {
        for (ClientHandler client : mazeServer.getClients()) {
            client.out.println("Повідомлення: " + message);
        }
    }

    private void movePlayer(String direction) {
        var player = mazeServer.getPlayers().get(playerSymbol);
        var maze = mazeServer.getMaze();
        if (player != null) {
            int x = player.getPosX();
            int y = player.getPosY();
            switch (direction) {
                case "RIGHT" -> {
                    if (x + 1 < MazeServer.WIDTH && (maze[y][x + 1] == MazeServer.EMPTY || maze[y][x + 1] == MazeServer.DIAMOND)) {
                        x++;
                    }
                }
                case "DOWN" -> {
                    if (y + 1 < MazeServer.HEIGHT && (maze[y + 1][x] == MazeServer.EMPTY || maze[y + 1][x] == MazeServer.DIAMOND)) {
                        y++;
                    }
                }
                case "LEFT" -> {
                    if (x - 1 >= 0 && (maze[y][x - 1] == MazeServer.EMPTY || maze[x - 1][x] == MazeServer.DIAMOND)) {
                        x--;
                    }
                }
                case "UP" -> {
                    if (y - 1 >= 0 && (maze[y - 1][x] == MazeServer.EMPTY || maze[y - 1][x] == MazeServer.DIAMOND)) {
                        y--;
                    }
                }
            }
            checkCell(player, maze, x, y);
        }
    }

    private void checkCell(Player player, char[][] maze, int x, int y) {
        if (x != player.getPosX() || y != player.getPosY()) {
            maze[player.getPosY()][player.getPosX()] = MazeServer.EMPTY;
            if (isMagicMode) {
                mazeServer.getMaze()[player.getPosY()][player.getPosX()] = MazeServer.DIAMOND;
            }
            player.setPosX(x);
            player.setPosY(y);
            if (maze[y][x] == MazeServer.DIAMOND) {
                player.incrementScore();
                mazeServer.getMaze()[y][x] = MazeServer.EMPTY;
            }

            maze[y][x] = player.getSymbol();
        }
    }

    private void killPlayer(char symbol) {
        Player player = mazeServer.getPlayers().remove(symbol);
        if (player != null) {
            out.println("Гравця " + symbol + " знищено.");
        } else {
            out.println("Гравця з ім'ям " + symbol + " не знайдено.");
        }
    }

    private void bombArea(int radius) {
        var player = mazeServer.getPlayers().get(playerSymbol);
        if (player != null) {
            int centerX = player.getPosX();
            int centerY = player.getPosY();

            IntStream.range(Math.max(0, centerY - radius), Math.min(MazeServer.HEIGHT - 1, centerY + radius) + 1).forEach(y ->
                    IntStream.range(Math.max(0, centerX - radius), Math.min(MazeServer.WIDTH - 1, centerX + radius) + 1).forEach(x -> {
                        if (mazeServer.getMaze()[y][x] != MazeServer.WALL) {
                            if (mazeServer.getMaze()[y][x] != MazeServer.EMPTY && mazeServer.getMaze()[y][x] != MazeServer.DIAMOND) {
                                // Якщо на цій комірці є гравець, знищуємо його
                                var victim = mazeServer.getClients().stream().filter(cl -> cl.playerSymbol == mazeServer.getMaze()[y][x]).findFirst();
                                victim.ifPresent(client -> {
                                    mazeServer.getPlayers().remove(client.getPlayerSymbol());
                                    client.out.println("CLOS: Вас вбило вибухом!");
                                    client.stop();
                                });
                            }
                            mazeServer.getMaze()[y][x] = MazeServer.EMPTY;
                        }
                    }));
            broadcastMessage("Гравець " + player.getName() + " використав BOMB з радіусом " + radius);
        }
    }


    private char getNextAvailableSymbol() {
        for (char c = '1'; c <= '9'; c++) {
            if (!mazeServer.getPlayers().containsKey(c))
                return c;
        }
        return '0';
    }

    public String getPlayerName() {
        return playerName;
    }

    public long getStartTime() {
        return startTime;
    }

    public char getPlayerSymbol() {
        return playerSymbol;
    }
}
