public class InvocationOnAnonymous {
    public static void main(String[] args) {
        new Producer() {
            @Override
            public NestedClass produce() {
                return new NestedClass();
            }
        }.produce();
    }

    static class NestedClass {
        void method() {
        }
    }

    interface Producer {
        NestedClass produce();
    }
}
