public class TernaryOperator
{
    public static void Main(string[] args)
    {
        object value = DateTimeOffset.Now.ToUnixTimeMilliseconds() % 2 == 1 ? five() : 10;
    }
    private static int five()
    {
        return 5;
    }
}
