package pages;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.Select;

import java.util.ArrayList;
import java.util.List;

public class CreatePolicyPage {

    // 1) Constructor
    public WebDriver driver;

    public CreatePolicyPage(WebDriver driver) {
        this.driver = driver;
    }

    // 2) Locators (public, taken from your HTML)
    public By ddlMainCategory   = By.id("ContentPlaceHolder_Admin_ddlMainCategory");
    public By ddlSubCategory    = By.id("ContentPlaceHolder_Admin_ddlSubCategory");

    public By txtPolicyName     = By.id("ContentPlaceHolder_Admin_txtPolicyName");
    public By txtPremium        = By.id("ContentPlaceHolder_Admin_txtPremium");
    public By txtSumAssured     = By.id("ContentPlaceHolder_Admin_txtSumAssured");

    public By sliderTenure      = By.id("ContentPlaceHolder_Admin_sliderTenure");
    public By tenureValueLabel  = By.id("tenureValue");

    public By btnCreate         = By.id("ContentPlaceHolder_Admin_btnCreate");
    public By btnReset          = By.id("ContentPlaceHolder_Admin_btnReset");

    // Review modal and controls
    public By policyReviewModal = By.id("policyReviewModal");
    public By policyReviewFrame = By.id("policyReviewFrame");
    // Visible OK button inside review modal
    public By modalOkButton     = By.xpath("//div[@id='policyReviewModal']//button[contains(.,'OK')]");
    // Hidden Confirm (server postback)
    public By btnConfirmInsert  = By.id("ContentPlaceHolder_Admin_btnConfirmInsert");

    // Hidden fields (used by the JS to pass values; handy for assertions)
    public By hiddenMainCategory= By.id("ContentPlaceHolder_Admin_hiddenMainCategory");
    public By hiddenSubCategory = By.id("ContentPlaceHolder_Admin_hiddenSubCategory");
    public By hiddenPolicyName  = By.id("ContentPlaceHolder_Admin_hiddenPolicyName");
    public By hiddenSumAssured  = By.id("ContentPlaceHolder_Admin_hiddenSumAssured");
    public By hiddenPremium     = By.id("ContentPlaceHolder_Admin_hiddenPremium");
    public By hiddenTenure      = By.id("ContentPlaceHolder_Admin_hiddenTenure");

    // 3) Actions (public) — kept simple as you prefer

    // ----- Dropdowns -----
    public void selectMainCategory(String visibleText) {
        new Select(driver.findElement(ddlMainCategory)).selectByVisibleText(visibleText);
    }

    /** Select Sub Category by visible text (call after postback populates it). */
    public void selectSubCategory(String visibleText) {
        new Select(driver.findElement(ddlSubCategory)).selectByVisibleText(visibleText);
    }

    /** Get all visible options from Main Category (useful for option-list validation). */
    public List<String> getMainCategoryOptions() {
        List<WebElement> opts = new Select(driver.findElement(ddlMainCategory)).getOptions();
        List<String> names = new ArrayList<>();
        for (WebElement o : opts) names.add(o.getText().trim());
        return names;
    }

    public String getSelectedMainCategory() {
        return new Select(driver.findElement(ddlMainCategory)).getFirstSelectedOption().getText().trim();
    }

    public String getSelectedSubCategory() {
        return new Select(driver.findElement(ddlSubCategory)).getFirstSelectedOption().getText().trim();
    }

    // ----- Text/number fields -----
    public void setPolicyName(String value) {
        WebElement e = driver.findElement(txtPolicyName);
        e.clear(); e.sendKeys(value);
    }

    public void setPremium(String value) {
        WebElement e = driver.findElement(txtPremium);
        e.clear(); e.sendKeys(value);
    }

    public void setSumAssured(String value) {
        WebElement e = driver.findElement(txtSumAssured);
        e.clear(); e.sendKeys(value);
    }

    /** Sets tenure slider; also triggers oninput so label updates. */
    public void setTenure(int years) {
        WebElement slider = driver.findElement(sliderTenure);
        // Use JS to ensure the oninput handler fires (updateTenure)
        ((JavascriptExecutor) driver).executeScript(
            "arguments[0].value = arguments[1]; arguments[0].dispatchEvent(new Event('input'));",
            slider, years
        );
    }

    public String getTenureLabelText() {
        return driver.findElement(tenureValueLabel).getText().trim();
    }

    // ----- Buttons / Modal -----
    /** Clicks Create to open Review dialog (does not submit form). */
    public void clickCreateToOpenReview() {
        driver.findElement(btnCreate).click();
    }

    /** Clicks OK in the review dialog to trigger server postback (save). */
    public void clickReviewOk() {
        driver.findElement(modalOkButton).click();
    }

    /** Clicks Reset (client-side reset only per your JS). */
    public void clickReset() {
        driver.findElement(btnReset).click();
    }

    // ----- Review dialog & URL helpers -----
    /** Returns true if the review modal is displayed via inline style (display != none). */
    public boolean isReviewModalVisible() {
        try {
            WebElement modal = driver.findElement(policyReviewModal);
            return modal.isDisplayed(); // works because style 'display:block' makes it rendered
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    /** Returns the iframe src URL to verify values passed to review page. */
    public String getReviewFrameSrc() {
        return driver.findElement(policyReviewFrame).getAttribute("src");
    }

    // ----- Hidden field getters (populated by JS before opening review) -----
    public String getHiddenMainCategory() { return driver.findElement(hiddenMainCategory).getAttribute("value").trim(); }
    public String getHiddenSubCategory()  { return driver.findElement(hiddenSubCategory).getAttribute("value").trim(); }
    public String getHiddenPolicyName()   { return driver.findElement(hiddenPolicyName).getAttribute("value").trim(); }
    public String getHiddenSumAssured()   { return driver.findElement(hiddenSumAssured).getAttribute("value").trim(); }
    public String getHiddenPremium()      { return driver.findElement(hiddenPremium).getAttribute("value").trim(); }
    public String getHiddenTenure()       { return driver.findElement(hiddenTenure).getAttribute("value").trim(); }

    // ----- Generic validation helpers (for Premium validation fallbacks) -----
    /** Returns placeholder attribute from Premium (empty if not set in HTML). */
    public String getPremiumPlaceholder() {
        return driver.findElement(txtPremium).getAttribute("placeholder");
    }

    /** Returns the current Premium field value as string (browser may normalize it). */
    public String getPremiumValue() {
        return driver.findElement(txtPremium).getAttribute("value");
    }

    /** Quick probe for common error UI elements near the form (adjust when you add IDs for messages). */
    public List<WebElement> findErrorMessages() {
        List<WebElement> errs = new ArrayList<>();
        errs.addAll(driver.findElements(By.cssSelector(".invalid-feedback, .text-danger, .alert-danger")));
        return errs;
    }
}