import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.mp4.Mp4MetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.drew.metadata.exif.ExifSubIFDDirectory;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;
import java.util.stream.Stream;

public class OrganizerGUI extends JFrame implements ActionListener {
    private enum Actions {
        FOLDER,
        ONEFOLDER,
        ORGANIZE
    }

    JButton allToOneFolderButton;
    JButton organizeButton;
    JMenuBar menuBar;
    JMenu file;
    JMenuItem open;
    JLabel label;
    JFileChooser chooser;
    boolean moveResult;

    public OrganizerGUI() {
        super("Photo Cleaner");
        setSize(350,250);
        setLayout(new FlowLayout());
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        addGuiComponents();
        setResizable(false);
    }

    private void addGuiComponents() {
        //Menu
        menuBar = new JMenuBar();
        file = new JMenu("Složka");
        open = new JMenuItem("Otevřít");

        file.add(open);
        menuBar.add(file);
        setJMenuBar(menuBar);
        open.setActionCommand(Actions.FOLDER.name());
        open.addActionListener(this);

        //Label
        label = new JLabel("Žádná složka nevybrána");
        add(label);

        //allToOneFolderButton Button
        allToOneFolderButton = new JButton("Přemístit do jednoho souboru");
        allToOneFolderButton.setActionCommand(Actions.ONEFOLDER.name());
        allToOneFolderButton.addActionListener(this);
        add(allToOneFolderButton);

        //organize Button
        organizeButton = new JButton("Organizace médií");
        organizeButton.setActionCommand(Actions.ORGANIZE.name());
        organizeButton.addActionListener(this);
        add(organizeButton);

    }

    @Override
    public void actionPerformed(ActionEvent e) {
        //Action for choosing folder path
        if(e.getActionCommand() == Actions.FOLDER.name()) {
            chooser = new JFileChooser(System.getProperty("user.home"));
            chooseFolder(chooser, label);

            // Logic for moving media to root directory
        }else if(e.getActionCommand() == Actions.ONEFOLDER.name()) {
            Path rootDirectory = Path.of(label.getText());
            if(label.getText() == "Žádná složka nevybrána")
                showErrorDialog(this);

            else {
                chooser.setCurrentDirectory(rootDirectory.toFile());
                // Moving files to rootDirectory
                try (Stream<Path> filePaths = Files.walk(rootDirectory)) {
                    filePaths
                            .filter(Files::isRegularFile)
                            .forEach(file -> {
                                Path fileNewPath = getUniqueFileName(rootDirectory.resolve(file.getFileName()));
                                try {
                                    Files.move(file, fileNewPath);
                                    System.out.println(file.getFileName() + " přesunuto do složky " + rootDirectory);
                                } catch (IOException ex) {
                                    throw new RuntimeException(ex);
                                }
                            });
                } catch (IOException x) {
                    System.err.println(x);
                }

                // Delete empty directories
                try (Stream<Path> filePaths = Files.walk(rootDirectory)) {
                    filePaths
                            .filter(Files::isDirectory)
                            .sorted(Comparator.reverseOrder())
                            .forEach(dir -> {
                                try(Stream<Path> entries = Files.list(dir)) {
                                    if (entries.findAny().isEmpty()){
                                        Files.delete(dir);
                                        System.out.println("Smazáno " + dir.getFileName() );
                                    }
                                }catch (IOException exceptionDir) {
                                    System.err.println("Chyba při čtení složky " + dir + ": " + exceptionDir.getMessage());
                                }
                            });
                } catch (IOException x) {
                    System.err.println(x);
                }
            }

            // Logic for organization media
        } else if (e.getActionCommand() == Actions.ORGANIZE.name()) {
            Path rootDirectory = Path.of(label.getText());

            if(label.getText() == "Žádná složka nevybrána")
                showErrorDialog(this);

            else {
                chooser.setCurrentDirectory(rootDirectory.toFile());
                File folder = new File(String.valueOf(rootDirectory));
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM");
                int countImageWithoutDate = 0;
                // First loop: Process files where imageDate != null
                for (File file : folder.listFiles()) {
                    if (file.isFile() && ! isVideoFile(file)) {
                        String imageDate = getImageCreatedDate(file, dateFormat);
                        if (imageDate != null) {
                            Path directoryByDate = rootDirectory.resolve(imageDate);
                            if (!Files.exists(directoryByDate)) {
                                try {
                                    Files.createDirectory(directoryByDate);
                                } catch (IOException ex) {
                                    throw new RuntimeException(ex);
                                }
                            }
                            try {
                                Path targetPath = directoryByDate.resolve(file.getName());
                                Files.move(file.toPath(), targetPath);
                                System.out.println(file.getName() + " přesunuto do složky " + directoryByDate);
                            } catch (IOException ex) {
                                throw new RuntimeException(ex);
                            }
                        } else
                            countImageWithoutDate++;
                    }
                }
                // Second loop: Process files where imageDate == null
                if(countImageWithoutDate>0) {
                    showMoveChooseDialog(countImageWithoutDate);
                        for (File file : folder.listFiles()) {
                            if (moveResult) {
                                chooser.setCurrentDirectory(rootDirectory.toFile());
                                if (file.isFile() && !isVideoFile(file)) {
                                    String imageDate = getImageCreatedDate(file, dateFormat);
                                    if (imageDate == null) {
                                        showImageWithoutDateDialog(file.getAbsolutePath(), chooser);
                                    }
                                }
                            }
                        }
                }
                // Organizer for Videos
                for (File file : folder.listFiles()) {
                    if (file.isFile() && isVideoFile(file)) {
                        if (getVideoCreatedDate(file) != null){
                            String originalDate = getVideoCreatedDate(file);
                            SimpleDateFormat originalFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH);
                            SimpleDateFormat yearMonthFormat = new SimpleDateFormat("yyyy-MM");

                            try {
                                Date date = originalFormat.parse(originalDate);
                                String yearMonth = yearMonthFormat.format(date);
                                Path directoryByDate = rootDirectory.resolve(yearMonth);
                                if (!Files.exists(directoryByDate)) {
                                    try {
                                        Files.createDirectory(directoryByDate);
                                    } catch (IOException ex) {
                                        throw new RuntimeException(ex);
                                    }
                                }
                                try {
                                    Path targetPath = directoryByDate.resolve(file.getName());
                                    Files.move(file.toPath(), targetPath);
                                    System.out.println(file.getName() + " přesunuto do složky " + directoryByDate);
                                } catch (IOException ex) {
                                    throw new RuntimeException(ex);
                                }
                            } catch (ParseException parseException) {
                                parseException.printStackTrace();
                            }
                        }
                    }
                }
            }
        }
    }
    private void showErrorDialog(Frame frame){
        JDialog noPathDialog = new JDialog(frame, "Chyba", true);
        noPathDialog.setSize(200,100);
        noPathDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        noPathDialog.setResizable(false);

        JLabel noPathLabel = new JLabel("Musí se vybrat cesta ke složce");
        noPathLabel.setFont(new Font("Dialog", Font.PLAIN, 12));
        noPathLabel.setHorizontalAlignment(SwingConstants.CENTER);
        noPathDialog.add(noPathLabel, BorderLayout.CENTER);

        noPathDialog.setLocationRelativeTo(frame);
        noPathDialog.setVisible(true);
    }

    private void showMoveChooseDialog(int imagesForMove) {
        final JDialog dialog = new JDialog(this, "Přemístit soubory?", true);
        dialog.setResizable(false);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(300, 120);

        JLabel label = new JLabel("Chcete přemístit " + imagesForMove + " soborů?");
        label.setFont(new Font("Dialog", Font.PLAIN, 12));
        label.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel buttonPanel = new JPanel();
        JButton yes = new JButton("Yes");
        JButton no = new JButton("No");

        yes.addActionListener(e -> {
            dialog.setVisible(false);
            moveResult = true;
        });

        no.addActionListener(e -> {
            dialog.setVisible(false);
            moveResult = false;
        });

        buttonPanel.add(yes);
        buttonPanel.add(no);

        dialog.add(label, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void showImageWithoutDateDialog(String filePath, JFileChooser chooser){
        JDialog dialog = new JDialog(this, "Obrázek bez datumu", true);
        dialog.setSize(300,180);
        dialog.setResizable(false);
        dialog.setLayout(null);
        Container pane = dialog.getContentPane();

        JButton preview = new JButton("Náhled");
        preview.setBounds(20,10,100,30);

        JButton chooseFolder = new JButton("Vybrat složku");
        chooseFolder.setBounds(150,10,120,30);

        JLabel imagePathLabel = new JLabel("Složka nevybrána");
        imagePathLabel.setBounds(10,100,290,30);

        JButton move = new JButton("Přemístit");
        move.setBounds(150,60,100,30);

        JButton cancel = new JButton("Zrušit");
        cancel.setBounds(20, 60, 100, 30);

        //cancelB action
        cancel.addActionListener(e -> {
            moveResult = false;
            dialog.setVisible(false);
        });

        //previewB action
        preview.addActionListener(e -> {
            try {
                previewImage(this, filePath);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });

        //chooseFolderB action
        chooseFolder.addActionListener(e -> {
            chooseFolder(chooser, imagePathLabel);
        });

        //moveB action
        move.addActionListener(e -> {
            if(imagePathLabel.getText().equals("Složka nevybrána"))
                showErrorDialog(this);
            else {
                Path directory = Paths.get(imagePathLabel.getText());
                Path fileName = Paths.get(filePath).getFileName();
                Path targetPath = directory.resolve(fileName);
                System.out.println(targetPath);
                try {
                    Files.move(Path.of(filePath), targetPath);
                    System.out.println(Path.of(filePath).getFileName() + " přesunuto do složky " + directory);
                    dialog.setVisible(false);
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });

        pane.add(preview);
        pane.add(chooseFolder);
        pane.add(cancel);
        pane.add(move);
        pane.add(imagePathLabel);

        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void previewImage(Frame frame, String filePath) throws Exception {
        BufferedImage originalImage = ImageIO.read(new File(filePath));
        BufferedImage resizedImage = resizeImage(originalImage, 300,300);
        JDialog imageDialog = new JDialog(frame, filePath, true);
        imageDialog.setSize(400, 400);
        imageDialog.setLocationRelativeTo(null);
        JLabel imageLabel = new JLabel(new ImageIcon(resizedImage));
        imageDialog.add(imageLabel);
        imageDialog.setVisible(true);
    }

    private void chooseFolder(JFileChooser chooser, JLabel label) {
        String path = null;
        chooser = new JFileChooser("C:\\Users\\jakub\\OneDrive\\Desktop\\new");
        chooser.setDialogTitle("Vyber složku s fotkami a videi");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);
        int returnVal = chooser.showOpenDialog(this);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            System.out.println("selectedDirectory: "
                    +  chooser.getSelectedFile());
            path = chooser.getSelectedFile().toString();
            label.setText(path);

        }
        else {
            System.out.println("No Selection ");
        }
    }

    private static Path getUniqueFileName(Path target) {
        Path newTarget = target;
        int count = 1;

        // Loop until we find a file name that doesn't exist.
        if(Files.exists(newTarget)) {
            String fileName = target.getFileName().toString();
            int dotIndex = fileName.lastIndexOf('.');
            String name = (dotIndex == -1) ? fileName : fileName.substring(0, dotIndex);
            String extension = (dotIndex == -1) ? "" : fileName.substring(dotIndex);

            // Generate a new file name with a count appended.
            newTarget = target.getParent().resolve(name + "("+count+")" + extension);
            count++;
        }
        return newTarget;
    }

    private static boolean isVideoFile(File file) {
        String[] videoExtensions = { "mp4", "avi", "mkv", "mov", "wmv", "flv", "webm" };
        String fileName = file.getName().toLowerCase();

        for (String extension : videoExtensions) {
            if (fileName.endsWith("." + extension)) {
                return true;
            }
        }
        return false;
    }

    private String getImageCreatedDate(File file, SimpleDateFormat dateFormat) {
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(file);
            ExifSubIFDDirectory directory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
            if (directory != null) {
                Date date = directory.getDateOriginal();
                if (date != null)
                    return dateFormat.format(date);
            }
        } catch (Exception eex) {
            System.err.println("Error reading metadata from file " + file.getName() + ": " + eex.getMessage());
        }
        return null;
    }

    private String getVideoCreatedDate(File file) {
        try {
            Metadata metadata = Mp4MetadataReader.readMetadata(file);
            for(Directory directory:metadata.getDirectories()) {
                for(Tag tag:directory.getTags()){
                    if(tag.getTagName().equals("Creation Time")) {
                        return tag.getDescription();
                    }
                }
            }

        }catch (Exception eex){
            System.err.println("Error reading metadata from file " + file.getName() + ": " + eex.getMessage());
        }
        return null;
    }

    public static BufferedImage resizeImage(BufferedImage originalImage, int targetWidth, int targetHeight) {
        // Create a new image with the desired size
        BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);

        // Rendering a scaled image
        Graphics2D graphics2D = resizedImage.createGraphics();
        graphics2D.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
        graphics2D.dispose();

        return resizedImage;
    }
}
