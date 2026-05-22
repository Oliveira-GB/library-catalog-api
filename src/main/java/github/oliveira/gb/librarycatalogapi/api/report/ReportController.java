package github.oliveira.gb.librarycatalogapi.api.report;

import github.oliveira.gb.librarycatalogapi.domain.report.CsvGenerationService;
import github.oliveira.gb.librarycatalogapi.domain.report.PdfGenerationService;
import github.oliveira.gb.librarycatalogapi.domain.report.ReportService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * REST controller for management report endpoints.
 * Content type (JSON, CSV, or PDF) is determined exclusively by the Accept header.
 * Requires authentication for all report endpoints.
 */
@RestController
@RequestMapping("/api/v1/relatorios")
public class ReportController {

    private final ReportService reportService;
    private final CsvGenerationService csvGenerationService;
    private final PdfGenerationService pdfGenerationService;

    public ReportController(ReportService reportService,
                            CsvGenerationService csvGenerationService,
                            PdfGenerationService pdfGenerationService) {
        this.reportService = reportService;
        this.csvGenerationService = csvGenerationService;
        this.pdfGenerationService = pdfGenerationService;
    }

    /**
     * Generates the inventory report in the requested format.
     * Supported Accept headers: application/json, text/csv, application/pdf.
     *
     * @param accept the Accept header value
     * @return ResponseEntity with the report payload
     * @throws HttpMediaTypeNotAcceptableException if the requested format is not supported
     */
    @GetMapping(value = "/inventario")
    public ResponseEntity<?> inventoryReport(
            @RequestHeader(value = "Accept", defaultValue = MediaType.APPLICATION_JSON_VALUE) String accept)
            throws HttpMediaTypeNotAcceptableException {
        List<InventoryReportResponse> data = reportService.generateInventoryReport();
        return generateReportResponse(accept, data,
                csvGenerationService::generateInventoryCsv,
                pdfGenerationService::generateInventoryPdf);
    }

    /**
     * Generates the financial report in the requested format.
     * Supported Accept headers: application/json, text/csv, application/pdf.
     *
     * @param accept the Accept header value
     * @return ResponseEntity with the report payload
     * @throws HttpMediaTypeNotAcceptableException if the requested format is not supported
     */
    @GetMapping(value = "/financeiro")
    public ResponseEntity<?> financialReport(
            @RequestHeader(value = "Accept", defaultValue = MediaType.APPLICATION_JSON_VALUE) String accept)
            throws HttpMediaTypeNotAcceptableException {
        List<FinancialReportResponse> data = reportService.generateFinancialReport();
        return generateReportResponse(accept, data,
                csvGenerationService::generateFinancialCsv,
                pdfGenerationService::generateFinancialPdf);
    }

    private <T> ResponseEntity<?> generateReportResponse(
            String accept,
            List<T> data,
            BiConsumer<List<T>, OutputStream> csvGenerator,
            BiConsumer<List<T>, OutputStream> pdfGenerator) throws HttpMediaTypeNotAcceptableException {
        if (accept.contains(MediaType.APPLICATION_JSON_VALUE)) {
            return ResponseEntity.ok(data);
        } else if (accept.contains("text/csv")) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            csvGenerator.accept(data, out);
            return ResponseEntity.ok()
                    .contentType(MediaType.valueOf("text/csv"))
                    .body(out.toByteArray());
        } else if (accept.contains(MediaType.APPLICATION_PDF_VALUE)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            pdfGenerator.accept(data, out);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(out.toByteArray());
        }
        throw new HttpMediaTypeNotAcceptableException("Supported types: application/json, text/csv, application/pdf");
    }
}
