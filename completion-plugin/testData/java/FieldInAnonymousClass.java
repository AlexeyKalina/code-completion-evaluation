public class FieldInAnonymousClass {
    public static void main(String[] args) {
        new Producer() {
            public int a;

            @Override
            public NestedClass produce() {
                return new NestedClass();
            }
        };
    }

    static class NestedClass {
        void method() {
        }
    }

    interface Producer {
        NestedClass produce();
    }
}
