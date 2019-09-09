public class AnonymousDeclaration {
    public static void main(String[] args) {
        new Producer() {
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
