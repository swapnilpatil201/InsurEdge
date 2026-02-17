package pages;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.*;

import java.time.Duration;
import java.util.List;

/**
 * POM for Authorized Policies page (AdminAuthorizePolicy.aspx).
 */
public class AuthorizePolicyPage {

    public WebDriver driver;
    public WebDriverWait wait;

    public AuthorizePolicyPage(WebDriver driver) {
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

    // ===== Actions (filters) =====

    public void selectMainCategory(String visibleText) {
        new Select(driver.findElement(ddlMainCategory)).selectByVisibleText(visibleText);
    }

    public void selectSubCategory(String visibleText) {
        new Select(driver.findElement(ddlSubCategory)).selectByVisibleText(visibleText);
    }

    public void selectStatus(String visibleText) {
        new Select(driver.findElement(ddlStatus)).selectByVisibleText(visibleText);
    }

    // ===== Actions (buttons) =====

    /** Kept method name (lowercase 's') to match code that already calls 'clicksearchButton()' */
    public void clicksearchButton() {
        driver.findElement(btnSearch).click();
        wait.until(ExpectedConditions.presenceOfElementLocated(gridRows));
    }

    public void clickResetButton() {
        driver.findElement(btnReset).click();
        wait.until(ExpectedConditions.presenceOfElementLocated(gridRows));
    }

    // ===== Pagination helpers =====

    /** Navigate to page n if link exists; scroll + clickable + JS fallback to avoid click interception. */
    public boolean goToPageIfExists(int page) {
        String num = String.valueOf(page);
        By linkBy = By.xpath(
            "//tr[contains(@class,'pagination-container')]//a[normalize-space()='" + num + "']" +
            " | //ul[contains(@class,'pagination')]//a[normalize-space()='" + num + "']"
        );

        List<WebElement> links = driver.findElements(linkBy);
        if (links.isEmpty()) return false;

        WebElement target = links.get(0);

        // If your app uses a loader/overlay, adjust this selector accordingly:
        try {
            new WebDriverWait(driver, Duration.ofSeconds(5))
                .until(ExpectedConditions.invisibilityOfElementLocated(
                    By.cssSelector(".loading, .spinner, .modal-backdrop")));
        } catch (Exception ignored) {}

        try {
            ((JavascriptExecutor)driver).executeScript(
                "arguments[0].scrollIntoView({block:'center'});", target);
        } catch (Exception ignored) {}

        try {
            new WebDriverWait(driver, Duration.ofSeconds(5))
                .until(ExpectedConditions.elementToBeClickable(target))
                .click();
        } catch (Exception e) {
            try {
                ((JavascriptExecutor)driver).executeScript("arguments[0].click();", target);
            } catch (Exception ignored) { return false; }
        }

        new WebDriverWait(driver, Duration.ofSeconds(10))
            .until(ExpectedConditions.presenceOfElementLocated(gridRows));
        return true;
    }

    /** True if the current page number appears as a non-link span. */
    public boolean isCurrentPage(int page) {
        String num = String.valueOf(page);
        List<WebElement> spans = driver.findElements(By.xpath(
            "//tr[contains(@class,'pagination-container')]//span[normalize-space()='" + num + "']" +
            " | //ul[contains(@class,'pagination')]//span[normalize-space()='" + num + "']"
        ));
        if (!spans.isEmpty() && spans.get(0).isDisplayed()) return true;

        // Some templates: <li class='active'><span>num</span></li>
        List<WebElement> actives = driver.findElements(By.xpath(
            "//ul[contains(@class,'pagination')]//li[contains(@class,'active')]//span[normalize-space()='" + num + "']"
        ));
        return !actives.isEmpty() && actives.get(0).isDisplayed();
    }

    /** True if page number is a clickable link (i.e., NOT current page). */
    public boolean hasPageLink(int page) {
        String num = String.valueOf(page);
        List<WebElement> links = driver.findElements(By.xpath(
            "//tr[contains(@class,'pagination-container')]//a[normalize-space()='" + num + "']" +
            " | //ul[contains(@class,'pagination')]//a[normalize-space()='" + num + "']"
        ));
        return !links.isEmpty() && links.get(0).isDisplayed();
    }
}