package base;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.testng.annotations.*;
import pages.LoginPage;

import java.time.Duration;

public class BaseTest {

    public WebDriver driver;

    // Credentials & URL
    public String baseUrl  = "https://qeaskillhub.cognizant.com/LoginPage?logout=true";
    public String username = "admin_user";
    public String password = "testadmin";

    @BeforeClass(alwaysRun = true)
    public void setUpClass() {

        driver = new ChromeDriver();
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
        driver.manage().window().maximize();

        // Open login page ONCE per class
        driver.get(baseUrl);

        // Login ONCE per class
        LoginPage lp = new LoginPage(driver);
        lp.setUserName(username);
        lp.setPassword(password);
        lp.clickLoginButton();
    }

    @AfterClass(alwaysRun = true)
    public void tearDownClass() {
        if (driver != null) {
            driver.quit();
        }
    }
}
