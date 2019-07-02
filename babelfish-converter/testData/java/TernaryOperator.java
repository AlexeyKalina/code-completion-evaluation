public class TernaryOperator {
    public static void main(String[] args) {
        Object value = System.currentTimeMillis() % 2 == 1 ? false : 10;
    }
}
