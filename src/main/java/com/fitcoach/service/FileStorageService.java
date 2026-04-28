package com.fitcoach.service;

import com.fitcoach.exception.ResourceNotFoundException;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Persists uploaded files to the local filesystem under {@code app.upload.dir}.
 * Files are served back to clients via the static-resource mapping configured in
 * {@link com.fitcoach.config.WebMvcConfig} at the {@code /uploads/**} path prefix.
 */
@Service
public class FileStorageService {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    private Path rootPath;

    @PostConstruct
    public void init() throws IOException {
        rootPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(rootPath);
    }

    /**
     * Saves {@code file} under {@code subDir} (e.g. {@code "inbody/42"}) and returns the
     * relative URL path (e.g. {@code "/uploads/inbody/42/abc123.pdf"}) suitable for storage
     * in the database and for direct download by clients.
     */
    public String store(MultipartFile file, String subDir) {
        String originalName = StringUtils.cleanPath(
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "file");
        String extension = "";
        int dotIndex = originalName.lastIndexOf('.');
        if (dotIndex >= 0) {
            extension = originalName.substring(dotIndex);
        }
        String storedName = UUID.randomUUID() + extension;

        try {
            Path targetDir = rootPath.resolve(subDir);
            Files.createDirectories(targetDir);
            Path targetPath = targetDir.resolve(storedName);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new RuntimeException("Failed to store file: " + ex.getMessage(), ex);
        }

        return "/uploads/" + subDir + "/" + storedName;
    }

    /**
     * Resolves a URL path returned by {@link #store} (e.g. {@code /uploads/inbody/x/y.pdf})
     * to an absolute filesystem path under {@link #rootPath}.
     */
    public Path resolveStoredPath(String uploadsUrlPath) {
        if (uploadsUrlPath == null || !uploadsUrlPath.startsWith("/uploads/")) {
            throw new IllegalArgumentException("Not an uploads URL path: " + uploadsUrlPath);
        }
        String relativePath = uploadsUrlPath.substring("/uploads/".length());
        Path filePath = rootPath.resolve(relativePath).normalize();
        if (!filePath.startsWith(rootPath)) {
            throw new IllegalArgumentException("Path escapes upload root: " + uploadsUrlPath);
        }
        return filePath;
    }

    /**
     * Deletes the file at the given relative URL path (as previously returned by {@link #store}).
     * Silently succeeds if the file no longer exists on disk.
     */
    public void delete(String fileUrl) {
        if (fileUrl == null || !fileUrl.startsWith("/uploads/")) {
            return;
        }
        String relativePath = fileUrl.substring("/uploads/".length());
        Path filePath = rootPath.resolve(relativePath).normalize();
        try {
            Files.deleteIfExists(filePath);
        } catch (IOException ex) {
            // Log but do not fail the request — DB record deletion proceeds regardless.
        }
    }
}
