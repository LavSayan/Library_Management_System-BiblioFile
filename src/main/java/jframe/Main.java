package jframe;

public class Main {
    public static void main(String[] args) {
        // Set the Nimbus look and feel (optional, matches your other frames)
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            // Log or ignore if look and feel fails
        }

        // Launch the SignupPage
        java.awt.EventQueue.invokeLater(() -> {
            new SignupPage().setVisible(true);
        });
    }
}
