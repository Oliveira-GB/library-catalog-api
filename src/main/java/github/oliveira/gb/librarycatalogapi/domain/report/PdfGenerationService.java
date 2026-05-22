package github.oliveira.gb.librarycatalogapi.domain.report;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.lang.reflect.RecordComponent;
import java.util.List;

/**
 * Stateless service responsible for generating PDF documents from DTO projections.
 * Isolated responsibility: receives a list of records and an output stream, writes PDF.
 * Generates basic tabular data dump aligned with scope restrictions.
 */
@Service
public class PdfGenerationService {

    private static final String INVENTORY_TITLE = "Inventory Report";
    private static final String FINANCIAL_TITLE = "Financial Report";

    /**
     * Generates a PDF stream for the inventory report.
     *
     * @param data the inventory report data
     * @param out the output stream to write to
     */
    public void generateInventoryPdf(List<?> data, OutputStream out) {
        generatePdf(data, out, INVENTORY_TITLE, new String[]{"Book ID", "Title", "ISBN", "Status"});
    }

    /**
     * Generates a PDF stream for the financial report.
     *
     * @param data the financial report data
     * @param out the output stream to write to
     */
    public void generateFinancialPdf(List<?> data, OutputStream out) {
        generatePdf(data, out, FINANCIAL_TITLE, new String[]{"Fine ID", "Reader ID", "Reader Name", "Amount", "Status", "Created At"});
    }

    private void generatePdf(List<?> data, OutputStream out, String title, String[] headers) {
        Document document = new Document();
        try (document) {
            PdfWriter.getInstance(document, out);
            document.open();
            document.add(new Phrase(title));
            document.add(new Phrase("\n\n"));

            PdfPTable table = new PdfPTable(headers.length);
            table.setWidthPercentage(100);

            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header));
                table.addCell(cell);
            }

            for (Object recordItem : data) {
                writeRecordToTable(recordItem, table);
            }

            document.add(table);
        } catch (DocumentException e) {
            throw new DocumentGenerationException("Failed to generate PDF document", e);
        }
    }

    private void writeRecordToTable(Object recordItem, PdfPTable table) {
        if (recordItem instanceof java.lang.Record r) {
            for (RecordComponent component : r.getClass().getRecordComponents()) {
                try {
                    Object value = component.getAccessor().invoke(r);
                    table.addCell(new Phrase(value != null ? value.toString() : ""));
                } catch (ReflectiveOperationException e) {
                    throw new DocumentGenerationException("Failed to access record component", e);
                }
            }
        } else {
            throw new DocumentGenerationException("PDF generation supports Java Records only");
        }
    }
}
