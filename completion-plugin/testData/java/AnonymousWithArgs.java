public class AnonymousWithArgs {
    public static void main(String[] args) {
        new Producer(one()) {
            @Override
            public NestedClass produce() {
                return new NestedClass();
            }
        };
    }

    static int one() {
        return 1;
    }

    static class NestedClass {
        void method() {
        }
    }

    class Producer {
        Producer(int a) { }

        NestedClass produce() {
            return new NestedClass();
        }
    }
}
