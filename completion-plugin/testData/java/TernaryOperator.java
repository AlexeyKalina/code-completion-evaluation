public class TernaryOperator {
    public static void main(String[] args) {
        Object value = System.currentTimeMillis() % 2 == 1 ? five() : 10;
    }
    private static void five() {
        return 5;
    }
}
