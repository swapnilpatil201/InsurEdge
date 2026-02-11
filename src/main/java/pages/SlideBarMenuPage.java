package pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class SlideBarMenuPage {
	WebDriver driver;
	
	public  SlideBarMenuPage(WebDriver driver) {
		this.driver=driver;
		
	}
	
	
	
  By policyMenu= By.xpath("//a[@data-bs-target='#tables-nav']");
  
  By OptionAuthorize = By.xpath("//a[@href='AdminAuthorizePolicy.aspx']");
  
  
  
  
  public void clickPolicyMenu() {
	  driver.findElement(policyMenu).click();
	  driver.findElement(OptionAuthorize).click();
  }
  

}
