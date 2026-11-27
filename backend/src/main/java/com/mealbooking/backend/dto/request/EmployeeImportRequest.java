package com.mealbooking.backend.dto.request;

import com.mealbooking.backend.application.model.AdminCommands.ImportRow;
import com.mealbooking.backend.dto.request.AdminRequests.EmployeeInput;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record EmployeeImportRequest(@NotNull @Size(min = 1, max = 500) List<@NotNull @Valid Row> rows) {
    public List<ImportRow> toCommands() {
        return rows.stream().map(row -> new ImportRow(row.row(), row.employee().toCommand())).toList();
    }

    public record Row(@Min(2) int row, @Valid @NotNull EmployeeInput employee) {}
}
