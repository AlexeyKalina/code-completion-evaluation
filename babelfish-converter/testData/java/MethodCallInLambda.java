public class MethodCallInLambda {
    public static void main(String[] args) {
        Runnable r = () -> System.out.println("Hello from lambda");
        r.run();
    }
}
