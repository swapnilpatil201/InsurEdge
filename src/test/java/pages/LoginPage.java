package pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class LoginPage {

    // 👇 1) Constructor first
    public WebDriver driver;

    public LoginPage(WebDriver driver) {
        this.driver = driver;
    }

    // 👇 2) Locators (all public, simple)
    public By usernameInput = By.xpath("//input[@id='txtUsername']");
    public By passwordInput = By.xpath("//input[@id='txtPassword']");
    public By loginButton   = By.xpath("//input[@id='BtnLogin']");

    // 👇 3) Action methods (simple & public)
    public void setUserName(String userName) {
        driver.findElement(usernameInput).clear();
        driver.findElement(usernameInput).sendKeys(userName);
    }

    public void setPassword(String password) {
        driver.findElement(passwordInput).clear();
        driver.findElement(passwordInput).sendKeys(password);
    }

    public void clickLoginButton() {
        driver.findElement(loginButton).click();
    }

   
    }
