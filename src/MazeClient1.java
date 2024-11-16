import java.io.IOException;

public class MazeClient1 extends Client {

    public static void main(String[] args) {
        new MazeClient1().start();
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
