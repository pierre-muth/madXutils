package madXSimpleChart;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.SwingUtilities;

import cern.jdve.Chart;
import cern.jdve.ChartInteractor;
import cern.jdve.Style;
import cern.jdve.data.DefaultDataSet;
import cern.jdve.data.DefaultDataSource;
import cern.jdve.renderer.AreaChartRenderer;


public class TwissFileChart extends JPanel implements ActionListener {
	private Chart chartTime;
	private JMenuBar menuBar;
	private DefaultDataSource dataSource = new DefaultDataSource();
	private JFileChooser fileChooser;
	private JPanel dataSetVisibleOptionList;
	private HashMap<String, DefaultDataSet> dataSetMap;
	
	private static final String OPEN_COMMAND = "open";
	
	public TwissFileChart(){
		buildGUI();
	}
	
	private void readFile(File file) throws IOException{
		BufferedReader br = new BufferedReader( new FileReader(file) );
		String rawLine;
		ArrayList<MadElement> madElements = new ArrayList<>();
		
		rawLine = br.readLine();
		while(rawLine != null){
			
			if (rawLine.startsWith(" \"")){		// we skip the header 
				String[] lineParts = rawLine.split(" ");
				int index = 0;
				MadElement madElement = new MadElement();
				for (int i = 0; i < lineParts.length; i++) {
					if (lineParts[i].length() > 0){		// we skip the spaces between values
						if (index == 1) {
							madElement.type = lineParts[i].replaceAll("\"", "");
						}
						if (index == 2) {
							madElement.s = Double.parseDouble( lineParts[i] );
						}
						if (index == 3) {
							madElement.l = Double.parseDouble( lineParts[i] );
							break;
						}
						index++;
					}
				}
				madElements.add(madElement);
			}
			rawLine = br.readLine();
		}
		
		br.close();
		
		if (madElements.isEmpty()) return;
		
		// clear the dataSource in case we opened another file
		for (int i = 0; i < dataSource.getDataSetsCount(); i++) {
			dataSource.removeDataSet(i);
		}
		
		// make the dataSets
		ArrayList<String> typesIndexes = new ArrayList<>();
		dataSetMap = new HashMap<>();
		for (MadElement madElement : madElements) {
			System.out.println( madElement.type +": s="+madElement.s+" l="+madElement.l);
			
			if (!dataSetMap.containsKey(madElement.type) ){	// a new type of element
				typesIndexes.add(madElement.type);
				DefaultDataSet ds = new DefaultDataSet((typesIndexes.indexOf(madElement.type)+1)+"-"+madElement.type);
				dataSetMap.put(madElement.type, ds);

				JRadioButton dsVisibleOption = new JRadioButton(madElement.type);	// the ratio buttons
				dsVisibleOption.setSelected(true);
				dsVisibleOption.setActionCommand(madElement.type);
				dsVisibleOption.addActionListener(TwissFileChart.this);
				dataSetVisibleOptionList.add(dsVisibleOption);
			}

			// get the correstonding type dataset and add a square shape
			DefaultDataSet ds = dataSetMap.get(madElement.type);
			ds.add(madElement.s - madElement.l, 0);
			ds.add(madElement.s - madElement.l, 1+typesIndexes.indexOf(madElement.type));
			ds.add(madElement.s, 1+typesIndexes.indexOf(madElement.type));
			ds.add(madElement.s, 0);
		}
		
		// add all dataSets to dataSource
		dataSource.setDataSets(dataSetMap.values().toArray(new DefaultDataSet[]{}));
		
		try {
			chartTime.addInteractor(ChartInteractor.DATA_PICKER);
		} catch (IllegalArgumentException e) {
			// in case we allready added it (reopen a file)
		}
	
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				TwissFileChart.this.repaint(); // refresh the ratio button panel
			}
		});
		
	}
	
	
	private void buildGUI() {
		setLayout(new BorderLayout());
		setBackground(Color.WHITE);

		chartTime = new Chart();
		
		chartTime.setPreferredSize(new Dimension(800, 300));
		chartTime.addInteractor(ChartInteractor.ZOOM);
		chartTime.setLegendVisible(true);
		chartTime.setAntiAliasingText(true);
		chartTime.setAntiAliasing(true);
		chartTime.getLegend().setBackground(new Color(200, 200, 200, 150));

		AreaChartRenderer renderer = new AreaChartRenderer();

		Style styleBlack = new Style(new BasicStroke(1.0f), new Color(0, 0, 0, 255), new Color(0, 0, 0, 255));
		renderer.setStyle(0, styleBlack);

		renderer.setDataSource(dataSource);

		chartTime.addRenderer(0, renderer);
				
		add(chartTime, BorderLayout.CENTER);
		
		menuBar = new JMenuBar();
		JMenu fileMenu = new JMenu("File");
		menuBar.add(fileMenu);
		JMenuItem openItem = new JMenuItem("Open");
		fileMenu.add(openItem);
		openItem.addActionListener(this);
		openItem.setActionCommand(OPEN_COMMAND);
		
		add(menuBar, BorderLayout.NORTH);
		
		fileChooser = new JFileChooser();
		fileChooser.addActionListener(this);
		
		dataSetVisibleOptionList = new JPanel(new FlowLayout(FlowLayout.LEFT));
		dataSetVisibleOptionList.setPreferredSize(new Dimension(140, 300));
		add(dataSetVisibleOptionList, BorderLayout.EAST);
		
	}

	public static void main(String[] args) {

		JFrame frame = new JFrame("MadX Simple Chart");
		frame.setLayout(new BorderLayout());
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		TwissFileChart main = new TwissFileChart();
		frame.add(main, BorderLayout.CENTER );
		frame.setBackground(Color.WHITE);
		frame.pack();
		frame.setVisible(true);

		
	}


	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals(OPEN_COMMAND)){
			fileChooser.showOpenDialog(this);
		}
		
		if (e.getActionCommand().equals(JFileChooser.APPROVE_SELECTION)){
			try {
				readFile( fileChooser.getSelectedFile() );
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
		
		if ( dataSetMap.containsKey(e.getActionCommand()) ){
			boolean visible = dataSetMap.get(e.getActionCommand()).isVisible();
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
			dataSetMap.get(e.getActionCommand()).setVisible(!visible);
				}
			});
		}
	}
	
	private class MadElement {
		public double s;
		public double l;
		public String type;
	}


}
