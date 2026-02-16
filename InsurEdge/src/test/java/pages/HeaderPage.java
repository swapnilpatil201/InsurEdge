package pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class HeaderPage {
	
	WebDriver driver;
	
	public HeaderPage(WebDriver driver) {
		this.driver=driver;
	}
	
	
	
	
	
	By sidebarToggleButton = By.cssSelector(".toggle-sidebar-btn");
	
	
	
	
	
	 public void clickSideBarButton() {
         driver.findElement(sidebarToggleButton).click();
	 }

}
