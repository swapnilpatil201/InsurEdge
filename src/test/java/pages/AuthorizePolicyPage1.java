package pages;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.*;

import java.time.Duration;
import java.util.List;

/**
 * POM for Authorized Policies page (AdminAuthorizePolicy.aspx).
 * Minimal & simple: selectors by id/xpath; waits via WebDriverWait; no JS executor.
 */
public class AuthorizePolicyPage1 {

    public WebDriver driver;
    public WebDriverWait wait;

    public AuthorizePolicyPage1(WebDriver driver) {
        this.driver  = driver;
        this.wait    = new WebDriverWait(driver, Duration.ofSeconds(12));
    }

    // Filters
    public By ddlMainCategory   = By.id("ContentPlaceHolder_Admin_ddlMainCategory");
    public By ddlSubCategory    = By.id("ContentPlaceHolder_Admin_ddlSubCategory");
    public By ddlStatus         = By.id("ContentPlaceHolder_Admin_ddlStatus");

    // Search/Reset
    public By btnSearch         = By.id("ContentPlaceHolder_Admin_btnSearch");
    public By btnReset          = By.id("ContentPlaceHolder_Admin_btnReset");

    // Grid + pagination
    public By gridRows          = By.xpath("//table[@id='ContentPlaceHolder_Admin_gvPolicies']/tbody/tr");
    public By pagerRow          = By.xpath("//tr[contains(@class,'pagination-container')]");
    public By pagerUl           = By.cssSelector("ul.pagination");

    // ===== Actions =====

    public void selectMainCategory(String visibleText) {
        new Select(driver.findElement(ddlMainCategory)).selectByVisibleText(visibleText);
    }

    public void selectSubCategory(String visibleText) {
        new Select(driver.findElement(ddlSubCategory)).selectByVisibleText(visibleText);
    }

    public void selectStatus(String visibleText) {
        new Select(driver.findElement(ddlStatus)).selectByVisibleText(visibleText);
    }

    public void clicksearchButton() { // keep legacy name to match tests
        driver.findElement(btnSearch).click();
        wait.until(ExpectedConditions.presenceOfElementLocated(gridRows));
    }

    public void clickSearchButton() {
        clicksearchButton();
    }

    public void clickResetButton() {
        driver.findElement(btnReset).click();
        wait.until(ExpectedConditions.presenceOfElementLocated(gridRows));
    }

    /** Attempts to go to the given page via pager link; returns true if clicked. */
    public boolean goToPageIfExists(int page) {
        String num = String.valueOf(page);

        // Try TR-based pager
        List<WebElement> trs = driver.findElements(pagerRow);
        if (!trs.isEmpty()) {
            List<WebElement> links = trs.get(0).findElements(By.xpath(".//a[normalize-space()='" + num + "']"));
            if (!links.isEmpty()) {
                links.get(0).click();
                wait.until(ExpectedConditions.presenceOfElementLocated(gridRows));
                return true;
            }
        }

        // Try UL-based pager
        List<WebElement> uls = driver.findElements(pagerUl);
        if (!uls.isEmpty()) {
            List<WebElement> links = uls.get(0).findElements(By.xpath(".//a[normalize-space()='" + num + "']"));
            if (!links.isEmpty()) {
                links.get(0).click();
                wait.until(ExpectedConditions.presenceOfElementLocated(gridRows));
                return true;
            }
        }

        // Generic fallback across any pager container
        List<WebElement> any = driver.findElements(By.xpath("//tr[contains(@class,'pagination-container')] | //ul[contains(@class,'pagination')]"));
        if (!any.isEmpty()) {
            List<WebElement> links = any.get(0).findElements(By.xpath(".//a[normalize-space()='" + num + "']"));
            if (!links.isEmpty()) {
                links.get(0).click();
                wait.until(ExpectedConditions.presenceOfElementLocated(gridRows));
                return true;
            }
        }

        return false;
    }

    /** True if the current page number is shown as a non-link span. */
    public boolean isCurrentPage(int page) {
        String num = String.valueOf(page);
        List<WebElement> spans = driver.findElements(By.xpath(
                "//tr[contains(@class,'pagination-container')]//span[normalize-space()='" + num + "']" +
                        " | //ul[contains(@class,'pagination')]//span[normalize-space()='" + num + "']"
        ));
        if (!spans.isEmpty() && spans.get(0).isDisplayed()) return true;

        // Some templates render current page as <li class='active'><span>num</span></li>
        List<WebElement> actives = driver.findElements(By.xpath(
                "//ul[contains(@class,'pagination')]//li[contains(@class,'active')]//span[normalize-space()='" + num + "']"
        ));
        return !actives.isEmpty() && actives.get(0).isDisplayed();
    }

    /** True if there's a clickable anchor for the given page (i.e., not the current page). */
    public boolean hasPageLink(int page) {
        String num = String.valueOf(page);
        List<WebElement> links = driver.findElements(By.xpath(
                "//tr[contains(@class,'pagination-container')]//a[normalize-space()='" + num + "']" +
                        " | //ul[contains(@class,'pagination')]//a[normalize-space()='" + num + "']"
        ));
        return !links.isEmpty() && links.get(0).isDisplayed();
    }
}