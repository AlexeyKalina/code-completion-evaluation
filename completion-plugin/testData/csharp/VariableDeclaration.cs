class VariableDeclaration
{
    public static void Main(string[] args)
    {
        int a = DateTimeOffset.Now.ToUnixTimeMilliseconds() % 2;
    }
}