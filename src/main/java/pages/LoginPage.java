package pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class LoginPage {
	
	WebDriver driver;
	
	
	//constructor
 public LoginPage(WebDriver driver){
		this.driver=driver;
		
	}
	
	
	//locator
	By usernameInput=By.xpath("//input[@id='txtUsername']");
	By passwordInput=By.xpath("//input[@id='txtPassword']");
	By loginButton=By.xpath("//input[@id='BtnLogin']");
	
	//action method
	
	public void setUserName(String userName) {
		driver.findElement(usernameInput).sendKeys(userName);
	}
	
	public void setPassword (String password) {
		driver.findElement(passwordInput).sendKeys(password);
	}
	
	public void clickLoginButton() {
		driver.findElement(loginButton).click();
	}
	
	

}
