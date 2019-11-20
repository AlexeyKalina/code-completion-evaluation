package hello;

import package1.CallingMethodsInSameClass;
import package2.ReadAndPrintScores;

class Main {
    public static void main(String[] args) {
        Greeting greeting = new Greeting();
        greeting.sayHello("world");

        CallingMethodsInSameClass.printTwo();
        ReadAndPrintScores.readAndPrint();
    }
}