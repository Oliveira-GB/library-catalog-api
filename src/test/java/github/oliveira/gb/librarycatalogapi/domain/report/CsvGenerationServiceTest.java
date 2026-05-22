package github.oliveira.gb.librarycatalogapi.domain.report;

import github.oliveira.gb.librarycatalogapi.api.report.InventoryReportResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CSV Generation Service Unit Tests")
class CsvGenerationServiceTest {

    private final CsvGenerationService service = new CsvGenerationService();

    @Test
    @DisplayName("Should generate CSV with header and data rows")
    void shouldGenerateCsvWithHeaderAndDataRows() {
        List<InventoryReportResponse> data = List.of(
                new InventoryReportResponse(1L, "Clean Code", "978-0-13-235088-4", "DISPONIVEL"),
                new InventoryReportResponse(2L, "Design Patterns", "978-0-201-63361-0", "EMPRESTADO")
        );

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        service.generateInventoryCsv(data, out);

        String csv = out.toString(StandardCharsets.UTF_8);
        assertThat(csv)
                .contains("bookId,title,isbn,status")
                .contains("1,Clean Code,978-0-13-235088-4,DISPONIVEL")
                .contains("2,Design Patterns,978-0-201-63361-0,EMPRESTADO");
    }

    @Test
    @DisplayName("Should generate CSV with header only for empty data")
    void shouldGenerateCsvWithHeaderOnlyForEmptyData() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        service.generateInventoryCsv(Collections.emptyList(), out);

        String csv = out.toString(StandardCharsets.UTF_8);
        assertThat(csv).contains("bookId,title,isbn,status");
        assertThat(csv.lines().count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should handle null values gracefully")
    void shouldHandleNullValuesGracefully() {
        List<InventoryReportResponse> data = List.of(
                new InventoryReportResponse(1L, null, "978-0-00-000000-0", null)
        );

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        service.generateInventoryCsv(data, out);

        String csv = out.toString(StandardCharsets.UTF_8);
        assertThat(csv).contains("1,,978-0-00-000000-0,");
    }

    @Test
    @DisplayName("Should throw exception for non-record types")
    void shouldThrowExceptionForNonRecordTypes() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        List<String> invalidData = List.of("invalid");
        assertThatThrownBy(() -> service.generateInventoryCsv(invalidData, out))
                .isInstanceOf(DocumentGenerationException.class)
                .hasMessageContaining("Java Records only");
    }
}
