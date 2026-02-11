package pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

public class AuthorizePolicyPage {
	
	WebDriver driver;
	
	public AuthorizePolicyPage(WebDriver driver) {
		this.driver=driver;
	}
	
	By DropdownMainCategory= By.xpath("//select[contains ( @id,'Admin_ddlMainCategory')]");
	By searchButton = By.xpath("//input[@id='ContentPlaceHolder_Admin_btnSearch']");
	
	
	public void selectMainCategory(String values) {
		
		WebElement mainCategory= driver.findElement(DropdownMainCategory);
		Select select = new Select(mainCategory);
		select.selectByVisibleText(values);
	
		
		}
	
	public void clicksearchButton() {
		driver.findElement(searchButton).click();
	}
	
	
	
	

}
