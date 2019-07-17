public class CodeBlockInLambda {
    public static void main(String[] args) {
        Runnable r = () -> {
            System.out.println("Hello from lambda 1");
            System.out.println("Hello from lambda 2");
        };
        r.run();
    }
}
