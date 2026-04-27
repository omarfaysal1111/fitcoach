package com.fitcoach.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class OcrService {

    @Value("${app.tesseract.path:tesseract}")
    private String tesseractPath;

    @Value("${app.tesseract.data-path:}")
    private String tessDataPath;

    // Matches numbers like 399, 399.00, 1,399.00 near EGP/LE/جنيه markers
    private static final Pattern AMOUNT_WITH_CURRENCY = Pattern.compile(
            "(?i)(?:EGP|LE|جنيه|ج\\.م)[\\s]*([0-9]{1,6}(?:[.,][0-9]{1,2})?)" +
            "|([0-9]{1,6}(?:[.,][0-9]{1,2})?)[\\s]*(?:EGP|LE|جنيه|ج\\.م)"
    );

    // Fallback: any standalone 3-6 digit number
    private static final Pattern BARE_AMOUNT = Pattern.compile(
            "\\b([1-9][0-9]{2,5}(?:\\.[0-9]{1,2})?)\\b"
    );

    /**
     * Runs Tesseract OCR on the uploaded screenshot and extracts the transferred amount in EGP.
     * Returns null if no recognisable amount is found.
     */
    public BigDecimal extractAmount(MultipartFile screenshot) {
        Path tempFile = null;
        try {
            tempFile = saveTempFile(screenshot);
            String ocrText = runTesseract(tempFile.toFile());
            log.debug("OCR output: {}", ocrText);
            return parseAmount(ocrText);
        } catch (Exception e) {
            log.error("OCR extraction failed: {}", e.getMessage(), e);
            return null;
        } finally {
            deleteSilently(tempFile);
        }
    }

    // ── Internals ─────────────────────────────────────────────────────────────

    private Path saveTempFile(MultipartFile file) throws IOException {
        String suffix = getExtension(file.getOriginalFilename());
        Path tmp = Files.createTempFile("payment_receipt_", suffix);
        file.transferTo(tmp.toFile());
        return tmp;
    }

    private String runTesseract(File input) throws IOException, InterruptedException {
        List<String> cmd = new ArrayList<>();
        cmd.add(tesseractPath);
        cmd.add(input.getAbsolutePath());
        cmd.add("stdout");
        cmd.add("--psm");
        cmd.add("6");
        if (!tessDataPath.isBlank()) {
            cmd.add("--tessdata-dir");
            cmd.add(tessDataPath);
        }
        // Prefer Arabic + English when available, fallback gracefully
        cmd.add("-l");
        cmd.add("ara+eng");

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exitCode = process.waitFor();

        if (exitCode != 0 && output.isBlank()) {
            // Retry with English only
            cmd.set(cmd.size() - 1, "eng");
            pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            process = pb.start();
            output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            process.waitFor();
        }
        return output;
    }

    private BigDecimal parseAmount(String text) {
        if (text == null || text.isBlank()) return null;

        // Priority 1: amount near explicit currency marker
        Matcher m = AMOUNT_WITH_CURRENCY.matcher(text);
        BigDecimal best = null;
        while (m.find()) {
            String raw = m.group(1) != null ? m.group(1) : m.group(2);
            BigDecimal candidate = parseDecimal(raw);
            if (candidate != null && (best == null || candidate.compareTo(best) > 0)) {
                best = candidate;
            }
        }
        if (best != null) return best;

        // Priority 2: largest bare number in the plausible plan-price range (300-1100)
        Matcher m2 = BARE_AMOUNT.matcher(text);
        while (m2.find()) {
            BigDecimal candidate = parseDecimal(m2.group(1));
            if (candidate != null
                    && candidate.compareTo(new BigDecimal("300")) >= 0
                    && candidate.compareTo(new BigDecimal("1100")) <= 0) {
                if (best == null || candidate.compareTo(best) > 0) {
                    best = candidate;
                }
            }
        }
        return best;
    }

    private BigDecimal parseDecimal(String raw) {
        if (raw == null) return null;
        try {
            // Normalise Arabic-extended digits and commas used as thousand separators
            String clean = raw.replace(",", "");
            return new BigDecimal(clean);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String getExtension(String filename) {
        if (filename == null) return ".png";
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot) : ".png";
    }

    private void deleteSilently(Path path) {
        if (path != null) {
            try { Files.deleteIfExists(path); } catch (IOException ignored) {}
        }
    }
}
