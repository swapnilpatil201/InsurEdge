package pages;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.Select;

import java.util.ArrayList;
import java.util.List;
import org.openqa.selenium.Keys;

/**
 * POM for the Create Policy page (AdminCreatePolicy.aspx).
 * Simple selectors, no JS executor. Includes helpers used in tests.
 */
public class CreatePolicyPage {

    public WebDriver driver;

    public CreatePolicyPage(WebDriver driver) {
        this.driver = driver;
    }

    // ====== Core controls ======
    public By ddlMainCategory = By.id("ContentPlaceHolder_Admin_ddlMainCategory");
    public By ddlSubCategory  = By.id("ContentPlaceHolder_Admin_ddlSubCategory");

    public By txtPolicyName   = By.id("ContentPlaceHolder_Admin_txtPolicyName");
    public By txtPremium      = By.id("ContentPlaceHolder_Admin_txtPremium");
    public By txtSumAssured   = By.id("ContentPlaceHolder_Admin_txtSumAssured");

    public By sliderTenure    = By.id("ContentPlaceHolder_Admin_sliderTenure");
    public By tenureLabel     = By.id("tenureValue");

    public By btnCreate       = By.id("ContentPlaceHolder_Admin_btnCreate");
    public By btnReset        = By.id("ContentPlaceHolder_Admin_btnReset");

    // ====== Review modal (uses style=display:block/none) ======
    public By policyReviewModal = By.id("policyReviewModal");
    public By modalOkButton     = By.xpath("//div[@id='policyReviewModal']//button[contains(normalize-space(),'OK')]");
    public By policyReviewFrame = By.id("policyReviewFrame");

    // ====== Hidden fields set by page JS before opening review ======
    public By hiddenMainCategory = By.id("ContentPlaceHolder_Admin_hiddenMainCategory");
    public By hiddenSubCategory  = By.id("ContentPlaceHolder_Admin_hiddenSubCategory");
    public By hiddenPolicyName   = By.id("ContentPlaceHolder_Admin_hiddenPolicyName");
    public By hiddenSumAssured   = By.id("ContentPlaceHolder_Admin_hiddenSumAssured");
    public By hiddenPremium      = By.id("ContentPlaceHolder_Admin_hiddenPremium");
    public By hiddenTenure       = By.id("ContentPlaceHolder_Admin_hiddenTenure");

    // ====== Actions ======

    // Dropdowns
    public void selectMainCategory(String text) {
        new Select(driver.findElement(ddlMainCategory)).selectByVisibleText(text);
    }

    public void selectSubCategory(String text) {
        new Select(driver.findElement(ddlSubCategory)).selectByVisibleText(text);
    }

    public List<String> getMainCategoryOptions() {
        List<WebElement> opts = new Select(driver.findElement(ddlMainCategory)).getOptions();
        List<String> names = new ArrayList<>();
        for (WebElement o : opts) names.add(o.getText().trim());
        return names;
    }

    public String getSelectedMainCategory() {
        return new Select(driver.findElement(ddlMainCategory))
                .getFirstSelectedOption().getText().trim();
    }

    public String getSelectedSubCategory() {
        return new Select(driver.findElement(ddlSubCategory))
                .getFirstSelectedOption().getText().trim();
    }

    // Text fields
    public void setPolicyName(String value) {
        WebElement e = driver.findElement(txtPolicyName);
        e.clear();
        e.sendKeys(value);
    }

    public void setPremium(String value) {
        WebElement e = driver.findElement(txtPremium);
        e.clear();
        e.sendKeys(value);
    }

    public void setSumAssured(String value) {
        WebElement e = driver.findElement(txtSumAssured);
        e.clear();
        e.sendKeys(value);
    }

    /** Legacy: not used by tests, kept for compatibility. */
    public void setTenure(int years) {
        driver.findElement(sliderTenure).sendKeys(String.valueOf(years));
    }

    /**
     * NEW: Deterministic slider set.
     * Always moves the slider to 0 first, then to the target 'years'.
     * Fires oninput so the label updates.
     */
    public void setTenureExact(int years) {
        WebElement slider = driver.findElement(sliderTenure);
        slider.click(); // focus

        // Push to min
        for (int i = 0; i < 40; i++) {
            slider.sendKeys(Keys.ARROW_LEFT);
        }
        // Move to target
        for (int i = 0; i < years; i++) {
            slider.sendKeys(Keys.ARROW_RIGHT);
        }
    }

    public String getTenureLabelText() {
        return driver.findElement(tenureLabel).getText().trim();
    }

    // Buttons / Modal
    public void clickCreateToOpenReview() {
        driver.findElement(btnCreate).click();
    }

    public void clickReset() {
        driver.findElement(btnReset).click();
    }

    public void clickReviewOk() {
        driver.findElement(modalOkButton).click();
    }

    // ====== Modal helpers ======

    public boolean isReviewModalVisible() {
        try {
            return driver.findElement(policyReviewModal).isDisplayed();
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    public String getReviewFrameSrc() {
        return driver.findElement(policyReviewFrame).getAttribute("src");
    }

    // ====== Hidden field getters ======
    public String getHiddenMainCategory() { return getHiddenValue(hiddenMainCategory); }
    public String getHiddenSubCategory()  { return getHiddenValue(hiddenSubCategory); }
    public String getHiddenPolicyName()   { return getHiddenValue(hiddenPolicyName); }
    public String getHiddenSumAssured()   { return getHiddenValue(hiddenSumAssured); }
    public String getHiddenPremium()      { return getHiddenValue(hiddenPremium); }
    public String getHiddenTenure()       { return getHiddenValue(hiddenTenure); }

    private String getHiddenValue(By by) {
        try {
            return driver.findElement(by).getAttribute("value").trim();
        } catch (NoSuchElementException e) {
            return "";
        }
    }

    // ====== Misc for tests ======
    public String getPremiumPlaceholder() {
        return driver.findElement(txtPremium).getAttribute("placeholder");
    }

    public String getPremiumValue() {
        return driver.findElement(txtPremium).getAttribute("value");
    }

    public List<WebElement> findErrorMessages() {
        List<WebElement> errs = new ArrayList<>();
        errs.addAll(driver.findElements(By.cssSelector(".invalid-feedback, .text-danger, .alert-danger")));
        return errs;
    }
}