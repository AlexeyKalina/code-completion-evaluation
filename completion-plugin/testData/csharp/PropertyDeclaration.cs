public class PropertyDeclaration
{
    public int Property
    {
        get { return DateTimeOffset.Now.ToUnixTimeMilliseconds(); }
    }

    public static void Main(string[] args)
    {
    }
}
