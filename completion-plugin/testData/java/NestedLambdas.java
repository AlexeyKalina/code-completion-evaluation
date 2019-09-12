public class NestedLambdas {
    public static void main(String[] args) {
        method(x -> y -> x.toString());
    }

    private static void method(Consumer consumer) {
        consumer.consume(1);
    }

    interface Consumer {
        void consume(Object x);
    }
}
