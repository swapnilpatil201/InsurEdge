package tests;
import pages.*;

import java.time.Duration;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class LoginTest  extends BaseTest{
	
	
	WebDriver driver;
	
	
	
	
	@Test
	void testLogin() {
		LoginPage lp= new LoginPage(driver);
		//lp.setUserName("admin_user");
	//	lp.setPassword("testadmin");
		//lp.clickLoginButton();
		
		
	Assert.assertEquals(driver.getTitle(), "InsurEdge - Admin Dashboard");
		
	}
	
	
	
	
	
	
	


}
