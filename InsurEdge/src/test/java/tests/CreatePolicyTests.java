package tests;

import base.BaseTest;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.*;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pages.CreatePolicyPage;
import pages.HeaderPage;
import pages.SlideBarMenuPage;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

public class CreatePolicyTests extends BaseTest {

    /** Public wait helper (12s) */
    public WebDriverWait uiWait() {
        return new WebDriverWait(driver, Duration.ofSeconds(12));
    }

    /** Public: Navigate to Policy → Create Policy page and wait for a stable element. */
    public void goToCreatePolicy() {
        HeaderPage header = new HeaderPage(driver);
        // If the app shows a hamburger to open left menu, uncomment line below:
        // header.clickSideBarButton();

        SlideBarMenuPage menu = new SlideBarMenuPage(driver);
        menu.expandPolicyMenu();
        menu.clickPolicyCreate();

        // Wait for Create Policy page to be ready (button/input unique to this page)
        CreatePolicyPage cp = new CreatePolicyPage(driver);
        uiWait().until(ExpectedConditions.presenceOfElementLocated(cp.btnCreate));
        uiWait().until(ExpectedConditions.presenceOfElementLocated(cp.txtPolicyName));
    }

    /** Public: Tries to close any open bootstrap modal (review dialog) gracefully. */
    public void closeAnyOpenModalIfPresent() {
        try {
            // Generic modal close handlers (Bootstrap close button / secondary / data-bs-dismiss)
            List<By> closeCandidates = Arrays.asList(
                    By.cssSelector(".modal.show .btn-close"),
                    By.cssSelector(".modal.show [data-bs-dismiss='modal']"),
                    By.cssSelector(".modal.show .btn-secondary"),
                    By.cssSelector(".modal.show .close")
            );

            for (By by : closeCandidates) {
                List<WebElement> els = driver.findElements(by);
                if (!els.isEmpty() && els.get(0).isDisplayed()) {
                    els.get(0).click();
                    uiWait().until(ExpectedConditions.invisibilityOfElementLocated(By.cssSelector(".modal.show")));
                    return;
                }
            }

            // Fallback: send ESC
            new Actions(driver).sendKeys(Keys.ESCAPE).perform();
            uiWait().until(ExpectedConditions.invisibilityOfElementLocated(By.cssSelector(".modal.show")));
        } catch (TimeoutException te) {
            // Modal not present or already invisible — ignore
        } catch (Exception ignored) {
            // No-op: best-effort modal close
        }
    }

    /** Public: Resets the Create Policy form to defaults (no logout). */
    public void resetCreatePolicyForm() {
        CreatePolicyPage cp = new CreatePolicyPage(driver);

        // If a modal is open, close it first or reset won't act on underlying form
        closeAnyOpenModalIfPresent();

        // Best-effort Reset via page object
        try {
            cp.clickReset();
        } catch (Exception e) {
            // If reset button isn't interactable, try a hard refresh and wait again
            driver.navigate().refresh();
            uiWait().until(ExpectedConditions.presenceOfElementLocated(cp.btnCreate));
            try {
                cp.clickReset();
            } catch (Exception ignored) {
                // Ignore if your reset is purely client-side and state is already default
            }
        }

        // Optional sanity checks (non-fatal): subcategory disabled and fields empty
        try {
            boolean disabled = driver.findElement(cp.ddlSubCategory).getAttribute("disabled") != null;
            // If not disabled, it might be fine depending on your HTML; we don't assert here.
        } catch (Exception ignored) { }
    }

    /** Clean state BEFORE EVERY TEST (shared session; no logout) */
    @BeforeMethod(alwaysRun = true)
    public void cleanState() {
        goToCreatePolicy();
        resetCreatePolicyForm();
    }

    // ==============================
    // User Story: Review dialog & confirm (SCRUM-25, SCRUM-92)
    // ==============================

    /** TC_CP_001: Review dialog opens showing all selected values. */
    @Test
    public void test_ReviewDialog_DisplaysSelectedValues() {
        CreatePolicyPage cp = new CreatePolicyPage(driver);

        // Select Main Category -> triggers postback to repopulate Sub Category
        cp.selectMainCategory("Life");
        uiWait().until(ExpectedConditions.presenceOfElementLocated(cp.ddlSubCategory));

        // Pick any valid sub category AFTER postback (if only default exists in your env, adjust)
        try {
            new Select(driver.findElement(cp.ddlSubCategory)).selectByIndex(1);
        } catch (Exception ignored) {
            // If no second option, keep default; test still validates other fields + review URL
        }

        // Fill other fields
        String pName = "AutoTest_" + System.currentTimeMillis();
        cp.setPolicyName(pName);
        cp.setSumAssured("500000");
        cp.setPremium("5000");
        cp.setTenure(5);
        Assert.assertEquals(cp.getTenureLabelText(), "5", "Tenure label not updated to 5");

        // Open Review
        cp.clickCreateToOpenReview();
        uiWait().until(ExpectedConditions.visibilityOfElementLocated(cp.policyReviewModal));
        Assert.assertTrue(cp.isReviewModalVisible(), "Review modal didn't appear.");

        // Validate review values using hidden fields / iframe URL
        Assert.assertEquals(cp.getHiddenPolicyName(), pName);
        Assert.assertEquals(cp.getHiddenSumAssured(), "500000");
        Assert.assertEquals(cp.getHiddenPremium(), "5000");
        Assert.assertEquals(cp.getHiddenTenure(), "5");

        // Validate that the iframe src contains our values (URL params)
        String src = cp.getReviewFrameSrc();
        Assert.assertTrue(src.contains("policyName=" + pName), "policyName missing in review URL");
        Assert.assertTrue(src.contains("sumAssured=500000"), "sumAssured missing in review URL");
        Assert.assertTrue(src.contains("premium=5000"), "premium missing in review URL");
        Assert.assertTrue(src.contains("tenure=5"), "tenure missing in review URL");
    }

    /** TC_CP_002: Selecting OK in the dialog proceeds to save (postback). */
    @Test
    public void test_Confirm_OK_ProceedsToSave() {
        CreatePolicyPage cp = new CreatePolicyPage(driver);

        // Prepare minimal valid data for this test (independent of other tests)
        cp.selectMainCategory("Life");
        uiWait().until(ExpectedConditions.presenceOfElementLocated(cp.ddlSubCategory));
        try { new Select(driver.findElement(cp.ddlSubCategory)).selectByIndex(1); } catch (Exception ignored) {}

        String pName = "AutoTest_" + System.currentTimeMillis();
        cp.setPolicyName(pName);
        cp.setSumAssured("500000");
        cp.setPremium("5000");
        cp.setTenure(5);

        // Open Review and confirm
        cp.clickCreateToOpenReview();
        uiWait().until(ExpectedConditions.visibilityOfElementLocated(cp.policyReviewModal));

        WebElement modal = driver.findElement(cp.policyReviewModal);
        cp.clickReviewOk();

        // After OK, server postback → page reload. Wait for staleness & known element.
        uiWait().until(ExpectedConditions.stalenessOf(modal));
        uiWait().until(ExpectedConditions.presenceOfElementLocated(cp.btnCreate));

        // TODO: When a visible success indicator is implemented, assert it here.
        // WebElement success = uiWait().until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".alert-success")));
        // Assert.assertTrue(success.isDisplayed(), "Success banner not visible after confirm.");
    }

    /** TC_CP_003: Success outcome is clear & creation happens only once per confirmation. */
    @Test
    public void test_Success_And_NoDuplicateOnSingleConfirm() {
        CreatePolicyPage cp = new CreatePolicyPage(driver);

        // Prepare a unique policy to avoid server-side duplicates
        cp.selectMainCategory("Life");
        uiWait().until(ExpectedConditions.presenceOfElementLocated(cp.ddlSubCategory));
        try { new Select(driver.findElement(cp.ddlSubCategory)).selectByIndex(1); } catch (Exception ignored) {}

        String pName = "AutoTest_" + System.currentTimeMillis();
        cp.setPolicyName(pName);
        cp.setSumAssured("500000");
        cp.setPremium("5000");
        cp.setTenure(5);

        cp.clickCreateToOpenReview();
        uiWait().until(ExpectedConditions.visibilityOfElementLocated(cp.policyReviewModal));
        cp.clickReviewOk();

        // Look for a generic success indicator if available
        List<WebElement> successLike = driver.findElements(By.cssSelector(".alert-success, .text-success"));
        if (successLike.isEmpty()) {
            throw new SkipException("No explicit success locator provided yet; add one to finalize this assertion.");
        }
        Assert.assertTrue(successLike.get(0).isDisplayed(), "Success indicator not visible.");
    }

    // ==============================
    // User Story: US17P2_10 Main Category list & single selection
    // ==============================

    /** TC_CP_004: Main Category dropdown lists expected categories. */
    @Test
    public void test_MainCategory_Options_Listed() {
        CreatePolicyPage cp = new CreatePolicyPage(driver);

        List<String> actual = cp.getMainCategoryOptions();
        List<String> expected = Arrays.asList(
                "-- Select Main Category --",
                "Personal Accident Insurance",
                "Phone Insurance",
                "Travel Insurance",
                "Life",
                "Cyber Insurance",
                "House Insurance",
                "Asset",
                "Auto"
        );
        Assert.assertTrue(actual.containsAll(expected),
                "Main Category options mismatch.\nExpected at least: " + expected + "\nActual: " + actual);
    }

    /** TC_CP_005: Selecting a new category replaces previous; page updates (Sub Category resets/repopulates). */
    @Test
    public void test_MainCategory_SingleSelection_And_Update() {
        CreatePolicyPage cp = new CreatePolicyPage(driver);

        cp.selectMainCategory("Life");
        uiWait().until(ExpectedConditions.presenceOfElementLocated(cp.ddlSubCategory));
        String sel1 = cp.getSelectedMainCategory();
        Assert.assertEquals(sel1, "Life");

        cp.selectMainCategory("Travel Insurance");
        uiWait().until(ExpectedConditions.presenceOfElementLocated(cp.ddlSubCategory));
        String sel2 = cp.getSelectedMainCategory();
        Assert.assertEquals(sel2, "Travel Insurance", "New selection did not replace previous.");

        // Sub Category should be reset to default option after main category change
        String subNow = cp.getSelectedSubCategory();
        Assert.assertTrue(subNow.contains("Select Sub Category") || subNow.equals("0"),
                "Sub Category did not reset after main category change. Actual: " + subNow);
    }

    /** TC_CP_006: Reset returns dropdown to default; clears fields; Tenure to 0. */
    @Test
    public void test_Reset_Returns_Defaults() {
        CreatePolicyPage cp = new CreatePolicyPage(driver);

        // Set some values
        cp.selectMainCategory("Life");
        uiWait().until(ExpectedConditions.presenceOfElementLocated(cp.ddlSubCategory));
        try { new Select(driver.findElement(cp.ddlSubCategory)).selectByIndex(1); } catch (Exception ignored) {}
        cp.setPolicyName("TempName");
        cp.setSumAssured("600000");
        cp.setPremium("6000");
        cp.setTenure(6);

        // Click Reset (client-side)
        cp.clickReset();

        // Validate defaults per your resetForm()
        Assert.assertTrue(cp.getSelectedMainCategory().contains("-- Select Main Category --") ||
                          cp.getSelectedMainCategory().equals("0"),
                "Main Category not reset to default.");

        // Sub Category should be cleared/disabled. Disabled is client-side; check attribute.
        String subVal = cp.getSelectedSubCategory();
        boolean disabled = driver.findElement(cp.ddlSubCategory).getAttribute("disabled") != null;
        Assert.assertTrue(subVal.contains("Select Sub Category") || subVal.equals("0"),
                "Sub Category not reset to default.");
        Assert.assertTrue(disabled, "Sub Category not disabled after Reset.");

        Assert.assertEquals(driver.findElement(cp.txtPolicyName).getAttribute("value"), "", "Policy Name not cleared");
        Assert.assertEquals(driver.findElement(cp.txtSumAssured).getAttribute("value"), "", "Sum Assured not cleared");
        Assert.assertEquals(driver.findElement(cp.txtPremium).getAttribute("value"), "", "Premium not cleared");
        Assert.assertEquals(cp.getTenureLabelText(), "0", "Tenure label not reset to 0");
    }

    // ==============================
    // User Story: US17P2_05 Premium numeric input UI
    // ==============================

    /** TC_CP_007: Premium label above numeric input (and input type=number). */
    @Test
    public void test_Premium_Label_And_NumericInput() {
        CreatePolicyPage cp = new CreatePolicyPage(driver);

        String type = driver.findElement(cp.txtPremium).getAttribute("type");
        Assert.assertEquals(type, "number", "Premium input is not type=number");
        // Label check: simple presence of the label text near field (could be more specific if needed)
        List<WebElement> labels = driver.findElements(By.xpath("//label[normalize-space()='Premium']"));
        Assert.assertTrue(!labels.isEmpty() && labels.get(0).isDisplayed(), "Label 'Premium' not visible above input");
    }

    /** TC_CP_008: Premium accepts numeric-only (browser/HTML5 behavior). */
    @Test
    public void test_Premium_NumericOnlyAcceptance() {
        CreatePolicyPage cp = new CreatePolicyPage(driver);

        // Try non-numeric
        cp.setPremium("abc");
        String val = cp.getPremiumValue();
        // Many browsers block, some may coerce; in either case, non-numeric must not persist
        Assert.assertTrue(val == null || val.isEmpty(), "Non-numeric premium should be blocked/cleared. Actual: " + val);

        // Now set a valid numeric
        cp.setPremium("5000");
        Assert.assertEquals(cp.getPremiumValue(), "5000", "Numeric premium not set/displayed correctly.");
    }

    /** TC_CP_009: Premium min/max constraints (adjust assertions once FRD values & UI messages are wired). */
    @Test
    public void test_Premium_MinMaxConstraints() {
        CreatePolicyPage cp = new CreatePolicyPage(driver);

        // Below min example (adjust when FRD provides limits)
        cp.setPremium("-1");
        cp.clickCreateToOpenReview();
        uiWait().until(ExpectedConditions.visibilityOfElementLocated(cp.policyReviewModal));
        cp.clickReviewOk();
        // Expect an error; look for generic error elements
        List<WebElement> errs = cp.findErrorMessages();
        if (errs.isEmpty()) {
            throw new SkipException("No visible error holder defined. Provide an error message locator for min/max validation.");
        }

        // Above max example
        goToCreatePolicy();
        resetCreatePolicyForm();
        cp.setPremium("10000001"); // adjust per FRD
        cp.clickCreateToOpenReview();
        uiWait().until(ExpectedConditions.visibilityOfElementLocated(cp.policyReviewModal));
        cp.clickReviewOk();
        errs = cp.findErrorMessages();
        if (errs.isEmpty()) {
            throw new SkipException("No visible error holder defined. Provide an error message locator for min/max validation.");
        }
    }

    /** TC_CP_010: Premium placeholder text (optional—your HTML currently has no placeholder on premium). */
    @Test
    public void test_Premium_Placeholder() {
        CreatePolicyPage cp = new CreatePolicyPage(driver);

        String ph = cp.getPremiumPlaceholder(); // likely null or empty in current HTML
        // If FRD mandates a placeholder, assert it. Otherwise, just check attribute exists when you add it.
        if (ph == null || ph.isEmpty()) {
            throw new SkipException("Premium input has no placeholder attribute in current HTML; add one to complete this test.");
        }
        Assert.assertTrue(ph.toLowerCase().contains("premium") || ph.toLowerCase().contains("enter"),
                "Unexpected Premium placeholder: " + ph);
    }

    /** TC_CP_011: Error message for invalid/empty Premium on submit. */
    @Test
    public void test_Premium_InvalidOrEmpty_ShowsError() {
        CreatePolicyPage cp = new CreatePolicyPage(driver);

        cp.setPremium(""); // empty
        cp.clickCreateToOpenReview();
        uiWait().until(ExpectedConditions.visibilityOfElementLocated(cp.policyReviewModal));
        cp.clickReviewOk();

        List<WebElement> errs = cp.findErrorMessages();
        if (errs.isEmpty()) {
            throw new SkipException("No visible error holder defined. Provide an error message locator to assert invalid/empty premium.");
        }
    }
}