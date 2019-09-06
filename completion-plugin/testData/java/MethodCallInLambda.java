public class MethodCallInLambda {
    public static void main(String[] args) {
        Runnable r = () -> test("Hello from lambda");
        r.run();
    }


    private void test(Object a) {
    }
}
