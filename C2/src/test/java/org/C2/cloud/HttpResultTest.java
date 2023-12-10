package org.C2.cloud;

import org.C2.utils.HttpResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class HttpResultTest {

    @Test
    public void stringOkVariant() {
        HttpResult<String> shouldBeOk = HttpResult.ok(200, "something");
        Assertions.assertTrue(shouldBeOk.isOk());

        HttpResult<String> shouldBeErr = HttpResult.err(500, "an error message");
        Assertions.assertFalse(shouldBeErr.isOk());
    }
}
