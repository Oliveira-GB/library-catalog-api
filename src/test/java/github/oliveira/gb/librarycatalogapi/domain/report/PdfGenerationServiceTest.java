package github.oliveira.gb.librarycatalogapi.domain.report;

import github.oliveira.gb.librarycatalogapi.api.report.FinancialReportResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PDF Generation Service Unit Tests")
class PdfGenerationServiceTest {

    private final PdfGenerationService service = new PdfGenerationService();

    @Test
    @DisplayName("Should generate non-empty PDF for data rows")
    void shouldGenerateNonEmptyPdfForDataRows() {
        List<FinancialReportResponse> data = List.of(
                new FinancialReportResponse(1L, 10L, "Alice", new BigDecimal("15.50"), "PENDENTE", LocalDate.now()),
                new FinancialReportResponse(2L, 11L, "Bob", new BigDecimal("22.00"), "PAGA", LocalDate.now())
        );

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        service.generateFinancialPdf(data, out);

        assertThat(out.size()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should generate PDF with title and headers for empty data")
    void shouldGeneratePdfWithTitleAndHeadersForEmptyData() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        service.generateFinancialPdf(Collections.emptyList(), out);

        assertThat(out.size()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should handle null values in records")
    void shouldHandleNullValuesInRecords() {
        List<FinancialReportResponse> data = List.of(
                new FinancialReportResponse(1L, 10L, null, new BigDecimal("10.00"), "PENDENTE", LocalDate.now())
        );

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        service.generateFinancialPdf(data, out);

        assertThat(out.size()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should throw exception for non-record types")
    void shouldThrowExceptionForNonRecordTypes() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        List<String> invalidData = List.of("invalid");
        assertThatThrownBy(() -> service.generateFinancialPdf(invalidData, out))
                .isInstanceOf(DocumentGenerationException.class)
                .hasMessageContaining("Java Records only");
    }
}
