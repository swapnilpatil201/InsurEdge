package pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class SlideBarMenuPage {

    // 1) Constructor
    public WebDriver driver;

    public SlideBarMenuPage(WebDriver driver) {
        this.driver = driver;
    }

    // 2) Locators (public)
    public By policyMenu         = By.xpath("//*[@id=\"sidebar-nav\"]/li[4]/a");
    public By linkPolicyCreate   = By.xpath("//*[@id=\"tables-nav\"]/li[1]/a");
    public By linkPolicyAuthorize= By.xpath("//*[@id=\"tables-nav\"]/li[2]/a");


    // 3) Actions (public)

    /** Expands the Policy menu (does not click any child). */
    public void expandPolicyMenu() {
        driver.findElement(policyMenu).click();
    }

    /** Expands Policy and clicks the Create link. */
    public void clickPolicyCreate() {
       // expandPolicyMenu();
        driver.findElement(linkPolicyCreate).click();
    }

    /** Expands Policy and clicks the Authorize link. */
    public void clickPolicyAuthorize() {
        //expandPolicyMenu();
        driver.findElement(linkPolicyAuthorize).click();
    }

    

   

    
}