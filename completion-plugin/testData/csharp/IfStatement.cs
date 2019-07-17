public class IfStatement
{
    public static void Main(string[] args)
    {
        if (DateTimeOffset.Now.ToUnixTimeMilliseconds() % 2 == 0)
        {
            System.Console.WriteLine("true");
        }
        else
        {
            System.Console.WriteLine("false");
        }
    }
}
