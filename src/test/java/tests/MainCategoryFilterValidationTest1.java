package tests;

import base.BaseTest;
import data.ExcelDataProvider;

import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.UnhandledAlertException;
import org.openqa.selenium.remote.UnreachableBrowserException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;

import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import pages.AuthorizePolicyPage;
import pages.SlideBarMenuPage;
import pages.LoginPage;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Authorized policies - Main Category filter validations.
 * Hardened to:
 *  - recover Chrome session if it dies (without touching BaseTest)
 *  - avoid fragile alert calls when browser is unreachable
 *  - tolerate ASP.NET postbacks (no selected option windows)
 */
public class MainCategoryFilterValidationTest1 extends BaseTest {

    // ======== Common locators (PUBLIC as requested) ========
    public static final By GRID_ROWS          = By.xpath("//table[@id='ContentPlaceHolder_Admin_gvPolicies']/tbody/tr");
    public static final By GRID_MAINCAT_CELLS = By.xpath("//table[@id='ContentPlaceHolder_Admin_gvPolicies']/tbody/tr/td[2]");
    public static final By PAGER_CONTAINER    = By.xpath("//tr[contains(@class,'pagination-container')] | //ul[contains(@class,'pagination')]");

    // Filter <select> elements
    public static final By DDL_MAIN_CATEGORY  = By.id("ContentPlaceHolder_Admin_ddlMainCategory");
    public static final By DDL_SUB_CATEGORY   = By.id("ContentPlaceHolder_Admin_ddlSubCategory");
    public static final By DDL_STATUS         = By.id("ContentPlaceHolder_Admin_ddlStatus");

    // ======== Helpers ========

    public WebDriverWait uiWait() {
        return new WebDriverWait(driver, Duration.ofSeconds(15));
    }

    /** Soft alert accept; won't fail if browser is gone. */
    private void acceptAlertIfAppears(int seconds) {
        try {
            Alert alert = new WebDriverWait(driver, Duration.ofSeconds(seconds))
                    .ignoring(NoAlertPresentException.class)
                    .until(ExpectedConditions.alertIsPresent());
            if (alert != null) {
                alert.accept();
                try { Thread.sleep(100); } catch (InterruptedException ignored) {}
            }
        } catch (TimeoutException | NoAlertPresentException | UnreachableBrowserException | UnhandledAlertException ignored) {
        } catch (Exception ignored) {
        }
    }

    /** Wait until a <select> is displayed, enabled, and has options. */
    private void waitForSelectReady(By ddl) {
        new WebDriverWait(driver, Duration.ofSeconds(15))
                .ignoring(NoSuchElementException.class)
                .ignoring(StaleElementReferenceException.class)
                .until(d -> {
                    try {
                        WebElement el = d.findElement(ddl);
                        if (!el.isDisplayed() || !el.isEnabled()) return false;
                        Select s = new Select(el);
                        List<WebElement> opts = s.getOptions();
                        return opts != null && !opts.isEmpty();
                    } catch (StaleElementReferenceException | NoSuchElementException e) {
                        return false;
                    }
                });
    }

    /** Get selected text safely; if none selected, select index 0 first. */
    private String getSelectedTextSafe(By ddl) {
        try {
            Select s = new Select(driver.findElement(ddl));
            List<WebElement> opts = s.getOptions();
            if (opts.isEmpty()) return "";
            WebElement selected;
            try {
                selected = s.getFirstSelectedOption();
            } catch (NoSuchElementException e) {
                s.selectByIndex(0);
                selected = s.getFirstSelectedOption();
            }
            return selected.getText().trim();
        } catch (Exception e) {
            return "";
        }
    }

    /** Ensure a select has a selection (index 0) if none selected. */
    private void ensureDefaultSelected(By ddl) {
        try {
            Select s = new Select(driver.findElement(ddl));
            List<WebElement> opts = s.getOptions();
            if (!opts.isEmpty()) {
                try {
                    s.getFirstSelectedOption();
                } catch (NoSuchElementException e) {
                    s.selectByIndex(0);
                }
            }
        } catch (Exception ignored) { }
    }

    /** Recover driver if session is dead, without touching BaseTest. */
    private void recoverDriverIfDead() {
        try {
            // cheap ping
            driver.getCurrentUrl();
        } catch (UnreachableBrowserException | org.openqa.selenium.NoSuchSessionException e) {
            try { driver.quit(); } catch (Exception ignored) {}

            // Recreate driver and login again (BaseTest fields are public)
            driver = new org.openqa.selenium.chrome.ChromeDriver();
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
            driver.manage().window().maximize();

            driver.get(baseUrl);
            LoginPage lp = new LoginPage(driver);
            lp.setUserName(username);
            lp.setPassword(password);
            lp.clickLoginButton();

            // small post-login alert drain (if any)
            acceptAlertIfAppears(2);
        }
    }

    /** Navigate to Policy → Authorized and wait for base elements (no direct alert.switchTo). */
    public void goToAuthorized() {
        // DO NOT call switchTo().alert() here (can crash when browser died)
        SlideBarMenuPage menu = new SlideBarMenuPage(driver);
        menu.clickPolicyAuthorize(); // internally alert-safe

        uiWait().until(ExpectedConditions.presenceOfElementLocated(DDL_MAIN_CATEGORY));
        uiWait().until(ExpectedConditions.presenceOfElementLocated(GRID_ROWS));
    }

    /** Reset filters without using fragile selected-option waits. */
    public void resetAuthorizedFilters() {
        AuthorizePolicyPage ap = new AuthorizePolicyPage(driver);

        // 1) Click Reset (preferred) and absorb any alert
        try {
            ap.clickResetButton();
            acceptAlertIfAppears(2);
        } catch (Exception ignored) {
            // Fallback: set defaults manually
            try { new Select(driver.findElement(DDL_MAIN_CATEGORY)).selectByIndex(0); } catch (Exception ignored2) {}
            try {
                Select sub = new Select(driver.findElement(DDL_SUB_CATEGORY));
                try { sub.selectByVisibleText("All"); } catch (Exception nf) { sub.selectByIndex(0); }
            } catch (Exception ignored2) {}
            try {
                Select st = new Select(driver.findElement(DDL_STATUS));
                try { st.selectByVisibleText("All"); } catch (Exception nf) { st.selectByIndex(0); }
            } catch (Exception ignored2) {}
        }

        // 2) Wait each select to be populated & stable
        waitForSelectReady(DDL_MAIN_CATEGORY);
        waitForSelectReady(DDL_SUB_CATEGORY);
        waitForSelectReady(DDL_STATUS);

        // 3) Ensure a selection exists in each
        ensureDefaultSelected(DDL_MAIN_CATEGORY);
        ensureDefaultSelected(DDL_SUB_CATEGORY);
        ensureDefaultSelected(DDL_STATUS);

        // 4) Verify defaults via SAFE getters
        String mainTxt = getSelectedTextSafe(DDL_MAIN_CATEGORY);
        Assert.assertTrue(
                mainTxt.isEmpty() || mainTxt.toLowerCase().contains("select"),
                "Main Category not at default. Actual: " + mainTxt
        );

        String subTxt = getSelectedTextSafe(DDL_SUB_CATEGORY);
        Assert.assertTrue(
                subTxt.equalsIgnoreCase("All")
                        || subTxt.equalsIgnoreCase("-- All --")
                        || subTxt.equalsIgnoreCase("All Sub Categories"),
                "Sub Category not reset to 'All'. Actual: " + subTxt
        );

        String statusTxt = getSelectedTextSafe(DDL_STATUS);
        Assert.assertTrue(
                statusTxt.equalsIgnoreCase("All"),
                "Status not reset to 'All'. Actual: " + statusTxt
        );

        // 5) Ensure grid appears; if not, click Search; final fallback: refresh
        try {
            uiWait().until(ExpectedConditions.presenceOfElementLocated(GRID_ROWS));
        } catch (TimeoutException te) {
            try {
                ap.clicksearchButton();
                acceptAlertIfAppears(2);
                uiWait().until(ExpectedConditions.presenceOfElementLocated(GRID_ROWS));
            } catch (Exception ignored) {
                driver.navigate().refresh();
                uiWait().until(ExpectedConditions.presenceOfElementLocated(DDL_MAIN_CATEGORY));
                uiWait().until(ExpectedConditions.presenceOfElementLocated(GRID_ROWS));
            }
        }
    }

    // ======== Clean state BEFORE EVERY TEST ========
    @BeforeMethod(alwaysRun = true)
    public void cleanState() {
        // If Chrome died between tests, recover the driver and log in again
        recoverDriverIfDead();

        goToAuthorized();
        resetAuthorizedFilters();
    }

    // ==========================================
    //                  TESTS
    // ==========================================

    @Test(dataProvider = "mainCategoryDP", dataProviderClass = ExcelDataProvider.class)
    public void validateMainCategoryFilter(String mainCategory) {
        WebDriverWait wait = uiWait();

        AuthorizePolicyPage ap = new AuthorizePolicyPage(driver);
        ap.selectMainCategory(mainCategory);
        ap.clicksearchButton();
        acceptAlertIfAppears(2);

        wait.until(ExpectedConditions.presenceOfElementLocated(GRID_ROWS));
        List<WebElement> cells = driver.findElements(GRID_MAINCAT_CELLS);

        // If zero-result should fail, uncomment:
        // Assert.assertTrue(cells.size() > 0, "No results for category: " + mainCategory);

        for (WebElement cell : cells) {
            String actual = cell.getText().trim();
            Assert.assertEquals(
                actual, mainCategory,
                "Mismatched category. Expected: " + mainCategory + " | Actual: " + actual
            );
        }
    }

    @Test
    public void validateClearSelectionShowsAllCategories() {
        WebDriverWait wait = uiWait();

        // Default main category
        Select main = new Select(driver.findElement(DDL_MAIN_CATEGORY));
        try { main.selectByVisibleText("Select Main Category"); } catch (Exception ignored) { main.selectByIndex(0); }

        AuthorizePolicyPage ap = new AuthorizePolicyPage(driver);
        ap.clicksearchButton();
        acceptAlertIfAppears(2);

        wait.until(ExpectedConditions.presenceOfElementLocated(GRID_ROWS));
        List<WebElement> cells = driver.findElements(GRID_MAINCAT_CELLS);

        Set<String> uniqueCategories = new HashSet<>();
        for (WebElement cell : cells) {
            String value = cell.getText().trim();
            if (!value.isEmpty()) uniqueCategories.add(value);
        }

        Assert.assertTrue(
            uniqueCategories.size() > 1,
            "Expected multiple main categories after clearing filter, but got: " + uniqueCategories
        );
    }

    @Test
    public void validateResetClearsAllFiltersAndReloadsAllPolicies() {
        WebDriverWait wait = uiWait();

        AuthorizePolicyPage ap = new AuthorizePolicyPage(driver);

        // Main Category (non-default)
        ap.selectMainCategory("Life"); // adjust if needed

        // Wait for sub category rebind
        waitForSelectReady(DDL_SUB_CATEGORY);

        // Sub Category: first non-default if available
        Select subCat = new Select(driver.findElement(DDL_SUB_CATEGORY));
        if (subCat.getOptions().size() > 1) {
            for (WebElement opt : subCat.getOptions()) {
                String t = opt.getText().trim();
                if (!t.equalsIgnoreCase("All") && !t.isEmpty()) {
                    ap.selectSubCategory(t);
                    break;
                }
            }
        }

        // Status: prefer "Approved"; else first non-default
        Select status = new Select(driver.findElement(DDL_STATUS));
        boolean pickedApproved = false;
        for (WebElement opt : status.getOptions()) {
            if (opt.getText().trim().equalsIgnoreCase("Approved")) {
                ap.selectStatus("Approved");
                pickedApproved = true;
                break;
            }
        }
        if (!pickedApproved && status.getOptions().size() > 1) {
            for (WebElement opt : status.getOptions()) {
                String t = opt.getText().trim();
                if (!t.equalsIgnoreCase("All") && !t.isEmpty()) {
                    ap.selectStatus(t);
                    break;
                }
            }
        }

        // Search
        ap.clicksearchButton();
        acceptAlertIfAppears(2);
        wait.until(ExpectedConditions.presenceOfElementLocated(GRID_ROWS));
        int filteredCount = driver.findElements(GRID_ROWS).size();

        // Reset
        ap.clickResetButton();
        acceptAlertIfAppears(2);

        // Wait selects ready and ensure selection
        waitForSelectReady(DDL_MAIN_CATEGORY);
        waitForSelectReady(DDL_SUB_CATEGORY);
        waitForSelectReady(DDL_STATUS);
        ensureDefaultSelected(DDL_MAIN_CATEGORY);
        ensureDefaultSelected(DDL_SUB_CATEGORY);
        ensureDefaultSelected(DDL_STATUS);

        String mainTxt = getSelectedTextSafe(DDL_MAIN_CATEGORY);
        Assert.assertTrue(
                mainTxt.isEmpty() || mainTxt.toLowerCase().contains("select"),
                "Main Category not reset. Actual: " + mainTxt
        );

        String subTxt = getSelectedTextSafe(DDL_SUB_CATEGORY);
        Assert.assertTrue(
                subTxt.equalsIgnoreCase("All")
                        || subTxt.equalsIgnoreCase("-- All --")
                        || subTxt.equalsIgnoreCase("All Sub Categories"),
                "Sub Category not reset to 'All'. Actual: " + subTxt
        );

        String statusTxt = getSelectedTextSafe(DDL_STATUS);
        Assert.assertTrue(statusTxt.equalsIgnoreCase("All"),
                "Status not reset to 'All'. Actual: " + statusTxt);

        // Validate grid appears to be unfiltered (variety)
        wait.until(ExpectedConditions.presenceOfElementLocated(GRID_ROWS));
        List<WebElement> mainCatCells = driver.findElements(GRID_MAINCAT_CELLS);

        Set<String> uniqueCategories = new HashSet<>();
        for (WebElement cell : mainCatCells) {
            String value = cell.getText().trim();
            if (!value.isEmpty()) uniqueCategories.add(value);
        }

        Assert.assertTrue(
                uniqueCategories.size() > 1,
                "Reset did not restore full list. Unique categories: " + uniqueCategories +
                        " | filtered rows before reset: " + filteredCount
        );
    }

    @Test
    public void validatePaginationResetsToFirstPageAfterReset() {
        WebDriverWait wait = uiWait();

        wait.until(ExpectedConditions.presenceOfElementLocated(GRID_ROWS));
        wait.until(ExpectedConditions.presenceOfElementLocated(PAGER_CONTAINER));

        AuthorizePolicyPage ap = new AuthorizePolicyPage(driver);
        boolean moved = ap.goToPageIfExists(3);
        if (!moved) ap.goToPageIfExists(2);

        ap.clickResetButton();
        acceptAlertIfAppears(2);

        wait.until(ExpectedConditions.presenceOfElementLocated(GRID_ROWS));
        wait.until(ExpectedConditions.presenceOfElementLocated(PAGER_CONTAINER));

        Assert.assertTrue(
                ap.isCurrentPage(1),
                "Pagination did not reset to first page after Reset (span '1' not present)."
        );
        Assert.assertFalse(
                ap.hasPageLink(1),
                "Page 1 appears as a link instead of current page after Reset."
        );
    }
}