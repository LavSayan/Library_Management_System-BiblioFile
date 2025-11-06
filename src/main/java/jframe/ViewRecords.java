/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package jframe;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;

public class ViewRecords extends javax.swing.JFrame {

    private static final java.util.logging.Logger logger
            = java.util.logging.Logger.getLogger(ViewRecords.class.getName());

    DefaultTableModel model;

    public ViewRecords() {
        initComponents();
        model = (DefaultTableModel) tbl_issueBookDetails.getModel();
        clearTable();
        setIssueBookDetails();
        date_fromDate.setDateFormatString("yyyy-MM-dd");
        date_toDate.setDateFormatString("yyyy-MM-dd");
    }

    public void setIssueBookDetails() {
        clearTable();
        String sql = "SELECT * FROM issue_book_details ORDER BY issue_date";

        try (Connection con = DBConnection.getConnection(); Statement st = con.createStatement(); ResultSet rs = st.executeQuery(sql)) {

            SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");

            while (rs.next()) {
                String id = rs.getString("id");
                String bookName = rs.getString("book_name");

                // Mask user name
                String userName = rs.getString("user_name");
                int visibleChars = 2;
                String maskedUserName = userName.length() > visibleChars
                        ? userName.substring(0, visibleChars) + "****"
                        : "****";

                Timestamp issueTs = rs.getTimestamp("issue_date");
                Timestamp dueTs = rs.getTimestamp("due_date");

                String issueDate = (issueTs != null) ? fmt.format(issueTs) : "";
                String dueDate = (dueTs != null) ? fmt.format(dueTs) : "";

                String status = rs.getString("status");

                Object[] obj = {id, bookName, maskedUserName, issueDate, dueDate, status};
                model.addRow(obj);
            }

        } catch (Exception e) {
            logger.severe("Error loading issue_book_details: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Error loading records: " + e.getMessage());
        }
    }

    public void clearTable() {
        if (model == null) {
            model = (DefaultTableModel) tbl_issueBookDetails.getModel();
        }
        model.setRowCount(0);
    }

    public void search() {
        Date uFromDate = date_fromDate.getDate();
        Date uToDate = date_toDate.getDate();

        if (uFromDate == null || uToDate == null) {
            JOptionPane.showMessageDialog(this, "Please select both From and To dates");
            return;
        }

        if (uFromDate.after(uToDate)) {
            JOptionPane.showMessageDialog(this, "'From' date must be before or equal to 'To' date");
            return;
        }

        ZoneId zone = ZoneId.systemDefault();
        LocalDate fromLocal = Instant.ofEpochMilli(uFromDate.getTime()).atZone(zone).toLocalDate();
        LocalDate toLocal = Instant.ofEpochMilli(uToDate.getTime()).atZone(zone).toLocalDate();

        Instant startInstant = fromLocal.atStartOfDay(zone).toInstant();
        Instant endExclusiveInstant = toLocal.plusDays(1).atStartOfDay(zone).toInstant();

        Timestamp startTs = Timestamp.from(startInstant);
        Timestamp endExclusiveTs = Timestamp.from(endExclusiveInstant);

        String sql = "SELECT * FROM issue_book_details "
                + "WHERE issue_date >= ? AND issue_date < ? "
                + "  AND due_date   >= ? AND due_date   < ? "
                + "ORDER BY issue_date";

        clearTable();

        try (Connection con = DBConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setTimestamp(1, startTs);
            pst.setTimestamp(2, endExclusiveTs);
            pst.setTimestamp(3, startTs);
            pst.setTimestamp(4, endExclusiveTs);

            try (ResultSet rs = pst.executeQuery()) {

                SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");

                boolean found = false;
                while (rs.next()) {
                    found = true;
                    String id = rs.getString("id");
                    String bookName = rs.getString("book_name");

                    // Mask user name
                    String userName = rs.getString("user_name");
                    int visibleChars = 2;
                    String maskedUserName = userName.length() > visibleChars
                            ? userName.substring(0, visibleChars) + "****"
                            : "****";

                    Timestamp issueTs = rs.getTimestamp("issue_date");
                    Timestamp dueTs = rs.getTimestamp("due_date");

                    String issueDate = (issueTs != null) ? fmt.format(issueTs) : "";
                    String dueDate = (dueTs != null) ? fmt.format(dueTs) : "";

                    String status = rs.getString("status");

                    Object[] obj = {id, bookName, maskedUserName, issueDate, dueDate, status};
                    model.addRow(obj);
                }

                if (!found) {
                    JOptionPane.showMessageDialog(this, "No Record Found");
                }
            }

        } catch (Exception e) {
            logger.severe("Search error: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Error fetching records: " + e.getMessage());
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel72 = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jPanel3 = new javax.swing.JPanel();
        jLabel93 = new javax.swing.JLabel();
        jLabel94 = new javax.swing.JLabel();
        rSMaterialButtonCircle3 = new rojerusan.RSMaterialButtonCircle();
        rSMaterialButtonCircle4 = new rojerusan.RSMaterialButtonCircle();
        date_toDate = new com.toedter.calendar.JDateChooser();
        date_fromDate = new com.toedter.calendar.JDateChooser();
        jPanel4 = new javax.swing.JPanel();
        jPanel5 = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        tbl_issueBookDetails = new rojeru_san.complementos.RSTableMetro();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setUndecorated(true);
        getContentPane().setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel72.setFont(new java.awt.Font("Serif", 0, 17)); // NOI18N
        jLabel72.setForeground(new java.awt.Color(255, 255, 255));
        jLabel72.setIcon(new javax.swing.ImageIcon(getClass().getResource("/AddNewBookIcons/icons8_Rewind_48px.png"))); // NOI18N
        jLabel72.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jLabel72MouseClicked(evt);
            }
        });
        getContentPane().add(jLabel72, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 0, 50, 70));

        jPanel1.setBackground(new java.awt.Color(120, 27, 27));
        jPanel1.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel2.setFont(new java.awt.Font("Serif", 1, 25)); // NOI18N
        jLabel2.setForeground(new java.awt.Color(255, 255, 255));
        jLabel2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/AddNewBookIcons/icons8_Literature_100px_1.png"))); // NOI18N
        jLabel2.setText("    Records Details");
        jPanel1.add(jLabel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(570, 20, 300, -1));

        jPanel3.setBackground(new java.awt.Color(255, 255, 255));

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 340, Short.MAX_VALUE)
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 5, Short.MAX_VALUE)
        );

        jPanel1.add(jPanel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(550, 140, 340, 5));

        jLabel93.setFont(new java.awt.Font("Serif", 0, 17)); // NOI18N
        jLabel93.setForeground(new java.awt.Color(255, 255, 255));
        jLabel93.setText("Start Date : ");
        jPanel1.add(jLabel93, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 200, 90, -1));

        jLabel94.setFont(new java.awt.Font("Serif", 0, 17)); // NOI18N
        jLabel94.setForeground(new java.awt.Color(255, 255, 255));
        jLabel94.setText("End Date : ");
        jPanel1.add(jLabel94, new org.netbeans.lib.awtextra.AbsoluteConstraints(550, 200, 90, -1));

        rSMaterialButtonCircle3.setBackground(new java.awt.Color(255, 255, 255));
        rSMaterialButtonCircle3.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(255, 255, 255), 1, true));
        rSMaterialButtonCircle3.setForeground(new java.awt.Color(120, 27, 27));
        rSMaterialButtonCircle3.setText("RESET");
        rSMaterialButtonCircle3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rSMaterialButtonCircle3ActionPerformed(evt);
            }
        });
        jPanel1.add(rSMaterialButtonCircle3, new org.netbeans.lib.awtextra.AbsoluteConstraints(1270, 180, 180, 60));

        rSMaterialButtonCircle4.setBackground(new java.awt.Color(255, 255, 255));
        rSMaterialButtonCircle4.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(255, 255, 255), 1, true));
        rSMaterialButtonCircle4.setForeground(new java.awt.Color(120, 27, 27));
        rSMaterialButtonCircle4.setText("SEARCH");
        rSMaterialButtonCircle4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rSMaterialButtonCircle4ActionPerformed(evt);
            }
        });
        jPanel1.add(rSMaterialButtonCircle4, new org.netbeans.lib.awtextra.AbsoluteConstraints(1060, 180, 180, 60));

        date_toDate.setBorder(javax.swing.BorderFactory.createMatteBorder(1, 1, 1, 1, new java.awt.Color(120, 27, 27)));
        date_toDate.setForeground(new java.awt.Color(120, 27, 27));
        date_toDate.setDateFormatString("yyyy,MM,dd");
        date_toDate.setFont(new java.awt.Font("Serif", 0, 17)); // NOI18N
        jPanel1.add(date_toDate, new org.netbeans.lib.awtextra.AbsoluteConstraints(660, 190, 360, 40));

        date_fromDate.setBorder(javax.swing.BorderFactory.createMatteBorder(1, 1, 1, 1, new java.awt.Color(120, 27, 27)));
        date_fromDate.setForeground(new java.awt.Color(120, 27, 27));
        date_fromDate.setDateFormatString("yyyy,MM,dd");
        date_fromDate.setFont(new java.awt.Font("Serif", 0, 17)); // NOI18N
        jPanel1.add(date_fromDate, new org.netbeans.lib.awtextra.AbsoluteConstraints(140, 190, 360, 40));

        jPanel4.setBackground(new java.awt.Color(120, 27, 27));
        jPanel4.setBorder(javax.swing.BorderFactory.createMatteBorder(2, 2, 2, 2, new java.awt.Color(255, 255, 255)));

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 376, Short.MAX_VALUE)
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 56, Short.MAX_VALUE)
        );

        jPanel1.add(jPanel4, new org.netbeans.lib.awtextra.AbsoluteConstraints(130, 180, 380, 60));

        jPanel5.setBackground(new java.awt.Color(120, 27, 27));
        jPanel5.setBorder(javax.swing.BorderFactory.createMatteBorder(2, 2, 2, 2, new java.awt.Color(255, 255, 255)));

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 378, Short.MAX_VALUE)
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 58, Short.MAX_VALUE)
        );

        jPanel1.add(jPanel5, new org.netbeans.lib.awtextra.AbsoluteConstraints(650, 180, -1, -1));

        getContentPane().add(jPanel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 1490, 280));

        jPanel2.setBackground(new java.awt.Color(255, 255, 255));
        jPanel2.setBorder(new javax.swing.border.MatteBorder(null));
        jPanel2.setToolTipText("");
        jPanel2.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        tbl_issueBookDetails.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        tbl_issueBookDetails.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Record Id", "Book Name", "User Name", "Issue Date", "Due Date", "Status"
            }
        ));
        tbl_issueBookDetails.setColorBackgoundHead(new java.awt.Color(120, 27, 27));
        tbl_issueBookDetails.setColorFilasForeground1(new java.awt.Color(0, 0, 0));
        tbl_issueBookDetails.setColorFilasForeground2(new java.awt.Color(0, 0, 0));
        tbl_issueBookDetails.setColorSelBackgound(new java.awt.Color(153, 0, 0));
        tbl_issueBookDetails.setFont(new java.awt.Font("Serif", 0, 14)); // NOI18N
        tbl_issueBookDetails.setFuenteFilas(new java.awt.Font("Serif", 1, 14)); // NOI18N
        tbl_issueBookDetails.setFuenteFilasSelect(new java.awt.Font("Serif", 1, 18)); // NOI18N
        tbl_issueBookDetails.setFuenteHead(new java.awt.Font("Serif", 1, 20)); // NOI18N
        tbl_issueBookDetails.setRowHeight(40);
        tbl_issueBookDetails.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                tbl_issueBookDetailsMouseClicked(evt);
            }
        });
        jScrollPane1.setViewportView(tbl_issueBookDetails);

        jPanel2.add(jScrollPane1, new org.netbeans.lib.awtextra.AbsoluteConstraints(120, 50, 1240, 480));

        getContentPane().add(jPanel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 280, 1480, 580));

        setSize(new java.awt.Dimension(1481, 857));
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void rSMaterialButtonCircle3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rSMaterialButtonCircle3ActionPerformed
        clearTable();
        setIssueBookDetails();

    }//GEN-LAST:event_rSMaterialButtonCircle3ActionPerformed

    private void tbl_issueBookDetailsMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tbl_issueBookDetailsMouseClicked

    }//GEN-LAST:event_tbl_issueBookDetailsMouseClicked

    private void jLabel72MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jLabel72MouseClicked
        // TODO add your handling code here:
        HomePage home = new HomePage();
        home.setVisible(true);
        dispose();
    }//GEN-LAST:event_jLabel72MouseClicked

    private void rSMaterialButtonCircle4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rSMaterialButtonCircle4ActionPerformed
        if (date_toDate.getDate() != null && date_toDate.getDate() != null) {
            clearTable();
            search();
        } else {
            JOptionPane.showMessageDialog(this, "Please Select a Date");
        }

    }//GEN-LAST:event_rSMaterialButtonCircle4ActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException ex) {
            logger.log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> new ViewRecords().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private com.toedter.calendar.JDateChooser date_fromDate;
    private com.toedter.calendar.JDateChooser date_toDate;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel72;
    private javax.swing.JLabel jLabel93;
    private javax.swing.JLabel jLabel94;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JScrollPane jScrollPane1;
    private rojerusan.RSMaterialButtonCircle rSMaterialButtonCircle3;
    private rojerusan.RSMaterialButtonCircle rSMaterialButtonCircle4;
    private rojeru_san.complementos.RSTableMetro tbl_issueBookDetails;
    // End of variables declaration//GEN-END:variables
}
