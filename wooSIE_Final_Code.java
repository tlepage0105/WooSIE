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

	//Variables to be used throughout all functions
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
	
	//Main function
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
	
	//Connect to VNA
	public static void connectToVNA() {
		lib = null;
		rc = null;
		
		//Throw an error if there is a connection error
		try {
			lib = new VNALibrary();
			lib.loadDriverByName("miniVNA Tiny", "COM10");
			System.out.println("Connected to VNA");
		} catch (Exception e) {
			System.out.println("failed with " + e.getMessage());
		}
		
	}
	
	//Run the transmission test
	public static void runTransmissionTestlib() {
		
		try {
		  //////This needs to be updated based on users settings///////
		  lib.loadCalibrationFile("REPLACE WITH FULL CALIBRATION FILE PATH");  
		  /////////////////////////////////////////////////////////////
		  
		  ////////These values are frequencies in Hertz to sweep through/////
		  rc = lib.scan(1300000000, 1700000000, 10, "TRAN");
		  //////////////////////////////////////////////////////////////////
		  
		  //Get the transmission loss of each frequency
		  transLoss = new float[rc.getCalibratedSamples().length];
		  for (int i = 0; i < (rc.getCalibratedSamples().length - 1);i++) {
			  float data = (float)rc.getCalibratedSamples()[i].getTransmissionLoss();
			  transLoss[i] = data;
		  }
		  //Find cumulative sum of the transmission loss
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
		
		//Mapping of the midi values to the cumulative sum values
		float slope = (midi_high - midi_low) / ((onCalValue) - offCalValue);
		System.out.println(slope);
		int midiPressure = (int) (midi_low + Math.round(slope * (cumSum - offCalValue)));
		//Making sure the midid values do not go outside their range and throw an error
		if (midiPressure > 127) {
			midiPressure = 127;
		}
		else if (midiPressure < 0) {
			midiPressure = 0;
		}
		System.out.println(midiPressure);
		
		//Creating the midi message to send
		ShortMessage myMsg = new ShortMessage();
		long timeStamp = -1;
		myMsg.setMessage(176, 0, 50, midiPressure);
		rcvr.send(myMsg, timeStamp);
	}
	
	//Button to stop testing
	public static void CreateDialogFromOptionPane() {
        JFrame parent = new JFrame();
        JPanel panel = new JPanel();
        JButton button_stop = new JButton("Stop Test");

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
        
    }
	
	//Button to find the calibration values for midi mapping
	public static void collectInitialData() {
        JFrame parent = new JFrame();
        JPanel panel = new JPanel();
        JButton button_OffData = new JButton("Collect No Touch Calibration Data");
        JButton button_OnData = new JButton("Collect Touch Calibration Data ");

        panel.add(button_OffData);
        panel.add(button_OnData);
        parent.add(panel);
        parent.pack();
        parent.setVisible(true);
        parent.setLocation(250,0);

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
	
	//Finding the receiving device to send midi message to. For this application LoopBe was used
	public static void getTheReceiver() throws MidiUnavailableException {
		
		MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
		for(Info devices : infos )
		{
			///////This line will need to get updated if other devices are used.///////////
			if(devices.getName().equals("2- LoopBe Internal MIDI") && devices.getDescription().equals("External MIDI Port")) {
			//////////////////////////////////////////////////////////////////////////////	
				device = MidiSystem.getMidiDevice(devices);
				rcvr =  device.getReceiver();
			}
		}
	}
	
	//Sleep function used for debugging
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
