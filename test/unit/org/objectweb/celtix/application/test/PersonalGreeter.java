package org.objectweb.celtix.application.test;

public class PersonalGreeter {
    private String name;

    public PersonalGreeter(String n) {
        name = n;
    }

    public String sayHello() {
        return "Hello " + name + "!";
    }
}
