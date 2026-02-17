package tests;

import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.*;

import pages.LoginPage;
import pages.SlideBarMenuPage;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PolicyAuthorizeMain {

    // ====== Config ======
    static String baseUrl  = "https://qeaskillhub.cognizant.com/LoginPage?logout=true";
    static String username = "admin_user";
    static String password = "testadmin";

    // ====== Driver + Wait ======
    static WebDriver driver;
    static WebDriverWait wait;

    // ====== Authorize page locators ======
    static final By DDL_MAIN_CATEGORY  = By.id("ContentPlaceHolder_Admin_ddlMainCategory");
    static final By DDL_SUB_CATEGORY   = By.id("ContentPlaceHolder_Admin_ddlSubCategory");
    static final By DDL_STATUS         = By.id("ContentPlaceHolder_Admin_ddlStatus");
    static final By BTN_SEARCH         = By.id("ContentPlaceHolder_Admin_btnSearch");
    static final By BTN_RESET          = By.id("ContentPlaceHolder_Admin_btnReset");

    static final By GRID_ROWS          = By.xpath("//table[@id='ContentPlaceHolder_Admin_gvPolicies']/tbody/tr");
    static final By GRID_MAINCAT_CELLS = By.xpath("//table[@id='ContentPlaceHolder_Admin_gvPolicies']/tbody/tr/td[2]");
    static final By PAGER_CONTAINER    = By.xpath("//tr[contains(@class,'pagination-container')] | //ul[contains(@class,'pagination')]");

    // ====== Counters ======
    static int total = 0, passed = 0, failed = 0;

    public static void main(String[] args) {
        try {
            // 1) Driver
            driver = new ChromeDriver();
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
            driver.manage().window().maximize();
            wait = new WebDriverWait(driver, Duration.ofSeconds(15));

            // 2) Login
            login();

            // 3) Navigate to Authorize page
            goToAuthorize();

            System.out.println("==== RUNNING PolicyAuthorize Test Cases ====");

            tc_PA_TC001_ValidFilter("Insurance");                 // Adjust if your label differs
            tc_PA_TC002_ClearSelection_ShowsAllCategories();
            tc_PA_TC003_Reset_ClearsAllFilters_And_Reloads();
            tc_PA_TC004_Pagination_ResetsToFirstPage();

            System.out.println("==== COMPLETED PolicyAuthorize Test Cases ====");
            System.out.println("\nSUMMARY: Total=" + total + ", Passed=" + passed + ", Failed=" + failed);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (driver != null) {
                try { driver.quit(); } catch (Exception ignored) {}
            }
        }
    }

    // ========= Basic flow helpers =========

    static void login() {
        driver.get(baseUrl);
        LoginPage lp = new LoginPage(driver);
        lp.setUserName(username);
        lp.setPassword(password);
        lp.clickLoginButton();
        acceptAlertIfAppears(2);
    }

    static void goToAuthorize() {
        SlideBarMenuPage menu = new SlideBarMenuPage(driver);
        menu.clickPolicyAuthorize();
        wait.until(ExpectedConditions.presenceOfElementLocated(DDL_MAIN_CATEGORY));
        wait.until(ExpectedConditions.presenceOfElementLocated(GRID_ROWS));
    }

    static void acceptAlertIfAppears(int seconds) {
        try {
            WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(seconds));
            Alert alert = shortWait.until(ExpectedConditions.alertIsPresent());
            alert.accept();
            Thread.sleep(120);
        } catch (Exception ignored) {}
    }

    static void waitForSelectReady(By ddl) {
        new WebDriverWait(driver, Duration.ofSeconds(20))
            .ignoring(NoSuchElementException.class)
            .ignoring(StaleElementReferenceException.class)
            .until(d -> {
                try {
                    WebElement el = d.findElement(ddl);
                    String disabled = el.getAttribute("disabled");
                    if (!el.isDisplayed() || !el.isEnabled() || "true".equalsIgnoreCase(disabled)) return false;
                    Select s = new Select(el);
                    List<WebElement> opts = s.getOptions();
                    return opts != null && !opts.isEmpty();
                } catch (StaleElementReferenceException | NoSuchElementException e) {
                    return false;
                }
            });
    }

    static void ensureDefaultSelected(By ddl) {
        try {
            Select s = new Select(driver.findElement(ddl));
            List<WebElement> opts = s.getOptions();
            if (!opts.isEmpty()) {
                try { s.getFirstSelectedOption(); }   // throws if none
                catch (NoSuchElementException e) { s.selectByIndex(0); }
            }
        } catch (Exception ignored) {}
    }

    static String getSelectedTextSafe(By ddl) {
        try {
            Select s = new Select(driver.findElement(ddl));
            List<WebElement> opts = s.getOptions();
            if (opts.isEmpty()) return "";
            WebElement selected;
            try { selected = s.getFirstSelectedOption(); }
            catch (NoSuchElementException e) { s.selectByIndex(0); selected = s.getFirstSelectedOption(); }
            return selected.getText().trim();
        } catch (Exception e) {
            return "";
        }
    }

    static void clickSearch() {
        driver.findElement(BTN_SEARCH).click();
        acceptAlertIfAppears(2);
        wait.until(ExpectedConditions.presenceOfElementLocated(GRID_ROWS));
    }

    /** Deterministic Reset: grid staleness + reattach + select-ready */
    static void clickReset() {
        WebElement preResetRow = null;
        try { preResetRow = driver.findElement(GRID_ROWS); } catch (Exception ignored) {}

        driver.findElement(BTN_RESET).click();
        acceptAlertIfAppears(2);

        // tiny throttle for postback microtasks
        try { Thread.sleep(200); } catch (InterruptedException ignored) {}

        if (preResetRow != null) {
            try {
                new WebDriverWait(driver, Duration.ofSeconds(10))
                        .until(ExpectedConditions.stalenessOf(preResetRow));
            } catch (Exception ignored) {}
        }
        new WebDriverWait(driver, Duration.ofSeconds(15))
                .until(ExpectedConditions.presenceOfElementLocated(GRID_ROWS));

        // ensure selects are re-bound & enabled with options
        waitForSelectReady(DDL_MAIN_CATEGORY);
        waitForSelectReady(DDL_SUB_CATEGORY);
        waitForSelectReady(DDL_STATUS);

        ensureDefaultSelected(DDL_MAIN_CATEGORY);
        ensureDefaultSelected(DDL_SUB_CATEGORY);
        ensureDefaultSelected(DDL_STATUS);
    }

    static boolean goToPageIfExists(int page) {
        String num = String.valueOf(page);
        By linkBy = By.xpath(
            "//tr[contains(@class,'pagination-container')]//a[normalize-space()='" + num + "']" +
            " | //ul[contains(@class,'pagination')]//a[normalize-space()='" + num + "']"
        );

        List<WebElement> links = driver.findElements(linkBy);
        if (links.isEmpty()) return false;

        WebElement target = links.get(0);

        // If your app has an overlay, add its selector below
        try {
            new WebDriverWait(driver, Duration.ofSeconds(5))
                .until(ExpectedConditions.invisibilityOfElementLocated(
                    By.cssSelector(".loading, .spinner, .modal-backdrop")));
        } catch (Exception ignored) {}

        // Scroll to center and click when clickable, else JS-click
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
            .until(ExpectedConditions.presenceOfElementLocated(GRID_ROWS));
        return true;
    }

    // ========= Test Cases =========

    /** PA_TC001_ValidFilter */
    static void tc_PA_TC001_ValidFilter(String mainCategory) {
        total++;
        System.out.println("\n[PA_TC001] Validate filtering by valid Main Category: " + mainCategory);
        boolean pass = true;
        String msg = "";

        try {
            goToAuthorize();

            // Step 2: Select Main Category
            new Select(driver.findElement(DDL_MAIN_CATEGORY)).selectByVisibleText(mainCategory);
            System.out.println("Step 2: Selected main category = " + mainCategory);

            // Step 3: Search
            clickSearch();
            System.out.println("Step 3: Search clicked; grid present.");

            // Step 4: Validate
            List<WebElement> cells = driver.findElements(GRID_MAINCAT_CELLS);
            for (WebElement cell : cells) {
                String actual = cell.getText().trim();
                if (!actual.equals(mainCategory)) {
                    pass = false;
                    msg = "Found row with Main Category = '" + actual + "' (expected '" + mainCategory + "')";
                    break;
                }
            }
            System.out.println("Step 4: Verified all rows == " + mainCategory);

        } catch (Exception e) {
            pass = false;
            msg = "Exception: " + e.getMessage();
        }
        if (pass) { passed++; System.out.println("PA_TC001: PASS"); }
        else      { failed++; System.out.println("PA_TC001: FAIL - " + msg); }
    }

    /** PA_TC002_ClearSelection_ShowsAllCategories */
    static void tc_PA_TC002_ClearSelection_ShowsAllCategories() {
        total++;
        System.out.println("\n[PA_TC002] Validate default selection shows multiple categories");
        boolean pass = true;
        String msg = "";

        try {
            goToAuthorize();

            // Step 1: Default
            Select main = new Select(driver.findElement(DDL_MAIN_CATEGORY));
            try { main.selectByVisibleText("Select Main Category"); }
            catch (Exception ignored) { main.selectByIndex(0); }
            System.out.println("Step 1: Default/placeholder selected");

            // Step 2: Search
            clickSearch();
            System.out.println("Step 2: Search clicked");

            // Step 3: Distinct categories > 1
            List<WebElement> cells = driver.findElements(GRID_MAINCAT_CELLS);
            Set<String> unique = new HashSet<>();
            for (WebElement c : cells) {
                String val = c.getText().trim();
                if (!val.isEmpty()) unique.add(val);
            }
            System.out.println("Step 3: Unique categories found = " + unique);
            if (unique.size() <= 1) {
                pass = false;
                msg = "Expected multiple categories; got " + unique;
            }

        } catch (Exception e) {
            pass = false;
            msg = "Exception: " + e.getMessage();
        }
        if (pass) { passed++; System.out.println("PA_TC002: PASS"); }
        else      { failed++; System.out.println("PA_TC002: FAIL - " + msg); }
    }

    /** PA_TC003_Reset_ClearsAllFilters_And_Reloads */
    static void tc_PA_TC003_Reset_ClearsAllFilters_And_Reloads() {
        total++;
        System.out.println("\n[PA_TC003] Validate Reset clears filters and reloads all policies");
        boolean pass = true;
        String msg = "";

        try {
            goToAuthorize();

            // Step 1: Set non-default filters and Search
            new Select(driver.findElement(DDL_MAIN_CATEGORY)).selectByVisibleText("Life"); // adjust if needed
            waitForSelectReady(DDL_SUB_CATEGORY);

            Select sub = new Select(driver.findElement(DDL_SUB_CATEGORY));
            if (sub.getOptions().size() > 1) {
                for (WebElement opt : sub.getOptions()) {
                    String t = opt.getText().trim();
                    if (!t.equalsIgnoreCase("All") && !t.isEmpty()) {
                        sub.selectByVisibleText(t);
                        break;
                    }
                }
            }

            Select status = new Select(driver.findElement(DDL_STATUS));
            boolean pickedApproved = false;
            for (WebElement opt : status.getOptions()) {
                String t = opt.getText().trim();
                if (t.equalsIgnoreCase("Approved")) {
                    status.selectByVisibleText("Approved");
                    pickedApproved = true;
                    break;
                }
            }
            if (!pickedApproved && status.getOptions().size() > 1) {
                for (WebElement opt : status.getOptions()) {
                    String t = opt.getText().trim();
                    if (!t.equalsIgnoreCase("All") && !t.isEmpty()) {
                        status.selectByVisibleText(t);
                        break;
                    }
                }
            }

            clickSearch();
            List<WebElement> filteredRows = driver.findElements(GRID_ROWS);
            System.out.println("Step 1: Filtered row count = " + filteredRows.size());

            // Step 2: Click Reset; verify defaults
            clickReset();
            String mainTxt = getSelectedTextSafe(DDL_MAIN_CATEGORY);
            String subTxt  = getSelectedTextSafe(DDL_SUB_CATEGORY);
            String stTxt   = getSelectedTextSafe(DDL_STATUS);

            boolean mainOk = mainTxt.isEmpty() || mainTxt.toLowerCase().contains("select");
            boolean subOk  = subTxt.equalsIgnoreCase("All") || subTxt.equalsIgnoreCase("-- All --") || subTxt.equalsIgnoreCase("All Sub Categories");
            boolean stOk   = stTxt.equalsIgnoreCase("All");

            System.out.println("Step 2: Defaults after Reset -> Main=" + mainTxt + ", Sub=" + subTxt + ", Status=" + stTxt);
            if (!mainOk || !subOk || !stOk) {
                pass = false;
                msg = "Defaults not set correctly.";
            }

            // Step 3: Verify variety after reset
            wait.until(ExpectedConditions.presenceOfElementLocated(GRID_ROWS));
            List<WebElement> cells = driver.findElements(GRID_MAINCAT_CELLS);
            Set<String> unique = new HashSet<>();
            for (WebElement c : cells) {
                String val = c.getText().trim();
                if (!val.isEmpty()) unique.add(val);
            }
            System.out.println("Step 3: Unique categories after reset = " + unique);
            if (unique.size() <= 1) {
                pass = false;
                msg = "Expected multiple categories after reset; got " + unique;
            }

        } catch (Exception e) {
            pass = false;
            msg = "Exception: " + e.getMessage();
        }
        if (pass) { passed++; System.out.println("PA_TC003: PASS"); }
        else      { failed++; System.out.println("PA_TC003: FAIL - " + msg); }
    }

    /** PA_TC004_Pagination_ResetsToFirstPage */
    static void tc_PA_TC004_Pagination_ResetsToFirstPage() {
        total++;
        System.out.println("\n[PA_TC004] Validate Reset returns pagination to page 1");
        boolean pass = true;
        String msg = "";

        try {
            goToAuthorize();

            // Step 1: Go to later page (3 or 2)
            boolean moved = goToPageIfExists(3);
            if (!moved) moved = goToPageIfExists(2);
            System.out.println("Step 1: Moved to later page? " + moved);

            // Step 2: Reset, then verify current=1 & no link for page 1
            clickReset();

            boolean isCurrent1 = isCurrentPage(1);
            boolean hasLink1   = hasPageLink(1);

            System.out.println("Step 2: isCurrent(1)=" + isCurrent1 + ", hasLink(1)=" + hasLink1);
            if (!isCurrent1 || hasLink1) {
                pass = false;
                msg = "Expected current page=1 (span) and no link for page 1.";
            }

        } catch (Exception e) {
            pass = false;
            msg = "Exception: " + e.getMessage();
        }
        if (pass) { passed++; System.out.println("PA_TC004: PASS"); }
        else      { failed++; System.out.println("PA_TC004: FAIL - " + msg); }
    }

    // ========= Pagination state checks =========

    static boolean isCurrentPage(int page) {
        String num = String.valueOf(page);
        List<WebElement> spans = driver.findElements(By.xpath(
            "//tr[contains(@class,'pagination-container')]//span[normalize-space()='" + num + "']" +
            " | //ul[contains(@class,'pagination')]//span[normalize-space()='" + num + "']"
        ));
        if (!spans.isEmpty() && spans.get(0).isDisplayed()) return true;

        List<WebElement> actives = driver.findElements(By.xpath(
            "//ul[contains(@class,'pagination')]//li[contains(@class,'active')]//span[normalize-space()='" + num + "']"
        ));
        return !actives.isEmpty() && actives.get(0).isDisplayed();
    }

    static boolean hasPageLink(int page) {
        String num = String.valueOf(page);
        List<WebElement> links = driver.findElements(By.xpath(
            "//tr[contains(@class,'pagination-container')]//a[normalize-space()='" + num + "']" +
            " | //ul[contains(@class,'pagination')]//a[normalize-space()='" + num + "']"
        ));
        return !links.isEmpty() && links.get(0).isDisplayed();
    }
}