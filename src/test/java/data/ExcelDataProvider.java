package data;

import org.testng.annotations.DataProvider;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ExcelDataProvider {

    @DataProvider(name = "mainCategoryDP")
    public static Object[][] mainCategoryDP() {
        // Excel under src/test/resources/testdata
        String resourcePath = "/testdata/PolicyModuleData.xlsx";
        String sheetName = "Authorize_MainCategory_Filter";
        return readSheetFromClasspath(resourcePath, sheetName);
    }

    private static Object[][] readSheetFromClasspath(String resourcePath, String sheetName) {
        List<Object[]> rows = new ArrayList<>();

        try (InputStream is = ExcelDataProvider.class.getResourceAsStream(resourcePath);
             Workbook wb = new XSSFWorkbook(is)) {

            Sheet sheet = wb.getSheet(sheetName);
            if (sheet == null) {
                throw new RuntimeException("Sheet not found: " + sheetName);
            }

            Iterator<Row> it = sheet.iterator();
            if (!it.hasNext()) return new Object[0][0]; // no data
            Row header = it.next(); // skip header
            int cols = header.getLastCellNum();
            DataFormatter fmt = new DataFormatter();

            while (it.hasNext()) {
                Row r = it.next();

                // read first column only: MainCategory
                String mainCategory = fmt.formatCellValue(r.getCell(0)).trim();
                if (mainCategory.isEmpty()) continue;

                rows.add(new Object[]{ mainCategory });
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed reading Excel from classpath: " + e.getMessage(), e);
        }

        return rows.toArray(new Object[0][0]);
    }
}