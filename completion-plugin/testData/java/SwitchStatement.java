public class SwitchStatement {
    public static void main(String[] args) {
        switch (args.length) {
            case 0:
                test("No arguments found");
                break;
            case 1:
                test(args[0]);
            default:
                test("Unexpected arguments");
        }
    }


    private void test(Object a) {
    }
}
