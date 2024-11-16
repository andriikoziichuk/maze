import java.util.Random;

class Player {
    private final String name;
    private final char symbol;
    private int score;
    private int posX;
    private int posY;


    Player(String name, char symbol) {
        this.name = name;
        this.symbol = symbol;
        this.score = 0;
        this.posX = new Random().nextInt(MazeServer.WIDTH);
        this.posY = new Random().nextInt(MazeServer.HEIGHT);
    }

    public String getName() {
        return name;
    }

    public char getSymbol() {
        return symbol;
    }

    public int getScore() {
        return score;
    }

    public int getPosX() {
        return posX;
    }

    public int getPosY() {
        return posY;
    }

    public void setPosX(int posX) {
        this.posX = posX;
    }

    public void setPosY(int posY) {
        this.posY = posY;
    }

    public void incrementScore() {
        score++;
    }
}
