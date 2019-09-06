public class IfStatement {
    public static void main(String[] args) {
        if (random() % 2 == 0) {
            test("true");
        } else {
            test("false");
        }
    }

    private void test(Object a) {
    }

    private int random() {
        return 3;
    }
}
