import krause.vna.data.calibrated.VNACalibratedSampleBlock;
import krause.vna.library.VNALibrary;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiDevice.Info;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.swing.JPanel;

public class final_Code {

	
	public static VNALibrary lib;
	public static VNACalibratedSampleBlock rc;
	public static boolean loop_Check = true;
	public static boolean Calibrating = true;
	public static float[] transLoss;
	public static float sum;
	public static float cumSum;
	public static float offCalValue;
	public static float onCalValue;
	public static MidiDevice device;
	public static Receiver rcvr;
	
	public static void main(String[] args) throws  InvalidMidiDataException, MidiUnavailableException {
		connectToVNA();
		getTheReceiver();
		device.open();
		collectInitialData();
		CreateDialogFromOptionPane();
		while (loop_Check) {
			if (!Calibrating) {
			runTransmissionTestlib();
			sendMidiMessage();
			}
		}
		lib.shutdown();
		device.close();
	}
	
	public static void connectToVNA() {
		lib = null;
		rc = null;
		
		try {
			lib = new VNALibrary();
			lib.loadDriverByName("miniVNA Tiny", "COM10");
			System.out.println("Connected to VNA");
		} catch (Exception e) {
			System.out.println("failed with " + e.getMessage());
		}
		
	}
	
	public static void runTransmissionTestlib() {
		
		try {
		  lib.loadCalibrationFile("C:\\Users\\tlepa\\vnaJ.3.4\\calibration\\Tran_Cal_220801_New.cal");  
		  rc = lib.scan(1300000000, 1700000000, 10, "TRAN");
		  transLoss = new float[rc.getCalibratedSamples().length];
		  for (int i = 0; i < (rc.getCalibratedSamples().length - 1);i++) {
			  float data = (float)rc.getCalibratedSamples()[i].getTransmissionLoss();
			  transLoss[i] = data;
		  }
		  float sum[] = cumulativeSum(transLoss);
		  cumSum = sum[rc.getCalibratedSamples().length-1];
		  System.out.println(cumSum);
		  
		} catch (Exception e) {
			System.out.println("failed with " + e.getMessage());
		}
	}
	
	public static void sendMidiMessage() throws InvalidMidiDataException, MidiUnavailableException {
		
		int midi_low = 0;
		int midi_high = 127;
		
		float slope = (midi_high - midi_low) / ((onCalValue) - offCalValue);
		System.out.println(slope);
		int midiPressure = (int) (midi_low + Math.round(slope * (cumSum - offCalValue)));
		if (midiPressure > 127) {
			midiPressure = 127;
//			onCalValue = midiPressure;
		}
		else if (midiPressure < 0) {
			midiPressure = 0;
		}
		System.out.println(midiPressure);
		ShortMessage myMsg = new ShortMessage();
		long timeStamp = -1;
		myMsg.setMessage(176, 0, 50, midiPressure);
//		sleep(2000);
		rcvr.send(myMsg, timeStamp);
	}
	
	public static void CreateDialogFromOptionPane() {
        JFrame parent = new JFrame();
        JPanel panel = new JPanel();
        JButton button_start = new JButton("Start Test");
        JButton button_stop = new JButton("Stop Test");

//        Container contentPane = parent.getContentPane();	
//        button_start.setText("Click to end test");
//        button_stop.setText("Click to end test");
        panel.add(button_start);
        panel.add(button_stop);
        parent.add(panel);
        parent.pack();
        parent.setVisible(true);

        button_stop.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                parent.setVisible(false);
                loop_Check = false;
                
            }
        });
        
        button_start.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                parent.setVisible(false);
                loop_Check = false;
                
            }
        });
        
    }
	
	public static void collectInitialData() {
        JFrame parent = new JFrame();
        JPanel panel = new JPanel();
        JButton button_OffData = new JButton("Collect No Touch Calibration Data");
        JButton button_OnData = new JButton("Collect Touch Calibration Data ");

//        Container contentPane = parent.getContentPane();	
//        button_start.setText("Click to end test");
//        button_stop.setText("Click to end test");
//        JOptionPane.showMessageDialog(panel, "Click Here Once Done Calibrating");
        panel.add(button_OffData);
        panel.add(button_OnData);
        parent.add(panel);
        parent.pack();
        parent.setVisible(true);
        
//        JFrame stopFrame = new JFrame();

        button_OffData.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
            	int i = 10;
            	float sumOffCal = 0;
            	for(int j = 0; j < i; j++) {
            		runTransmissionTestlib();
            		sumOffCal += cumSum;
            	}
            	offCalValue = sumOffCal / i;
            }
        });
        
        button_OnData.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
            	int i = 10;
            	float sumOnCal = 0;
            	for(int j = 0; j < i; j++) {
            		runTransmissionTestlib();
            		sumOnCal += cumSum;
            	}
            	onCalValue = sumOnCal / i;
                parent.setVisible(false);
                Calibrating = false;
                
            }
        });
        
    }
	
	public static void getTheReceiver() throws MidiUnavailableException {
		
		MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
		for(Info devices : infos )
		{
			if(devices.getName().equals("2- LoopBe Internal MIDI") && devices.getDescription().equals("External MIDI Port")) {
				device = MidiSystem.getMidiDevice(devices);
				rcvr =  device.getReceiver();
			}
		}
	}
	
    public static void sleep(int time)
    {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    public static float[] cumulativeSum(float[] numbers) {
        // variable
        float sum = 0;

        // traverse through the array
        for (int i = 0; i < numbers.length; i++) {
          sum += numbers[i]; // find sum
          numbers[i] = sum; // replace
        }
        
        return numbers;
    }
	


}
