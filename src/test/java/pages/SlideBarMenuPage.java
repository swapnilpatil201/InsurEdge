package pages;

import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.NoSuchSessionException;
import org.openqa.selenium.UnhandledAlertException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.UnreachableBrowserException;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

/**
 * Simple & alert-safe navigation:
 * - Dismisses alert if present (tolerant if browser/session is gone)
 * - Navigates via submenu link hrefs (works even if submenu is collapsed)
 */
public class SlideBarMenuPage {

    public WebDriver driver;
    public WebDriverWait wait;

    public SlideBarMenuPage(WebDriver driver) {
        this.driver = driver;
        this.wait   = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    // Toggle and submenu
    public By policyToggle   = By.cssSelector("a.nav-link[data-bs-target='#tables-nav']");
    public By policySubmenu  = By.id("tables-nav");

    // Submenu links
    public By linkCreate     = By.cssSelector("#tables-nav a[href*='AdminCreatePolicy']");
    public By linkAuthorize  = By.cssSelector("#tables-nav a[href*='AdminAuthorizePolicy']");

    /** Dismiss browser alert if present; tolerate unreachable/closed sessions. */
    public void dismissAlertIfPresent() {
        try {
            Alert a = driver.switchTo().alert();
            a.accept();
            try { Thread.sleep(120); } catch (InterruptedException ignored) {}
        } catch (NoAlertPresentException | UnreachableBrowserException | NoSuchSessionException | UnhandledAlertException ignored) {
        } catch (Exception ignored) {
            // final guard: ignore any other runtime error
        }
    }

    /** Try to navigate to a link via its absolute href; fallback to relative. */
    private boolean navigateViaHref(By by, String fallbackRelative) {
        try {
            List<WebElement> els = driver.findElements(by);
            if (!els.isEmpty()) {
                String href = els.get(0).getAttribute("href");
                if (href != null && !href.isEmpty()) {
                    driver.navigate().to(href);
                    return true;
                }
            }
            // Fallback: build relative url
            if (fallbackRelative != null && !fallbackRelative.isEmpty()) {
                String current = driver.getCurrentUrl();
                int lastSlash = current.lastIndexOf('/');
                if (lastSlash > 7) {
                    String base = current.substring(0, lastSlash + 1);
                    driver.navigate().to(base + fallbackRelative);
                    return true;
                }
            }
        } catch (UnreachableBrowserException | NoSuchSessionException ignored) {
        } catch (Exception ignored) {
        }
        return false;
    }

    /** Navigate → Policy → Create (alert-safe). */
    public void clickPolicyCreate() {
        dismissAlertIfPresent();
        if (navigateViaHref(linkCreate, "AdminCreatePolicy.aspx")) return;

        // Try toggling once, then retry
        try { driver.findElement(policyToggle).click(); } catch (Exception ignored) {}
        if (navigateViaHref(linkCreate, "AdminCreatePolicy.aspx")) return;

        try { Thread.sleep(200); } catch (InterruptedException ignored) {}
        navigateViaHref(linkCreate, "AdminCreatePolicy.aspx");
    }

    /** Navigate → Policy → Authorize (alert-safe). */
    public void clickPolicyAuthorize() {
        dismissAlertIfPresent();
        if (navigateViaHref(linkAuthorize, "AdminAuthorizePolicy.aspx")) return;

        try { driver.findElement(policyToggle).click(); } catch (Exception ignored) {}
        if (navigateViaHref(linkAuthorize, "AdminAuthorizePolicy.aspx")) return;

        try { Thread.sleep(200); } catch (InterruptedException ignored) {}
        navigateViaHref(linkAuthorize, "AdminAuthorizePolicy.aspx");
    }

    public void clickPolicyAuthorized() {
        clickPolicyAuthorize();
    }
}