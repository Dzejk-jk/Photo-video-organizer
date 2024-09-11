import javax.swing.*;

public class App {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                OrganizerGUI appGUI = new OrganizerGUI();
                appGUI.setVisible(true);
            }
        });
    }
}
