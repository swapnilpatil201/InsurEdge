package pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

public class AuthorizePolicyPage {

    // 1) Constructor
    public WebDriver driver;

    public AuthorizePolicyPage(WebDriver driver) {
        this.driver = driver;
    }

    // 2) Locators (public, simple, and stable)
    // Matches: <select id="ContentPlaceHolder_Admin_ddlMainCategory" ...>
    public By dropdownMainCategory = By.id("ContentPlaceHolder_Admin_ddlMainCategory");

    // Matches: <input id="ContentPlaceHolder_Admin_btnSearch" ...>
    public By searchButton = By.id("ContentPlaceHolder_Admin_btnSearch");

    // Additional controls you added (kept public)
    public By ddlSubCategory = By.id("ContentPlaceHolder_Admin_ddlSubCategory");
    public By ddlStatus      = By.id("ContentPlaceHolder_Admin_ddlStatus");
    public By btnReset       = By.id("ContentPlaceHolder_Admin_btnReset");
    
    
    
    public By gridRows       = By.xpath("//table[@id='ContentPlaceHolder_Admin_gvPolicies']/tbody/tr");
    public By pagerContainer = By.xpath("//tr[contains(@class,'pagination-container')]");

    // 3) Actions (public)

    /** Select Main Category by visible text. */
    public void selectMainCategory(String visibleText) {
        WebElement mainCategory = driver.findElement(dropdownMainCategory);
        Select select = new Select(mainCategory);
        select.selectByVisibleText(visibleText);
    }

    /** Select the default Main Category option (index 0). Use when you want to clear selection. */
    public void selectDefaultMainCategory() {
        new Select(driver.findElement(dropdownMainCategory)).selectByIndex(0);
    }

    /** Click Search button. */
    public void clicksearchButton() {
        driver.findElement(searchButton).click();
    }

    /** Select Sub Category by visible text. */
    public void selectSubCategory(String visibleText) {
        new Select(driver.findElement(ddlSubCategory)).selectByVisibleText(visibleText);
    }

    /** Select Status by visible text. */
    public void selectStatus(String visibleText) {
        new Select(driver.findElement(ddlStatus)).selectByVisibleText(visibleText);
    }

    /** Click Reset button. */
    public void clickResetButton() {
        driver.findElement(btnReset).click();
    }

    // 4) Optional getters (handy for assertions in tests)

    /** Returns currently selected Main Category text. */
    public String getSelectedMainCategory() {
        return new Select(driver.findElement(dropdownMainCategory))
                .getFirstSelectedOption().getText().trim();
    }

    /** Returns currently selected Sub Category text. */
    public String getSelectedSubCategory() {
        return new Select(driver.findElement(ddlSubCategory))
                .getFirstSelectedOption().getText().trim();
    }

    /** Returns currently selected Status text. */
    public String getSelectedStatus() {
        return new Select(driver.findElement(ddlStatus))
                .getFirstSelectedOption().getText().trim();
    }
    
    
   

    /** Returns true if the given page number is the current page (non-clickable <span>). */
    public boolean isCurrentPage(int pageNo) {
        String spanXpath = "//tr[contains(@class,'pagination-container')]//span[normalize-space()='" + pageNo + "']";
        return !driver.findElements(By.xpath(spanXpath)).isEmpty();
    }

    /** Returns true if the given page number is a clickable link (i.e., not the current page). */
    public boolean hasPageLink(int pageNo) {
        String linkXpath = "//tr[contains(@class,'pagination-container')]//a[normalize-space()='" + pageNo + "']";
        return !driver.findElements(By.xpath(linkXpath)).isEmpty();
    }

    /** Click a page number if a link is available and wait for grid to reload. */
    public boolean goToPageIfExists(int pageNo) {
        String linkXpath = "//tr[contains(@class,'pagination-container')]//a[contains(@href,\"Page$" + pageNo + "\")]";
        java.util.List<org.openqa.selenium.WebElement> links = driver.findElements(By.xpath(linkXpath));
        if (links.isEmpty()) return false;
        links.get(0).click();
        // tiny, safe wait for grid presence
        new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofSeconds(10))
                .until(org.openqa.selenium.support.ui.ExpectedConditions.presenceOfElementLocated(gridRows));
        new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofSeconds(10))
                .until(org.openqa.selenium.support.ui.ExpectedConditions.presenceOfElementLocated(pagerContainer));
        return true;
    }
}