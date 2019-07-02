public class FieldAssignment {
    private int field = 100;

    public static void main(String[] args) {
        FieldAssignment fieldAssignment = new FieldAssignment();
        fieldAssignment.field = 1000;
        System.out.println(fieldAssignment);
    }
}
