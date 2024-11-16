import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public abstract class Client {
    private static final int PORT = 8001;
    private BufferedReader in;
    private PrintWriter out;

    protected void connectToServer() throws IOException {
        try (Socket socket = new Socket("localhost", PORT)) {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            System.out.println("Підключено до сервера");
        } catch (Exception e) {
            throw e;
        }
    }

    protected void handleUserInput() {
        try (BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in))) {
            System.out.print("Введіть ваше ім'я: ");
            String name = userInput.readLine();
            System.out.print("Введіть символ, яким вас позначатимуть: ");
            char symbol = userInput.readLine().charAt(0);
            out.println("NAME " + name + " " + symbol);

            new Thread(new Client.ServerResponseHandler()).start();

            while (true) {
                String input = userInput.readLine();
                out.println(input);
                if (input.equals("QUIT") || input.equals("CLOS")) break;
            }
        } catch (IOException e) {
            System.err.println("Помилка вводу: " + e.getMessage());
        }
    }

    private class ServerResponseHandler implements Runnable {
        public void run() {
            try {
                while (true) {
                    String response = in.readLine();
                    System.out.println(response);
                    if (response == null || response.startsWith("CLOS")) {
                        break;
                    }
                }
            } catch (IOException e) {
                System.err.println("Помилка з'єднання: " + e.getMessage());
            }
        }
    }
}
