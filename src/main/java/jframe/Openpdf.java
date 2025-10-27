package jframe;

import javax.swing.JOptionPane;
import java.io.File;

public class Openpdf {
    public static void OpenById(String id) {
        try {
            String filePath = InventoryUtils.billPath + id + ".pdf";
            File file = new File(filePath);

            if (file.exists()) {
                Process p = Runtime.getRuntime()
                        .exec("rundll32 url.dll,FileProtocolHandler " + file.getAbsolutePath());
            } else {
                JOptionPane.showMessageDialog(null, "File does not exist: " + filePath);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error opening PDF: " + e.getMessage());
        }
    }
}
