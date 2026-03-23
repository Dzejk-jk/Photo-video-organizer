import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.mp4.Mp4MetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.tools.FileUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MediaOrganizer {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM");
    private static final SimpleDateFormat VIDEO_DATE_FORMAT =
            new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH);

    /**
     * Organizes all image files in rootDirectory into yyyy-MM subdirectories.
     * Returns a list of image files that had no date metadata.
     */
    public List<File> organizeImages(Path rootDirectory) {
        List<File> undated = new ArrayList<>();
        File folder = rootDirectory.toFile();
        File[] files = folder.listFiles();
        if (files == null) return undated;

        for (File file : files) {
            if (!file.isFile() || FileUtils.isVideoFile(file)) continue;

            String imageDate = getImageCreatedDate(file);
            if (imageDate != null) {
                moveFileToDateFolder(file, rootDirectory, imageDate);
            } else {
                undated.add(file);
            }
        }
        return undated;
    }

    /**
     * Organizes all video files in rootDirectory into yyyy-MM subdirectories.
     */
    public void organizeVideos(Path rootDirectory) {
        File folder = rootDirectory.toFile();
        File[] files = folder.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (!file.isFile() || !FileUtils.isVideoFile(file)) continue;

            String rawDate = getVideoCreatedDate(file);
            if (rawDate == null) continue;

            try {
                Date date = VIDEO_DATE_FORMAT.parse(rawDate);
                String yearMonth = DATE_FORMAT.format(date);
                moveFileToDateFolder(file, rootDirectory, yearMonth);
            } catch (ParseException e) {
                System.err.println("Nelze parsovat datum videa u souboru " + file.getName() + ": " + e.getMessage());
            }
        }
    }

    /**
     * Moves a single file into rootDirectory/yearMonth/, creating the folder if needed.
     */
    public void moveFileToDateFolder(File file, Path rootDirectory, String yearMonth) {
        Path targetDir = rootDirectory.resolve(yearMonth);
        try {
            if (!Files.exists(targetDir)) {
                Files.createDirectory(targetDir);
            }
            Path targetPath = targetDir.resolve(file.getName());
            Files.move(file.toPath(), targetPath);
            System.out.println(file.getName() + " přesunuto do " + targetDir);
        } catch (IOException e) {
            System.err.println("Chyba při přesunu " + file.getName() + ": " + e.getMessage());
        }
    }

    /**
     * Reads EXIF date from an image file. Returns null if not available.
     */
    public String getImageCreatedDate(File file) {
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(file);
            ExifSubIFDDirectory directory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
            if (directory != null) {
                Date date = directory.getDateOriginal();
                if (date != null) {
                    return DATE_FORMAT.format(date);
                }
            }
        } catch (Exception e ) {
            System.err.println("Chyba při čtení metadat z " + file.getName() + ": " + e.getMessage());
        }
        return null;
    }

    /**
     * Reads creation time from a video file. Returns null if not available.
     */
    public String getVideoCreatedDate(File file) {
        try {
            Metadata metadata = Mp4MetadataReader.readMetadata(file);
            for (Directory directory : metadata.getDirectories()) {
                for (Tag tag : directory.getTags()) {
                    if (tag.getTagName().equals("Creation Time")) {
                        return tag.getDescription();
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Chyba při čtení metadat z " + file.getName() + ": " + e.getMessage());
        }
        return null;
    }
}
