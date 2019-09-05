public class CodeBlockInLambda {
    public static void main(String[] args) {
        Runnable r = () -> {
            test("Hello from lambda 1");
            test("Hello from lambda 2");
        };
        r.run();
    }

    private void test(String a) {
    }
}
