package tests;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.testng.annotations.*;
import pages.LoginPage;
import java.time.Duration;

public abstract class BaseTest {
    protected WebDriver driver;

    @BeforeClass(alwaysRun = true)
    public void baseSetUp() {
        // 1) Driver setup
        driver = new ChromeDriver();
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
        driver.manage().window().maximize();

        // 2) Auto-login (applies to ALL tests that extend BaseTest)
        driver.get("https://qeaskillhub.cognizant.com/LoginPage?logout=true");
        LoginPage lp = new LoginPage(driver);
        lp.setUserName("admin_user");
        lp.setPassword("testadmin");
        lp.clickLoginButton();
    }

    @AfterClass(alwaysRun = true)
    public void baseTearDown() {
        if (driver != null) driver.quit();
    }
}