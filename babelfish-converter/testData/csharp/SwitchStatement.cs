using System;

namespace test
{
    public class SwitchStatement
    {
        public static void Main(string[] args)
        {
            switch (args.Length)
            {
                case 0:
                    Console.WriteLine("No arguments found");
                    break;
                case 1:
                    Console.WriteLine(args[0]);
                    break;
                default:
                    Console.WriteLine("Unexpected arguments");
                    break;
            }
        }
    }
}
