package github.oliveira.gb.librarycatalogapi.domain.report;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.RecordComponent;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Stateless service responsible for generating CSV documents from DTO projections.
 * Isolated responsibility: receives a list of records and an output stream, writes CSV.
 */
@Service
public class CsvGenerationService {

    private static final String[] INVENTORY_HEADERS = {"bookId", "title", "isbn", "status"};
    private static final String[] FINANCIAL_HEADERS = {"fineId", "readerId", "readerName", "amount", "status", "createdAt"};

    /**
     * Generates a CSV stream for the inventory report.
     *
     * @param data the inventory report data
     * @param out the output stream to write to
     */
    public void generateInventoryCsv(List<?> data, OutputStream out) {
        writeCsv(data, out, INVENTORY_HEADERS);
    }

    /**
     * Generates a CSV stream for the financial report.
     *
     * @param data the financial report data
     * @param out the output stream to write to
     */
    public void generateFinancialCsv(List<?> data, OutputStream out) {
        writeCsv(data, out, FINANCIAL_HEADERS);
    }

    private void writeCsv(List<?> data, OutputStream out, String[] headers) {
        try (Writer writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
             CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.builder()
                     .setHeader(headers)
                     .build())) {
            for (Object record : data) {
                writeRecord(record, printer);
            }
            printer.flush();
        } catch (IOException e) {
            throw new DocumentGenerationException("Failed to generate CSV document", e);
        }
    }

    private void writeRecord(Object record, CSVPrinter printer) throws IOException {
        if (record instanceof java.lang.Record r) {
            for (RecordComponent component : r.getClass().getRecordComponents()) {
                try {
                    Object value = component.getAccessor().invoke(r);
                    printer.print(value != null ? value.toString() : "");
                } catch (ReflectiveOperationException e) {
                    throw new DocumentGenerationException("Failed to access record component", e);
                }
            }
            printer.println();
        } else {
            throw new DocumentGenerationException("CSV generation supports Java Records only");
        }
    }
}
