public class LambdaInvocationInPlace {
    public static void main(String[] args) {
        ((Runnable) () -> {
            test(11);
        }).run();
    }


    private void test(Object a) {
    }
}