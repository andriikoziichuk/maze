import java.io.IOException;

public class MazeClient extends Client{

    public static void main(String[] args) {
        new MazeClient().start();
    }

    public void start() {
        try {
            connectToServer();
            handleUserInput();
        } catch (IOException e) {
            System.err.println("Помилка з'єднання: " + e.getMessage());
        }
    }
}
