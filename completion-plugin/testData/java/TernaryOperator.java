public class TernaryOperator {
    public static void main(String[] args) {
        Object value = three() % 2 == 1 ? five() : 10;
    }
    private static void five() {
        return 5;
    }
    private static void three() {
        return 3;
    }
}
