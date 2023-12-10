package org.C2;


import org.C2.crdts.ORMap;

public class Main {
    public static void main(String[] args) {
        ORMap ormap = new ORMap("a");
        ormap.put("banana", 2);
        System.out.println(ormap.toJson());
    }
}
