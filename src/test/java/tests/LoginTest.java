package tests;

import base.BaseTest;
import org.testng.Assert;
import org.testng.annotations.Test;

public class LoginTest extends BaseTest {

    @Test
    void testLoginTitle() {
        // Already logged in through BaseTest
        Assert.assertEquals(
                driver.getTitle(),
                "InsurEdge - Admin Dashboard");
    }
}