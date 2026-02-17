package tests;

import base.BaseTest;

import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;

import org.openqa.selenium.support.ui.*;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import pages.AuthorizePolicyPage;
import pages.SlideBarMenuPage;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * PolicyAuthorize - Main Category Filter validations (reduced to 3 tests).
 * Extends BaseTest for driver & login reuse.
 */
public class MainCategoryFilterValidationTest extends BaseTest {

    // ====== Authorize page locators ======
    private static final By DDL_MAIN_CATEGORY  = By.id("ContentPlaceHolder_Admin_ddlMainCategory");
    private static final By DDL_SUB_CATEGORY   = By.id("ContentPlaceHolder_Admin_ddlSubCategory");
    private static final By DDL_STATUS         = By.id("ContentPlaceHolder_Admin_ddlStatus");
    private static final By GRID_ROWS          = By.xpath("//table[@id='ContentPlaceHolder_Admin_gvPolicies']/tbody/tr");
    private static final By GRID_MAINCAT_CELLS = By.xpath("//table[@id='ContentPlaceHolder_Admin_gvPolicies']/tbody/tr/td[2]");
    private static final By PAGER_CONTAINER    = By.xpath("//tr[contains(@class,'pagination-container')] | //ul[contains(@class,'pagination')]");

    private WebDriverWait uiWait() {
        return new WebDriverWait(driver, Duration.ofSeconds(12));
    }

    // ====== Light helpers (no heavy waits to avoid setup timeouts) ======

    private void acceptAlertIfAppears(int seconds) {
        try {
            Alert a = new WebDriverWait(driver, Duration.ofSeconds(seconds))
                    .until(ExpectedConditions.alertIsPresent());
            a.accept();
            Thread.sleep(120);
        } catch (Exception ignored) {}
    }

    private void goToAuthorize() {
        SlideBarMenuPage menu = new SlideBarMenuPage(driver);
        menu.clickPolicyAuthorize();
        uiWait().until(ExpectedConditions.presenceOfElementLocated(DDL_MAIN_CATEGORY));
        uiWait().until(ExpectedConditions.presenceOfElementLocated(GRID_ROWS));
    }

    /** Fast, forgiving reset: click Reset; accept alert; set each select to index 0 if possible. */
    private void fastReset() {
        AuthorizePolicyPage ap = new AuthorizePolicyPage(driver);

        try { driver.findElement(ap.btnReset).click(); } catch (Exception ignored) {}
        acceptAlertIfAppears(2);
        try { Thread.sleep(200); } catch (InterruptedException ignored) {}

        // Try force defaults; ignore if elements are still rebinding
        try { new Select(driver.findElement(DDL_MAIN_CATEGORY)).selectByIndex(0); } catch (Exception ignored) {}
        try { new Select(driver.findElement(DDL_SUB_CATEGORY)).selectByIndex(0); }  catch (Exception ignored) {}
        try { new Select(driver.findElement(DDL_STATUS)).selectByIndex(0); }        catch (Exception ignored) {}

        // Ensure grid is back (not strict)
        try { uiWait().until(ExpectedConditions.presenceOfElementLocated(GRID_ROWS)); } catch (Exception ignored) {}
    }

    private void clickSearch() {
        AuthorizePolicyPage ap = new AuthorizePolicyPage(driver);
        ap.clicksearchButton();
        acceptAlertIfAppears(2);
        uiWait().until(ExpectedConditions.presenceOfElementLocated(GRID_ROWS));
    }

    // ====== Clean state before every test (light, non-flaky) ======
    @BeforeMethod(alwaysRun = true)
    public void cleanState() {
        goToAuthorize();
        fastReset(); // No heavy waits here to avoid TimeoutException in setup
    }

    // =========================================================================================
    // @Tests  (PA_TC003 removed as requested)
    // =========================================================================================

    /**
     * PA_TC001: Validate filtering by valid Main Category shows only matching rows
     */
    @Test
    public void tc_PA_TC001_ValidFilter() {
        // Step 2: Select valid category (adjust label if needed)
        new Select(driver.findElement(DDL_MAIN_CATEGORY)).selectByVisibleText("Insurance");

        // Step 3: Search
        clickSearch();

        // Step 4: Validate all rows match
        List<WebElement> cells = driver.findElements(GRID_MAINCAT_CELLS);
        for (WebElement c : cells) {
            Assert.assertEquals(
                c.getText().trim(), "Insurance",
                "Found mismatched Main Category value."
            );
        }
    }

    /**
     * PA_TC002: Validate clearing Main Category (default) shows multiple categories
     */
    @Test
    public void tc_PA_TC002_ClearSelection_ShowsAllCategories() {
        // Step 1: Ensure default (placeholder or index 0)
        Select main = new Select(driver.findElement(DDL_MAIN_CATEGORY));
        try { main.selectByVisibleText("Select Main Category"); }
        catch (Exception ignored) { try { main.selectByIndex(0); } catch (Exception ignored2) {} }

        // Step 2: Search
        clickSearch();

        // Step 3: Expect multiple distinct categories in the grid
        List<WebElement> cells = driver.findElements(GRID_MAINCAT_CELLS);
        Set<String> unique = new HashSet<>();
        for (WebElement c : cells) {
            String val = c.getText().trim();
            if (!val.isEmpty()) unique.add(val);
        }
        Assert.assertTrue(unique.size() > 1, "Expected multiple categories; got " + unique);
    }

    /**
     * PA_TC004: Validate Reset returns pagination to first page
     */
    @Test
    public void tc_PA_TC004_Pagination_ResetsToFirstPage() {
        uiWait().until(ExpectedConditions.presenceOfElementLocated(GRID_ROWS));
        uiWait().until(ExpectedConditions.presenceOfElementLocated(PAGER_CONTAINER));

        AuthorizePolicyPage ap = new AuthorizePolicyPage(driver);

        // Step 1: Move to page 3 or 2 (if available)
        boolean moved = ap.goToPageIfExists(3);
        if (!moved) ap.goToPageIfExists(2);

        // Step 2: Reset then verify current==1 and page-1 is not a link
        fastReset();

        // Give pager a brief moment after reset
        try { Thread.sleep(200); } catch (InterruptedException ignored) {}

        Assert.assertTrue(ap.isCurrentPage(1),
                "Pagination did not reset to page 1 (no span '1').");
        Assert.assertFalse(ap.hasPageLink(1),
                "Page 1 appears as a link (should be current page).");
    }
}