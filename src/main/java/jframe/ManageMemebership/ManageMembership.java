/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package jframe.ManageMemebership;

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
import jframe.DBConnection;
import jframe.HomePage;

public class ManageMembership extends javax.swing.JFrame {

    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(ManageMembership.class.getName());

    public ManageMembership() {
        initComponents();
        setLocationRelativeTo(null);
        loadMembershipDetails();
    }

    private void loadMembershipDetails() {
        try (Connection con = DBConnection.getConnection(); PreparedStatement pst = con.prepareStatement(
                "SELECT user_id, user_name, membership_type, start_date, end_date, status FROM membership_details WHERE membership_type = 'Scribe'"
        )) {

            DefaultTableModel model = (DefaultTableModel) tbl_membershipDetails.getModel();
            model.setRowCount(0);

            Date today = new Date();
            ResultSet rs = pst.executeQuery();

            while (rs.next()) {
                int userId = rs.getInt("user_id");
                String userName = rs.getString("user_name");
                String membershipType = rs.getString("membership_type");
                Date startDate = rs.getDate("start_date");
                Date endDate = rs.getDate("end_date");
                String status = rs.getString("status");

                // UI-only masking
                int visibleChars = 2;
                String maskedName = userName.length() > visibleChars
                        ? userName.substring(0, visibleChars) + "****"
                        : "****";

                // Expiration logic
                if (endDate.before(today) && !status.equalsIgnoreCase("expired")) {
                    try (PreparedStatement pst1 = con.prepareStatement(
                            "UPDATE membership_details SET status = 'expired' WHERE user_id = ?")) {
                        pst1.setInt(1, userId);
                        pst1.executeUpdate();
                    }

                    try (PreparedStatement pst2 = con.prepareStatement(
                            "UPDATE user_details SET membership_type = 'Normal' WHERE user_id = ?")) {
                        pst2.setInt(1, userId);
                        pst2.executeUpdate();
                    }

                    status = "expired";
                }

                // Cancellation logic
                if (status.equalsIgnoreCase("cancelled")) {
                    try (PreparedStatement pst3 = con.prepareStatement(
                            "UPDATE user_details SET membership_type = 'Normal' WHERE user_id = ?")) {
                        pst3.setInt(1, userId);
                        pst3.executeUpdate();
                    }
                }

                Object[] row = {userId, maskedName, membershipType, startDate, endDate, status};
                model.addRow(row);
            }

        } catch (Exception e) {
            logger.severe("Error loading membership details: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Error loading membership details: " + e.getMessage());
        }
    }

    private void cancelMembership() {
        int selectedRow = tbl_membershipDetails.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a membership record to cancel.");
            return;
        }

        int userId = (int) tbl_membershipDetails.getValueAt(selectedRow, 0);

        String sql = "UPDATE membership_details SET status = 'cancelled' WHERE user_id = ? AND membership_type = 'Scribe'";

        try (Connection con = DBConnection.getConnection(); PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setInt(1, userId);

            if (pst.executeUpdate() > 0) {
                // Also downgrade user to Normal
                try (PreparedStatement pst2 = con.prepareStatement(
                        "UPDATE user_details SET membership_type = 'Normal' WHERE user_id = ?")) {
                    pst2.setInt(1, userId);
                    pst2.executeUpdate();
                }

                JOptionPane.showMessageDialog(this, "Membership cancelled.");
                loadMembershipDetails();
            } else {
                JOptionPane.showMessageDialog(this, "No active Scribe membership found for this user.");
            }

        } catch (Exception e) {
            logger.severe("Error cancelling membership: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Error cancelling membership: " + e.getMessage());
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
        btn_cancel = new rojerusan.RSMaterialButtonCircle();
        btn_renew = new rojerusan.RSMaterialButtonCircle();
        jPanel2 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        tbl_membershipDetails = new rojeru_san.complementos.RSTableMetro();

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
        jLabel2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/AddNewBookIcons/membership.png"))); // NOI18N
        jLabel2.setText("    Manage Membership");
        jPanel1.add(jLabel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(380, 40, 370, -1));

        jPanel3.setBackground(new java.awt.Color(255, 255, 255));

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 390, Short.MAX_VALUE)
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 5, Short.MAX_VALUE)
        );

        jPanel1.add(jPanel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(370, 150, 390, 5));

        btn_cancel.setBackground(new java.awt.Color(255, 255, 255));
        btn_cancel.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(255, 255, 255), 1, true));
        btn_cancel.setForeground(new java.awt.Color(120, 27, 27));
        btn_cancel.setText("CANCEL");
        btn_cancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_cancelActionPerformed(evt);
            }
        });
        jPanel1.add(btn_cancel, new org.netbeans.lib.awtextra.AbsoluteConstraints(580, 200, 180, 60));

        btn_renew.setBackground(new java.awt.Color(255, 255, 255));
        btn_renew.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(255, 255, 255), 1, true));
        btn_renew.setForeground(new java.awt.Color(120, 27, 27));
        btn_renew.setText("RENEW");
        btn_renew.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_renewActionPerformed(evt);
            }
        });
        jPanel1.add(btn_renew, new org.netbeans.lib.awtextra.AbsoluteConstraints(380, 200, 180, 60));

        getContentPane().add(jPanel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 1150, 280));

        jPanel2.setBackground(new java.awt.Color(255, 255, 255));
        jPanel2.setBorder(new javax.swing.border.MatteBorder(null));
        jPanel2.setToolTipText("");
        jPanel2.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        tbl_membershipDetails.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        tbl_membershipDetails.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "User Id", "User Name", "Membership Type", "Start Date", "End Date", "Status"
            }
        ));
        tbl_membershipDetails.setColorBackgoundHead(new java.awt.Color(120, 27, 27));
        tbl_membershipDetails.setColorFilasForeground1(new java.awt.Color(0, 0, 0));
        tbl_membershipDetails.setColorFilasForeground2(new java.awt.Color(0, 0, 0));
        tbl_membershipDetails.setColorSelBackgound(new java.awt.Color(153, 0, 0));
        tbl_membershipDetails.setFont(new java.awt.Font("Serif", 0, 14)); // NOI18N
        tbl_membershipDetails.setFuenteFilas(new java.awt.Font("Serif", 1, 14)); // NOI18N
        tbl_membershipDetails.setFuenteFilasSelect(new java.awt.Font("Serif", 1, 18)); // NOI18N
        tbl_membershipDetails.setFuenteHead(new java.awt.Font("Serif", 1, 20)); // NOI18N
        tbl_membershipDetails.setRowHeight(40);
        tbl_membershipDetails.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                tbl_membershipDetailsMouseClicked(evt);
            }
        });
        jScrollPane1.setViewportView(tbl_membershipDetails);

        jPanel2.add(jScrollPane1, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 1150, 580));

        getContentPane().add(jPanel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 280, 1150, 580));

        setSize(new java.awt.Dimension(1151, 857));
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void btn_cancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_cancelActionPerformed
        cancelMembership();
    }//GEN-LAST:event_btn_cancelActionPerformed

    private void tbl_membershipDetailsMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tbl_membershipDetailsMouseClicked

    }//GEN-LAST:event_tbl_membershipDetailsMouseClicked

    private void jLabel72MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jLabel72MouseClicked
        // TODO add your handling code here:
        HomePage home = new HomePage();
        home.setVisible(true);
        dispose();
    }//GEN-LAST:event_jLabel72MouseClicked

    private void btn_renewActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_renewActionPerformed
        MembershipPayment payment = new MembershipPayment();
        payment.setVisible(true);
        dispose();
    }//GEN-LAST:event_btn_renewActionPerformed

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
        java.awt.EventQueue.invokeLater(() -> new ManageMembership().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private rojerusan.RSMaterialButtonCircle btn_cancel;
    private rojerusan.RSMaterialButtonCircle btn_renew;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel72;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JScrollPane jScrollPane1;
    private rojeru_san.complementos.RSTableMetro tbl_membershipDetails;
    // End of variables declaration//GEN-END:variables

}
