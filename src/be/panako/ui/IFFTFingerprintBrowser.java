/***************************************************************************
*                                                                          *
* Panako - acoustic fingerprinting                                         *
* Copyright (C) 2014 - 2015 - Joren Six / IPEM                             *
*                                                                          *
* This program is free software: you can redistribute it and/or modify     *
* it under the terms of the GNU Affero General Public License as           *
* published by the Free Software Foundation, either version 3 of the       *
* License, or (at your option) any later version.                          *
*                                                                          *
* This program is distributed in the hope that it will be useful,          *
* but WITHOUT ANY WARRANTY; without even the implied warranty of           *
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the            *
* GNU Affero General Public License for more details.                      *
*                                                                          *
* You should have received a copy of the GNU Affero General Public License *
* along with this program.  If not, see <http://www.gnu.org/licenses/>     *
*                                                                          *
****************************************************************************
*    ______   ________   ___   __    ________   ___   ___   ______         *
*   /_____/\ /_______/\ /__/\ /__/\ /_______/\ /___/\/__/\ /_____/\        *
*   \:::_ \ \\::: _  \ \\::\_\\  \ \\::: _  \ \\::.\ \\ \ \\:::_ \ \       *
*    \:(_) \ \\::(_)  \ \\:. `-\  \ \\::(_)  \ \\:: \/_) \ \\:\ \ \ \      *
*     \: ___\/ \:: __  \ \\:. _    \ \\:: __  \ \\:. __  ( ( \:\ \ \ \     *
*      \ \ \    \:.\ \  \ \\. \`-\  \ \\:.\ \  \ \\: \ )  \ \ \:\_\ \ \    *
*       \_\/     \__\/\__\/ \__\/ \__\/ \__\/\__\/ \__\/\__\/  \_____\/    *
*                                                                          *
****************************************************************************
*                                                                          *
*                              Panako                                      *
*                       Acoustic Fingerprinting                            *
*                                                                          *
****************************************************************************/



package be.panako.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;

import be.panako.strategy.ifft.IFFTEventPoint;
import be.panako.strategy.ifft.IFFTEventPointProcessor;
import be.panako.strategy.ifft.IFFTFingerprint;
import be.panako.util.Config;
import be.panako.util.Key;
import be.panako.util.StopWatch;
import be.panako.util.TimeUnit;
import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.dsp.ui.Axis;
import be.tarsos.dsp.ui.AxisUnit;
import be.tarsos.dsp.ui.CoordinateSystem;
import be.tarsos.dsp.ui.LinkedPanel;
import be.tarsos.dsp.ui.ViewPort;
import be.tarsos.dsp.ui.ViewPort.ViewPortChangedListener;
import be.tarsos.dsp.ui.layers.BackgroundLayer;
import be.tarsos.dsp.ui.layers.DragMouseListenerLayer;
import be.tarsos.dsp.ui.layers.Layer;
import be.tarsos.dsp.ui.layers.LayerUtilities;
import be.tarsos.dsp.ui.layers.SelectionLayer;
import be.tarsos.dsp.ui.layers.TimeAxisLayer;
import be.tarsos.dsp.ui.layers.ZoomMouseListenerLayer;
import be.tarsos.dsp.util.PitchConverter;
import be.tarsos.dsp.util.fft.FFT;

public class IFFTFingerprintBrowser extends JFrame{

	/**
	 * 
	 */
	private static final long serialVersionUID = 8131793763940515009L;
	
	
	private TreeMap<Float,float[]> magnitudes;
	
	private  List<IFFTEventPoint> referenceEventPoints;
	private  List<IFFTEventPoint> otherEventPoints;
	private  List<IFFTEventPoint> matchingEventPoints;
	private  List<IFFTFingerprint> referenceFingerprints;
	private  List<IFFTFingerprint> matchingPrints;
	
	private final float[] binStartingPointsInCents;
	private final float[] binHeightsInCents;
	
	public IFFTFingerprintBrowser(){
		this.setLayout(new BorderLayout());
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setTitle("IFFT Fingerprint Visualizer");
		
		magnitudes = new TreeMap<Float,float[]>();
		
		referenceEventPoints = new ArrayList<IFFTEventPoint>();
		otherEventPoints =  new ArrayList<IFFTEventPoint>();
		matchingEventPoints =  new ArrayList<IFFTEventPoint>();
		referenceFingerprints =  new ArrayList<IFFTFingerprint>();
		 matchingPrints =  new ArrayList<IFFTFingerprint>();
		
		int size = Config.getInt(Key.IFFT_SIZE);
		FFT fft = new FFT(size);
		 
		 binStartingPointsInCents = new float[size];
			binHeightsInCents = new float[size];
			for (int i = 1; i < size; i++) {
				binStartingPointsInCents[i] = (float) PitchConverter.hertzToAbsoluteCent(fft.binToHz(i,8000));
				binHeightsInCents[i] = binStartingPointsInCents[i] - binStartingPointsInCents[i-1];
			}
		
		
		
		
		
		this.add(createFeaturePanel(),BorderLayout.CENTER);
		this.add(createButtonPanel(),BorderLayout.SOUTH);
	}
	
	private Component createFeaturePanel() {
		final CoordinateSystem cs = new CoordinateSystem(AxisUnit.FREQUENCY, 3500, 11900);
		final LinkedPanel frequencyDomainPanel = new LinkedPanel(cs);
		frequencyDomainPanel.getViewPort().addViewPortChangedListener(new ViewPortChangedListener() {
			
			@Override
			public void viewPortChanged(ViewPort newViewPort) {
				frequencyDomainPanel.repaint();
				
			}
		});
		frequencyDomainPanel.addLayer(new ZoomMouseListenerLayer());
		frequencyDomainPanel.addLayer(new DragMouseListenerLayer(cs));
		frequencyDomainPanel.addLayer(new BackgroundLayer(cs));
		frequencyDomainPanel.addLayer(new Layer(){

			@Override
			public void draw(Graphics2D graphics) {
				Map<Float, float[]> magnitudesSubMap = magnitudes.subMap(
						cs.getMin(Axis.X) / 1000.0f, cs.getMax(Axis.X) / 1000.0f);
				
				float frameDurationInMS = Config.getInt(Key.IFFT_STEP_SIZE)/  ((float) Config.getInt(Key.IFFT_SAMPLE_RATE)) * 1000.f;
				float frameOffsetInMS = frameDurationInMS/2.0f;
				
				for (Map.Entry<Float, float[]> frameEntry : magnitudesSubMap.entrySet()) {
					double timeStart = frameEntry.getKey();// in seconds
					float[] magnitudes = frameEntry.getValue();
				
					// draw the pixels
					for (int i = 0; i < magnitudes.length; i++) {
						Color color = Color.black;
						
						//actual energy at frame.frequencyEstimates[i];
						
						float centsStartingPoint = binStartingPointsInCents[i];
						// only draw the visible frequency range
						if (centsStartingPoint >= cs.getMin(Axis.Y)
								&& centsStartingPoint <= cs.getMax(Axis.Y)) {
						
							int greyValue = 255 - (int) (magnitudes[i]* 255);
							greyValue = Math.max(0, greyValue);
							greyValue = Math.min(255, greyValue);
							color = new Color(greyValue, greyValue, greyValue);
							graphics.setColor(color);
							graphics.fillRect((int) Math.round(timeStart * 1000),
									Math.round(centsStartingPoint),
									(int) Math.round(frameDurationInMS),
									(int) Math.ceil(binHeightsInCents[i]));
						}
					}
				}
				
				for(IFFTEventPoint point : referenceEventPoints){
					int timeInMs = (int) (point.t * frameDurationInMS + frameOffsetInMS);
					graphics.setColor(Color.RED);
					if(timeInMs > cs.getMin(Axis.X) && timeInMs <  cs.getMax(Axis.X)){
						float cents = binStartingPointsInCents[point.f] + binHeightsInCents[point.f]/2.0f;
						float timeDiameter = LayerUtilities.pixelsToUnits(graphics, 10, true);
						float frequencyDiameter = LayerUtilities.pixelsToUnits(graphics, 10, false);
						
						graphics.drawOval(Math.round(timeInMs-timeDiameter/2.0f) , Math.round(cents - frequencyDiameter/2.0f), Math.round(timeDiameter), Math.round(frequencyDiameter));
					}
				}
				
				for(IFFTEventPoint point : otherEventPoints){
					int timeInMs = (int) (point.t * frameDurationInMS + frameOffsetInMS);
					graphics.setColor(Color.BLUE);
					if(timeInMs > cs.getMin(Axis.X) && timeInMs <  cs.getMax(Axis.X)){
						float cents = binStartingPointsInCents[point.f] + binHeightsInCents[point.f]/2.0f;
						float timeDiameter = LayerUtilities.pixelsToUnits(graphics, 10, true);
						float frequencyDiameter = LayerUtilities.pixelsToUnits(graphics, 10, false);
						
						graphics.drawOval(Math.round(timeInMs-timeDiameter/2.0f) , Math.round(cents - frequencyDiameter/2.0f), Math.round(timeDiameter), Math.round(frequencyDiameter));
					}
				}
				
				for(IFFTEventPoint point : matchingEventPoints){
					int timeInMs = (int) (point.t * frameDurationInMS + frameOffsetInMS);
					graphics.setColor(Color.GREEN);
					if(timeInMs > cs.getMin(Axis.X) && timeInMs <  cs.getMax(Axis.X)){
						float cents = binStartingPointsInCents[point.f] + binHeightsInCents[point.f]/2.0f;
						float timeDiameter = LayerUtilities.pixelsToUnits(graphics, 10, true);
						float frequencyDiameter = LayerUtilities.pixelsToUnits(graphics, 10, false);
						
						graphics.drawOval(Math.round(timeInMs-timeDiameter/2.0f) , Math.round(cents - frequencyDiameter/2.0f), Math.round(timeDiameter), Math.round(frequencyDiameter));
					}
				}	
				
				for(IFFTFingerprint print : referenceFingerprints){
					int timeInMsT1 = (int) (print.t1 * frameDurationInMS + frameOffsetInMS);
					int timeInMsT2 = (int) (print.t2 * frameDurationInMS + frameOffsetInMS);
					int timeInMsT3 = (int) (print.t3 * frameDurationInMS + frameOffsetInMS);
					
					graphics.setColor(Color.ORANGE);
					if(timeInMsT1 > cs.getMin(Axis.X) && timeInMsT1 <  cs.getMax(Axis.X)){
						float centsF1 = (float) PitchConverter.hertzToAbsoluteCent(print.f1);
						float centsF2 = (float) PitchConverter.hertzToAbsoluteCent(print.f2);	
						float centsF3 = (float) PitchConverter.hertzToAbsoluteCent(print.f3);	
						
						graphics.drawLine(Math.round(timeInMsT1), Math.round(centsF1), Math.round(timeInMsT2), Math.round(centsF2));						
						graphics.drawLine(Math.round(timeInMsT2), Math.round(centsF2), Math.round(timeInMsT3), Math.round(centsF3));
					}
				}	
				
				for(IFFTFingerprint print : matchingPrints){
					int timeInMsT1 = (int) (print.t1 * frameDurationInMS + frameOffsetInMS);
					int timeInMsT2 = (int) (print.t2 * frameDurationInMS + frameOffsetInMS);
					int timeInMsT3 = (int) (print.t3 * frameDurationInMS + frameOffsetInMS);
					
					graphics.setColor(Color.GREEN);
					if(timeInMsT1 > cs.getMin(Axis.X) && timeInMsT1 <  cs.getMax(Axis.X)){
						float centsF1 = (float) PitchConverter.hertzToAbsoluteCent(print.f1);
						float centsF2 = (float) PitchConverter.hertzToAbsoluteCent(print.f2);	
						float centsF3 = (float) PitchConverter.hertzToAbsoluteCent(print.f3);	
						
						graphics.drawLine(Math.round(timeInMsT1), Math.round(centsF1), Math.round(timeInMsT2), Math.round(centsF2));
						graphics.drawLine(Math.round(timeInMsT2), Math.round(centsF2), Math.round(timeInMsT3), Math.round(centsF3));
					}
				}	
				
			}

			@Override
			public String getName() {
				return "NCTEQ Layer";
			}});
		
		frequencyDomainPanel.addLayer(new FrequencyAxisLayer(cs));
		frequencyDomainPanel.addLayer(new TimeAxisLayer(cs));
		frequencyDomainPanel.addLayer(new SelectionLayer(cs));
		return frequencyDomainPanel;
	}

	public void addAudio(String audioFile) {
		if(magnitudes.isEmpty()){
			
			
			final StopWatch w = new StopWatch();
			w.start();
			
			
			int sampleRate = Config.getInt(Key.IFFT_SAMPLE_RATE);
			int size = Config.getInt(Key.IFFT_SIZE); 
			int overlap = size - Config.getInt(Key.IFFT_STEP_SIZE);
			
			final IFFTEventPointProcessor eventPointProcessor = new IFFTEventPointProcessor(size,overlap,sampleRate);
			
			final AudioDispatcher d = AudioDispatcherFactory.fromPipe(audioFile, sampleRate, size , overlap);
			d.addAudioProcessor(eventPointProcessor);
			d.addAudioProcessor(new AudioProcessor() {
				
				private float runningMaxMagnitude;
				private final TreeMap<Float,float[]> magnitudes = new TreeMap<Float,float[]> (); 
				
				@Override
				public boolean process(AudioEvent audioEvent) {
					float[] currentMagnitudes = eventPointProcessor.getMagnitudes().clone();
					log(currentMagnitudes);
					
					//for visualization purposes:
					//store the new max value or, decay the running max
					float currentMaxValue = max(currentMagnitudes);
					if(currentMaxValue > runningMaxMagnitude){
						runningMaxMagnitude = currentMaxValue;
					}else{
						runningMaxMagnitude = 0.9999f * runningMaxMagnitude;
					}
					normalize(currentMagnitudes);
					
					magnitudes.put((float)audioEvent.getTimeStamp(),currentMagnitudes);
					return true;
				}
				
				
				
				@Override
				public void processingFinished() {
					
					double duration = d.secondsProcessed();
					IFFTFingerprintBrowser.this.magnitudes = magnitudes;
					IFFTFingerprintBrowser.this.referenceEventPoints.addAll(eventPointProcessor.getEventPoints());
					IFFTFingerprintBrowser.this.referenceFingerprints.addAll(eventPointProcessor.getFingerprints());
					System.out.println("Extracted  " + referenceEventPoints.size() + " ( " + referenceEventPoints.size() /duration + " points/s ) event points in " + w.formattedToString() + " or " + duration/w.timePassed(TimeUnit.SECONDS) + " times realtime");
					System.out.println("Extracted  " + referenceFingerprints.size() + " ( " + referenceFingerprints.size() /duration + " prints/s ) fingerprints in " + w.formattedToString() + " or " + duration/w.timePassed(TimeUnit.SECONDS) + " times realtime");
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							IFFTFingerprintBrowser.this.repaint();
						}
					});
				}
				
				private void log(float[] magnitudes){
					for(int i = 0 ; i < magnitudes.length ; i ++){
						magnitudes[i] = (float) Math.log1p(magnitudes[i]);
					}
				}
				
				private float max(float[] magnitudes){
					float max = 0;
					for(int i = 0 ; i < magnitudes.length ;i++){
						if(magnitudes[i]!=0){
							max = Math.max(max, magnitudes[i]);
						}
					}
					return max;
				}
				
				/**
				 * Normalizes the magnitude values to a range of [0,1].
				 */
				private void normalize(float[] magnitudes){
					for(int i = 0 ; i < magnitudes.length ;i++){
						if(magnitudes[i]!=0){
							magnitudes[i] = magnitudes[i]/runningMaxMagnitude;
						}
					}
				}
				
			});

			
			new Thread(d).start();
		}else{
			
			final StopWatch w = new StopWatch();
			w.start();
			
			int sampleRate = Config.getInt(Key.IFFT_SAMPLE_RATE);
			int size = Config.getInt(Key.IFFT_SIZE); 
			int overlap = size - Config.getInt(Key.IFFT_STEP_SIZE);			
			final IFFTEventPointProcessor eventPointProcessor = new IFFTEventPointProcessor(size,overlap,sampleRate);			
			
			final AudioDispatcher d = AudioDispatcherFactory.fromPipe(audioFile, sampleRate, size , overlap);
			d.addAudioProcessor(eventPointProcessor);
			d.addAudioProcessor(new AudioProcessor() {
	
				@Override
				public boolean process(AudioEvent audioEvent) {
					return true;
				}
				
				@Override
				public void processingFinished() {
					
					IFFTFingerprintBrowser.this.otherEventPoints.clear();
					IFFTFingerprintBrowser.this.matchingEventPoints.clear();
					IFFTFingerprintBrowser.this.matchingPrints.clear();
					
				
					ArrayList<IFFTEventPoint> otherEventPoints =  new ArrayList<IFFTEventPoint>();
					ArrayList<IFFTEventPoint> matchingEventPoints =  new ArrayList<IFFTEventPoint>();
					Set<IFFTFingerprint> matchingPrints =  new HashSet<IFFTFingerprint>();
					
					otherEventPoints.addAll(eventPointProcessor.getEventPoints());
					
					IFFTFingerprintBrowser.this.otherEventPoints = otherEventPoints;
					
					int numberOfEqualEventPoints = 0;
					for(IFFTEventPoint other : otherEventPoints){
						for(IFFTEventPoint these : referenceEventPoints){
							if(other.t == these.t && other.f == these.f){
								matchingEventPoints.add(other);
								numberOfEqualEventPoints++;
							}
						}
					}					
					IFFTFingerprintBrowser.this.matchingEventPoints = matchingEventPoints;
					
					
					List<IFFTFingerprint> otherFingerprints = eventPointProcessor.getFingerprints();
					HashMap<Integer, Integer> counter = new HashMap<>();
					for(IFFTFingerprint otherPrint : otherFingerprints){
						for(IFFTFingerprint thisPrint : referenceFingerprints){
							if(thisPrint.hashCode()==otherPrint.hashCode()){
								matchingPrints.add(thisPrint);
								int timeDiff = thisPrint.t1-otherPrint.t1;
								if(!counter.containsKey(timeDiff)){
									counter.put(timeDiff, 0);
								}
								counter.put(timeDiff, counter.get(timeDiff)+1);
							}
						}
					}
					
					IFFTFingerprintBrowser.this.matchingPrints.addAll(matchingPrints);
					
					//IFFTFingerprintBrowser.this.magnitudes = magnitudes;
					//IFFTFingerprintBrowser.this.referenceEventPoints.addAll(eventPointProcessor.getEventPoints());
					//IFFTFingerprintBrowser.this.referenceFingerprints.addAll(eventPointProcessor.getFingerprints());
					
					double duration = d.secondsProcessed();
					
					System.out.println("Extracted  " + otherEventPoints.size() + " ( " + otherEventPoints.size() /duration + " points/s ) event points in " + w.formattedToString() + " or " + duration/w.timePassed(TimeUnit.SECONDS) + " times realtime");
					System.out.println("Extracted  " + otherFingerprints.size() + " ( " + otherFingerprints.size() /duration + " prints/s ) fingerprints in " + w.formattedToString() + " or " + duration/w.timePassed(TimeUnit.SECONDS) + " times realtime");
					
					System.out.println("Found " + numberOfEqualEventPoints + " matching event points, or " + numberOfEqualEventPoints/duration + " per second or " + numberOfEqualEventPoints/ ((float) Math.max(otherEventPoints.size(), referenceEventPoints.size())) + " % .");
					System.out.println("Found " + matchingPrints.size() + " matching fingerprints, or " + matchingPrints.size()/duration + " per second or " + matchingPrints.size()/ ((float) Math.max(otherFingerprints.size(), referenceFingerprints.size())) + " % .");
					
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							IFFTFingerprintBrowser.this.repaint();
						}
					});
				}
			});

			
			new Thread(d).start();
			
			
		}
	}	
	
	private JComponent createButtonPanel(){
		JPanel fileChooserPanel = new JPanel(new GridLayout(0,2));
		fileChooserPanel.setBorder(new TitledBorder("Actions"));
		
	   final JFileChooser fileChooser = new JFileChooser();
		
		final JButton chooseFileButton = new JButton("Open...");
		chooseFileButton.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent arg0) {
				int returnVal = fileChooser.showOpenDialog(IFFTFingerprintBrowser.this);
	            if (returnVal == JFileChooser.APPROVE_OPTION) {
	                File file = fileChooser.getSelectedFile();
	                String audioFile = file.getAbsolutePath();
	                setTitle("Fingerprints for: " + file.getName());
	                addAudio(audioFile);
	            } else {
	                //canceled
	            }
			}					
		});
		
		fileChooserPanel.add(chooseFileButton);
		
		
		return fileChooserPanel;
	}
	
	
	
}