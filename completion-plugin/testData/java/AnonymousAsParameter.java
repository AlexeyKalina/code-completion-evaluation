public class AnonymousAsParameter {
    public static void main(String[] args) {
        invoke(new Runnable() {
            @java.lang.Override
            public void run() {
            }
        });
    }

    private static void invoke(Runnable runnable) {
        runnable.run();
    }

    interface Runnable {
        void run();
    }
}
