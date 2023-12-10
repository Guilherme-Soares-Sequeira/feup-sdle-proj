package org.C2;


import org.C2.crdts.ORMap;

public class Main {
    public static void main(String[] args) {
        ORMap ormap1 = new ORMap("a");
        ORMap ormap2 = new ORMap("b");


        ormap1.put("banana", -1);

        ormap1.value("banana").inc(1);

        ormap1.value("banana").dec(3);

        ormap1.value("banana").inc(1);


        ormap2.join(ormap1);
        ormap2.value("banana").dec(1);
        ormap2.value("banana").inc(2);

    }
}
