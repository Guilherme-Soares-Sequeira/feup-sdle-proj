package org.C2.cloud;

import org.C2.utils.ServerInfo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ServerInfoTest {

    @Test
    public void fromAndToString() {
        String url1 = "localhost:5554";
        ServerInfo si1 = new ServerInfo("localhost", 5554);

        Assertions.assertEquals(ServerInfo.fromString(url1), si1);
        Assertions.assertEquals(url1, si1.toString());

        String url2 = "local:host:5557";
        ServerInfo si2 = new ServerInfo("local:host", 5557);

        Assertions.assertEquals(ServerInfo.fromString(url2), si2);
        Assertions.assertEquals(url2, si2.toString());
    }
}
