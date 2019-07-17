public class SwitchStatement {
    public static void main(String[] args) {
        switch (args.length) {
            case 0:
                System.out.println("No arguments found");
                break;
            case 1:
                System.out.println(args[0]);
            default:
                System.out.println("Unexpected arguments");
        }
    }
}
