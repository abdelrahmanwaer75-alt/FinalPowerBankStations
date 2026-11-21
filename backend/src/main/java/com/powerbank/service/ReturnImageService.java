package com.powerbank.service;

import com.powerbank.dto.ImageResponse;
import com.powerbank.entity.ImageOwnerType;
import com.powerbank.entity.InspectionImage;
import com.powerbank.entity.ReturnStatus;
import com.powerbank.exception.ApiException;
import com.powerbank.repository.InspectionImageRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.powerbank.service.ReservationService.conflict;

/** Stores return photos on disk and their metadata in the database. Only JPEG and PNG, up to 5 MB, max 6 per return. */
@Service
@Transactional
public class ReturnImageService {

    static final int MAX_FILES = 6;
    static final long MAX_BYTES = 5L * 1024 * 1024;
    static final Set<ReturnStatus> OPEN_FOR_UPLOAD = Set.of(ReturnStatus.PENDING_REVIEW, ReturnStatus.REQUIRES_MANUAL_REVIEW);

    private final ReturnService returns;
    private final InspectionImageRepository images;
    private final Path root;

    public ReturnImageService(ReturnService returns, InspectionImageRepository images,
                              @Value("${app.uploads.dir}") String uploadDir) {
        this.returns = returns;
        this.images = images;
        this.root = Path.of(uploadDir).toAbsolutePath().normalize();
    }

    public List<ImageResponse> upload(long userId, long returnId, List<MultipartFile> files) {
        var ret = returns.owned(userId, returnId);
        if (!OPEN_FOR_UPLOAD.contains(ret.getStatus())) {
            throw conflict("RETURN_NOT_OPEN", "Photos can no longer be added to this return");
        }
        if (files == null || files.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "NO_FILES", "Choose at least one photo");
        }
        long existing = images.countByOwnerTypeAndOwnerId(ImageOwnerType.RETURN, returnId);
        if (existing + files.size() > MAX_FILES) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "TOO_MANY_FILES",
                    "A return can have at most " + MAX_FILES + " photos");
        }

        // Validate every file before writing anything, so a bad file does not leave partial uploads.
        List<byte[]> contents = new ArrayList<>();
        for (MultipartFile f : files) {
            contents.add(validate(f));
        }

        List<ImageResponse> saved = new ArrayList<>();
        Path dir = root.resolve("returns").resolve(String.valueOf(returnId));
        try {
            Files.createDirectories(dir);
            for (int i = 0; i < files.size(); i++) {
                MultipartFile f = files.get(i);
                String ext = isPng(contents.get(i)) ? "png" : "jpg";
                String name = UUID.randomUUID() + "." + ext;
                Path target = dir.resolve(name).normalize();
                if (!target.startsWith(root)) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_PATH", "Invalid file path");
                }
                Files.write(target, contents.get(i));

                String relative = "returns/" + returnId + "/" + name;
                InspectionImage img = new InspectionImage(ImageOwnerType.RETURN, returnId, relative,
                        safeName(f.getOriginalFilename()), isPng(contents.get(i)) ? "image/png" : "image/jpeg",
                        contents.get(i).length, userId);
                images.save(img);
                saved.add(new ImageResponse(img.getId(), safeName(f.getOriginalFilename()),
                        isPng(contents.get(i)) ? "image/png" : "image/jpeg", contents.get(i).length));
            }
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "UPLOAD_FAILED", "The photos could not be saved");
        }
        return saved;
    }

    private byte[] validate(MultipartFile f) {
        if (f == null || f.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "EMPTY_FILE", "One of the files is empty");
        }
        if (f.getSize() > MAX_BYTES) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "FILE_TOO_LARGE", "Each photo must be 5 MB or smaller");
        }
        String type = f.getContentType();
        if (!"image/jpeg".equals(type) && !"image/png".equals(type)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "UNSUPPORTED_TYPE", "Only JPG and PNG photos are accepted");
        }
        byte[] bytes;
        try {
            bytes = f.getBytes();
        } catch (IOException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "UNREADABLE_FILE", "A photo could not be read");
        }
        // Check the real file signature, not only the type the browser sent.
        if (!isJpeg(bytes) && !isPng(bytes)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "UNSUPPORTED_TYPE", "The file is not a valid JPG or PNG");
        }
        return bytes;
    }

    private static boolean isJpeg(byte[] b) {
        return b.length > 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF;
    }

    private static boolean isPng(byte[] b) {
        return b.length > 4 && b[0] == (byte) 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G';
    }

    /** Keeps only the file name, without folders, and limits its length. The name is for display only. */
    private static String safeName(String raw) {
        String name = raw == null ? "photo" : raw.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1);
        return name.length() > 200 ? name.substring(0, 200) : name;
    }
}
