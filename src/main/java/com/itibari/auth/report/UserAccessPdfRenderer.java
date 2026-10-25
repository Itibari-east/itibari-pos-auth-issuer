package com.itibari.auth.report;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class UserAccessPdfRenderer {

    private static final DateTimeFormatter GENERATED_AT =
            DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");
    private static final Color HEADER_BG = new Color(15, 76, 129);
    private static final Color ROLE_BG = new Color(232, 240, 248);
    private static final Color GRID = new Color(210, 218, 226);

    public byte[] render(UserAccessReport report) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 40, 40, 48, 40);
            PdfWriter.getInstance(document, out);
            document.open();
            document.add(title("User Access Report"));
            document.add(infoTable(report));
            if (report.roles() == null || report.roles().isEmpty()) {
                document.add(spaced("No roles assigned.", bodyFont()));
            } else {
                for (UserAccessReport.RoleAccess role : report.roles()) {
                    document.add(roleSection(role));
                }
            }
            document.close();
            return out.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to generate access report PDF", ex);
        }
    }

    private Paragraph title(String text) {
        Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, HEADER_BG);
        Paragraph paragraph = new Paragraph(text, font);
        paragraph.setSpacingAfter(12);
        return paragraph;
    }

    private PdfPTable infoTable(UserAccessReport report) {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1.4f, 3.6f});
        table.setSpacingAfter(16);
        addInfoRow(table, "Name", blankToDash(report.fullName()));
        addInfoRow(table, "Email", blankToDash(report.email()));
        addInfoRow(table, "Username", blankToDash(report.username()));
        addInfoRow(table, "Shop", blankToDash(report.shopName()));
        addInfoRow(table, "Generated", report.generatedAt() == null
                ? "-"
                : GENERATED_AT.format(report.generatedAt()) + " EAT");
        return table;
    }

    private void addInfoRow(PdfPTable table, String label, String value) {
        table.addCell(labelCell(label));
        table.addCell(valueCell(value));
    }

    private PdfPTable roleSection(UserAccessReport.RoleAccess role) throws Exception {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{2.2f, 3.8f});
        table.setSpacingBefore(8);
        table.setSpacingAfter(12);
        PdfPCell roleHeader = cell("Role: " + blankToDash(role.name()), headerFont(), HEADER_BG);
        roleHeader.setColspan(2);
        table.addCell(roleHeader);

        table.addCell(cell("Privilege group", sectionFont(), ROLE_BG));
        table.addCell(cell("Privileges", sectionFont(), ROLE_BG));

        List<UserAccessReport.GroupAccess> groups = role.groups() == null ? List.of() : role.groups();
        if (groups.isEmpty()) {
            PdfPCell empty = cell("No privilege groups attached", bodyFont(), Color.WHITE);
            empty.setColspan(2);
            table.addCell(empty);
        } else {
            for (UserAccessReport.GroupAccess group : groups) {
                table.addCell(cell(blankToDash(group.name()), bodyFont(), Color.WHITE));
                table.addCell(cell(join(group.privileges()), bodyFont(), Color.WHITE));
            }
        }

        if (role.extraPrivileges() != null && !role.extraPrivileges().isEmpty()) {
            table.addCell(cell("Additional privileges", bodyFont(), Color.WHITE));
            table.addCell(cell(join(role.extraPrivileges()), bodyFont(), Color.WHITE));
        }
        return table;
    }

    private PdfPCell labelCell(String text) {
        return cell(text, sectionFont(), ROLE_BG);
    }

    private PdfPCell valueCell(String text) {
        return cell(text, bodyFont(), Color.WHITE);
    }

    private PdfPCell cell(String text, Font font, Color background) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(background);
        cell.setBorderColor(GRID);
        cell.setPadding(7);
        cell.setVerticalAlignment(Element.ALIGN_TOP);
        return cell;
    }

    private Paragraph spaced(String text, Font font) {
        Paragraph paragraph = new Paragraph(text, font);
        paragraph.setSpacingBefore(8);
        return paragraph;
    }

    private Font headerFont() {
        return FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
    }

    private Font sectionFont() {
        return FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, HEADER_BG);
    }

    private Font bodyFont() {
        return FontFactory.getFont(FontFactory.HELVETICA, 9, Color.DARK_GRAY);
    }

    private String join(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "-";
        }
        return String.join("\n", values);
    }

    private String blankToDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
