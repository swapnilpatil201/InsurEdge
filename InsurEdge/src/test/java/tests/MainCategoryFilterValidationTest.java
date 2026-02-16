package tests;

import base.BaseTest;
import data.ExcelDataProvider;

import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import pages.AuthorizePolicyPage;
import pages.HeaderPage;
import pages.SlideBarMenuPage;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainCategoryFilterValidationTest extends BaseTest {

    // ======== Common locators (PUBLIC as requested) ========
    public static final By GRID_ROWS          = By.xpath("//table[@id='ContentPlaceHolder_Admin_gvPolicies']/tbody/tr");
    public static final By GRID_MAINCAT_CELLS = By.xpath("//table[@id='ContentPlaceHolder_Admin_gvPolicies']/tbody/tr/td[2]");
    public static final By PAGER_CONTAINER    = By.xpath("//tr[contains(@class,'pagination-container')]");

    // Filter <select> elements
    public static final By DDL_MAIN_CATEGORY  = By.id("ContentPlaceHolder_Admin_ddlMainCategory");
    public static final By DDL_SUB_CATEGORY   = By.id("ContentPlaceHolder_Admin_ddlSubCategory");
    public static final By DDL_STATUS         = By.id("ContentPlaceHolder_Admin_ddlStatus");

    // ======== Public helpers ========

    /** Public: Standard explicit wait helper (12 seconds). */
    public WebDriverWait uiWait() {
        return new WebDriverWait(driver, Duration.ofSeconds(12));
    }

    /** Public: Navigate to Policy → Authorized page and wait until grid skeleton is present. */
    public void goToAuthorized() {
        // If your UI requires opening the sidebar, uncomment:
        HeaderPage header = new HeaderPage(driver);
        // header.clickSideBarButton();

        SlideBarMenuPage menu = new SlideBarMenuPage(driver);
        menu.clickPolicyAuthorize(); // expands Policy and opens Authorized

        // Wait for key elements so we know the page is ready
        uiWait().until(ExpectedConditions.presenceOfElementLocated(DDL_MAIN_CATEGORY));
        uiWait().until(ExpectedConditions.presenceOfElementLocated(GRID_ROWS));
    }

    /**
     * Public: Reset all filters to defaults and ensure grid is showing unfiltered list.
     * Uses page object actions when available and falls back to Select for resiliency.
     */
    public void resetAuthorizedFilters() {
        AuthorizePolicyPage ap = new AuthorizePolicyPage(driver);

        // Click the page's Reset button (preferred)
        try {
            ap.clickResetButton();
        } catch (Exception ignored) {
            // Fall back: manually set selects to default values
            try {
                // Main Category → select first option (usually "Select Main Category")
                Select main = new Select(driver.findElement(DDL_MAIN_CATEGORY));
                main.selectByIndex(0);
            } catch (Exception e) { /* ignore */ }

            try {
                // Sub Category → "All"
                Select sub = new Select(driver.findElement(DDL_SUB_CATEGORY));
                try {
                    sub.selectByVisibleText("All");
                } catch (Exception notFound) {
                    sub.selectByIndex(0); // fallback
                }
            } catch (Exception e) { /* ignore */ }

            try {
                // Status → "All"
                Select st = new Select(driver.findElement(DDL_STATUS));
                try {
                    st.selectByVisibleText("All");
                } catch (Exception notFound) {
                    st.selectByIndex(0); // fallback
                }
            } catch (Exception e) { /* ignore */ }
        }

        // After Reset, make sure dropdowns reflect defaults (tolerant to slight text differences)
        uiWait().until(driver1 -> {
            String mainTxt = new Select(driver1.findElement(DDL_MAIN_CATEGORY))
                    .getFirstSelectedOption().getText().trim().toLowerCase();
            return mainTxt.contains("select") || mainTxt.isEmpty();
        });

        uiWait().until(driver1 ->
                new Select(driver1.findElement(DDL_SUB_CATEGORY))
                        .getFirstSelectedOption().getText().trim().equalsIgnoreCase("All")
                || new Select(driver1.findElement(DDL_SUB_CATEGORY)).getFirstSelectedOption().getText().trim().equalsIgnoreCase("-- All --")
                || new Select(driver1.findElement(DDL_SUB_CATEGORY)).getFirstSelectedOption().getText().trim().equalsIgnoreCase("All Sub Categories")
        );

        uiWait().until(driver1 ->
                new Select(driver1.findElement(DDL_STATUS))
                        .getFirstSelectedOption().getText().trim().equalsIgnoreCase("All")
        );

        // Ensure grid is loaded with unfiltered results.
        // If Reset does not auto-load, click Search.
        try {
            uiWait().until(ExpectedConditions.presenceOfElementLocated(GRID_ROWS));
        } catch (TimeoutException te) {
            try {
                ap.clicksearchButton();
                uiWait().until(ExpectedConditions.presenceOfElementLocated(GRID_ROWS));
            } catch (Exception ignored) {
                // As a last resort, refresh
                driver.navigate().refresh();
                uiWait().until(ExpectedConditions.presenceOfElementLocated(DDL_MAIN_CATEGORY));
                uiWait().until(ExpectedConditions.presenceOfElementLocated(GRID_ROWS));
            }
        }
    }

    /** Public: Convenience method to select default main category no matter what its display text is. */
    public void selectDefaultMainCategory() {
        Select main = new Select(driver.findElement(DDL_MAIN_CATEGORY));
        // First try to click explicit default label
        try {
            main.selectByVisibleText("Select Main Category");
            return;
        } catch (Exception ignored) { }
        // Fallback to first option
        main.selectByIndex(0);
    }

    // ======== Clean state BEFORE EVERY TEST ========
    @BeforeMethod(alwaysRun = true)
    public void cleanState() {
        goToAuthorized();
        resetAuthorizedFilters();
    }

    // ==========================================
    //                TESTS
    // ==========================================

    @Test(dataProvider = "mainCategoryDP", dataProviderClass = ExcelDataProvider.class)
    public void validateMainCategoryFilter(String mainCategory) {
        WebDriverWait wait = uiWait();

        // Filters are already reset by @BeforeMethod
        AuthorizePolicyPage ap = new AuthorizePolicyPage(driver);
        ap.selectMainCategory(mainCategory);
        ap.clicksearchButton();

        // Validate grid shows only the selected main category
        wait.until(ExpectedConditions.presenceOfElementLocated(GRID_ROWS));
        List<WebElement> cells = driver.findElements(GRID_MAINCAT_CELLS);

        // If a zero-result should be considered a failure, uncomment:
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

        // Filters are already reset; ensure default on main category explicitly
        selectDefaultMainCategory();

        // Click Search
        AuthorizePolicyPage ap = new AuthorizePolicyPage(driver);
        ap.clicksearchButton();

        // Read Main Category values
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

        // 1) Apply ALL filters (non-default values)
        AuthorizePolicyPage ap = new AuthorizePolicyPage(driver);

        // Main Category (non-default; pick any valid option in your env)
        ap.selectMainCategory("Life"); // change if needed

        // Wait for ASP.NET postback to refresh Sub Category
        wait.until(ExpectedConditions.presenceOfElementLocated(DDL_SUB_CATEGORY));

        // Sub Category: pick first non-default if available
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

        // 2) Click Search and wait for grid
        ap.clicksearchButton();
        wait.until(ExpectedConditions.presenceOfElementLocated(GRID_ROWS));

        // Capture row count before reset (optional diagnostic)
        int filteredCount = driver.findElements(GRID_ROWS).size();

        // 3) Click Reset (should clear filters and reload)
        ap.clickResetButton();

        // 4) Verify filters reset to defaults (wait until each shows default)
        wait.until(driver1 -> {
            String txt = new Select(driver1.findElement(DDL_MAIN_CATEGORY))
                .getFirstSelectedOption().getText().trim();
            return txt.equalsIgnoreCase("Select Main Category") || txt.isEmpty();
        });

        String mainCatText = new Select(driver.findElement(DDL_MAIN_CATEGORY))
            .getFirstSelectedOption().getText().trim();
        Assert.assertTrue(
            mainCatText.equalsIgnoreCase("Select Main Category") || mainCatText.isEmpty(),
            "Main Category not reset. Actual: " + mainCatText
        );

        // Sub Category default → "All"
        wait.until(driver1 ->
            new Select(driver1.findElement(DDL_SUB_CATEGORY))
                .getFirstSelectedOption().getText().trim().equalsIgnoreCase("All")
        );
        String subCatText = new Select(driver.findElement(DDL_SUB_CATEGORY))
            .getFirstSelectedOption().getText().trim();
        Assert.assertTrue(
            subCatText.equalsIgnoreCase("All"),
            "Sub Category not reset to 'All'. Actual: " + subCatText
        );

        // Status default → "All"
        wait.until(driver1 ->
            new Select(driver1.findElement(DDL_STATUS))
                .getFirstSelectedOption().getText().trim().equalsIgnoreCase("All")
        );
        String statusText = new Select(driver.findElement(DDL_STATUS))
            .getFirstSelectedOption().getText().trim();
        Assert.assertTrue(
            statusText.equalsIgnoreCase("All"),
            "Status not reset to 'All'. Actual: " + statusText
        );

        // 5) Validate grid shows full/unfiltered list (ideally > previously filtered count)
        wait.until(ExpectedConditions.presenceOfElementLocated(GRID_ROWS));
        List<WebElement> mainCatCells = driver.findElements(GRID_MAINCAT_CELLS);

        Set<String> uniqueCategories = new HashSet<>();
        for (WebElement cell : mainCatCells) {
            String value = cell.getText().trim();
            if (!value.isEmpty()) uniqueCategories.add(value);
        }

        Assert.assertTrue(
            uniqueCategories.size() > 1,
            "Reset did not restore full list. Unique categories: " + uniqueCategories + " | filtered rows before reset: " + filteredCount
        );
    }

    @Test
    public void validatePaginationResetsToFirstPageAfterReset() {
        WebDriverWait wait = uiWait();

        // 1) We're already on Authorized with defaults due to @BeforeMethod
        wait.until(ExpectedConditions.presenceOfElementLocated(GRID_ROWS));
        wait.until(ExpectedConditions.presenceOfElementLocated(PAGER_CONTAINER));

        // 2) Move to a later page (prefer page 3; else 2)
        AuthorizePolicyPage ap = new AuthorizePolicyPage(driver);
        boolean moved = ap.goToPageIfExists(3);
        if (!moved) {
            ap.goToPageIfExists(2);
        }

        // 3) Click Reset
        ap.clickResetButton();

        // 4) Wait for first page to render again
        wait.until(ExpectedConditions.presenceOfElementLocated(GRID_ROWS));
        wait.until(ExpectedConditions.presenceOfElementLocated(PAGER_CONTAINER));

        // 5) Validate: current page is 1 (shown as a <span>1</span>) and not a link
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