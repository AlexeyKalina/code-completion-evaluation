public class LambdaInvocationInPlace {
    public static void main(String[] args) {
        ((Runnable) () -> {
            System.out.println(11);
        }).run();
    }
}