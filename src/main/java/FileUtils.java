import java.awt.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * Utility methods for file system operations and image processing.
 */
public class FileUtils {

    private static final String[] VIDEO_EXTENSIONS = {"mp4", "avi", "mkv", "mov", "wmv", "flv", "webm"};

    private FileUtils() {}

    /**
     * Returns a unique path by appending (1), (2), etc. if the target already exists.
     * Any existing trailing (N) suffix is stripped first so repeated calls never
     * produce names like IMG_8013(1)(1)(1).
     */
    public static Path getUniqueFileName(Path target) {
        if (!Files.exists(target)) {
            return target;
        }

        String fileName = target.getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');
        String name = (dotIndex == -1) ? fileName : fileName.substring(0, dotIndex);
        String extension = (dotIndex == -1) ? "" : fileName.substring(dotIndex);

        // Strip any existing trailing (N) suffix, e.g. "IMG_8013(1)" -> "IMG_8013"
        name = name.replaceAll("\\(\\d+\\)$", "");

        int count = 1;
        Path newTarget;
        do {
            newTarget = target.getParent().resolve(name + "(" + count + ")" + extension);
            count++;
        } while (Files.exists(newTarget));

        return newTarget;
    }

    /**
     * Checks whether the given file is a video based on its extension.
     */
    public static boolean isVideoFile(java.io.File file) {
        String fileName = file.getName().toLowerCase();
        for (String ext : VIDEO_EXTENSIONS) {
            if (fileName.endsWith("." + ext)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Moves all files from subdirectories into rootDirectory, then removes empty dirs.
     */
    public static void flattenDirectory(Path rootDirectory) throws IOException {
        // Move all nested files to root (skip files already directly in rootDirectory)
        try (Stream<Path> filePaths = Files.walk(rootDirectory)) {
            filePaths
                    .filter(Files::isRegularFile)
                    .filter(file -> !file.getParent().equals(rootDirectory))
                    .forEach(file -> {
                        Path destination = getUniqueFileName(rootDirectory.resolve(file.getFileName()));
                        try {
                            Files.move(file, destination);
                            System.out.println(file.getFileName() + " přesunuto do " + rootDirectory);
                        } catch (IOException ex) {
                            System.err.println("Chyba při přesunu " + file + ": " + ex.getMessage());
                        }
                    });
        }

        // Delete empty subdirectories (deepest first)
        try (Stream<Path> filePaths = Files.walk(rootDirectory)) {
            filePaths
                    .filter(path -> Files.isDirectory(path) && !path.equals(rootDirectory))
                    .sorted(Comparator.reverseOrder())
                    .forEach(dir -> {
                        try (Stream<Path> entries = Files.list(dir)) {
                            if (entries.findAny().isEmpty()) {
                                Files.delete(dir);
                                System.out.println("Smazána prázdná složka: " + dir.getFileName());
                            }
                        } catch (IOException ex) {
                            System.err.println("Chyba při mazání složky " + dir + ": " + ex.getMessage());
                        }
                    });
        }
    }
}