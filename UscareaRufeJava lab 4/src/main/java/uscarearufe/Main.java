package uscarearufe;

import uscarearufe.ui.MainForm;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Prognoza timpului de uscare a rufelor — sistem fuzzy Mamdani.
 * IntelliJ: Run 'Main.main()'
 */
public final class Main {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> new MainForm().setVisible(true));
    }
}
