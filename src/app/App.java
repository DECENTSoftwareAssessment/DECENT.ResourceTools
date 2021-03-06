package app;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.xtext.EcoreUtil2;

import resource.tools.BZResourceTool;
import resource.tools.ConfigurationResourceTool;
import resource.tools.DAGResourceTool;
import resource.tools.DECENTResourceTool;
import resource.tools.DuDeResourceTool;
import resource.tools.FAMIXResourceTool;
import resource.tools.MGResourceTool;
import resource.tools.MGResourceTool.MODE;
import Configuration.Setting;
import app.MSETool.SPLIT_MODE;

public class App {
	private Properties properties = new Properties();

	public void loadProperties(String[] arguments) throws Exception {
		if (arguments.length < 1) {
			System.out.println("No configuration provided! Usage: <CONFIGURATION> [<STEPS>]");
			System.exit(0);
		}
		String propertiesFilename = arguments[0];
		System.out.println("INIT: Loading settings from "+propertiesFilename+"...");
		properties.load(new FileInputStream(propertiesFilename));
		if (arguments.length == 2) {
			properties.setProperty("steps", arguments[1]);
		}
	}
	public void executeSteps() throws Exception {
		String dataLocation = properties.getProperty("dataLocation");
		String project = properties.getProperty("project");
		String location = dataLocation+project;
		for (String step : properties.getProperty("steps").split(",")) {
			executeTranslation(step, location);
		}
	}	
	public void executeTranslation(String step, String location) throws Exception {
		ConfigurationResourceTool configurationTool = new ConfigurationResourceTool();
		String mgConfiguration = "";
		System.out.println("TRANSLATE: "+step+"...");
		switch (step) {
		case "DECENT2BIN":
			translateDECENT2BIN(location);
			break;
		case "DECENT2JSON":
			translateDECENT2JSON(location);
			break;
		case "JSON2DECENT":
			translateJSON2DECENT(location);
			break;
		case "MG2BIN":
			translateMG2BIN(location);
			break;
		case "BIN2DECENT":
			translateBIN2DECENT(location);
			break;
		case "MG":
			mgConfiguration = properties.getProperty("mgConfiguration");
			configurationTool.process(location, mgConfiguration );
			translateMG(configurationTool.getSettings(),location,MODE.NO_LINEBLAME);
			break;
		case "MGLite":
			mgConfiguration = properties.getProperty("mgConfiguration");
			configurationTool.process(location, mgConfiguration );
			translateMG(configurationTool.getSettings(),location,MODE.NO_LINEBLAME_CONTENT_PATCH);
			break;
		case "BZ":
			String bzConfiguration = properties.getProperty("bzConfiguration");
			configurationTool.process(location, bzConfiguration);
			translateBZ(configurationTool.getSettings(),location);
			break;
		case "FAMIX":
			if (checkForCompleteness(location+"/famix")) {
				boolean filterFamix = Boolean.parseBoolean(properties.getProperty("filterFamix"));
				translateFAMIXcomplete(location+"/famix", filterFamix);
			}
			break;
		case "DAG":
			String gitPath = properties.getProperty("gitPath");
			generateDAG(location, gitPath);
			translateDAG(location);
			break;
		case "DUDE":
			String dudeConfiguration = properties.getProperty("dudeConfiguration");
			configurationTool.process(location, dudeConfiguration );
			translateDuDe(configurationTool.getSettings(),location);
			break;
		default:
			System.out.println("ERROR: Unknown step "+step);
			break;
		}

	}
	public static void main(String[] args) throws Exception {
		App app = new App();
		app.loadProperties(args);
		app.executeSteps();
	}

	private boolean checkForCompleteness(String workspace) {
		boolean isComplete = true;
		File ws = new File(workspace);
		String[] commits = ws.list();
		Arrays.sort(commits, new Comparator<String>() {

			@Override
			public int compare(String o1, String o2) {
				if (o1.equals(o2)) {
					return 0;
				} else if (Integer.parseInt(o1)>Integer.parseInt(o2)){
					return 1;
				} else {
					return -1;
				}
			}
		});
		
		for (String c : commits) {
			File model = new File(workspace+"/"+c+"/model.mse");
			if (!model.exists()) {
				isComplete=false;
				System.out.println("Missing model for CommitID "+c);
			}
		}
		return isComplete;
	}
	
	private void translateFAMIXcomplete(String workspace, boolean filter) {
		File ws = new File(workspace);
		String[] commits = ws.list();
		Arrays.sort(commits, new Comparator<String>() {

			@Override
			public int compare(String o1, String o2) {
				if (o1.equals(o2)) {
					return 0;
				} else if (Integer.parseInt(o1)>Integer.parseInt(o2)){
					return 1;
				} else {
					return -1;
				}
			}
		});
		String project = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
				+ "<projectDescription>" 
				+ "<name>FAMIX</name>"
				+ "<comment></comment>" 
				+ "<projects></projects>"
				+ "<buildSpec></buildSpec>" 
				+ "<natures></natures>"
				+ "</projectDescription>";

		for (String c : commits) {
//			if (c.equals("3")) {
//				System.out.println("Processing: "+c);
				String suffix = "";
				if (filter) {
					suffix = "filtered";
					filterMSE(workspace+"/"+c, suffix);
					suffix = "/"+suffix;
				}
				File projectFile = new File(workspace+"/"+c+suffix + "/.project");
				try {
					FileUtils.writeStringToFile(projectFile, project);
				} catch (IOException e) {
					e.printStackTrace();
				}
				FAMIXResourceTool famixTool = new FAMIXResourceTool();
				famixTool.process(workspace+"/"+c+suffix, Integer.parseInt(c));
				FileUtils.deleteQuietly(projectFile);
				
//			}
		}
		
//		for (int i = 1; i<=2; i++) {
//			int commitId = i;
//			workspace = "/home/philip-iii/TEMP/fmx/famix/"+commitId;
//
//			FAMIXResourceTool famixTool = new FAMIXResourceTool();
//			famixTool.process(workspace, commitId);
//		}
	}

	private void translateFAMIX(String workspace) {
		FAMIXResourceTool famixTool = new FAMIXResourceTool();
		famixTool.process(workspace, 1);
	}
	private void translateMG2BIN(String workspace) {
		MGResourceTool tool = new MGResourceTool();
		Resource resource = tool.loadResourceFromXMI(workspace+"/model.mg","mg");
		tool.storeBinaryResourceContents(resource.getContents(), workspace+"/model.mg"+"bin", "mgbin");
	}
	private void translateDECENT2BIN(String workspace) {
		DECENTResourceTool tool = new DECENTResourceTool();
		Resource resource = tool.loadResourceFromXMI(workspace+"/model.decent","decent");
		tool.storeBinaryResourceContents(resource.getContents(), workspace+"/model.decent"+"bin", "decentbin");
	}
	private void translateDECENT2JSON(String workspace) {
		DECENTResourceTool tool = new DECENTResourceTool();
		Resource resource = tool.loadResourceFromXMI(workspace+"/model.decent","decent");
		tool.storeResourceInJSON(resource.getContents(), workspace+"/model.decent"+"json", "decentjson");
	}
	private void translateJSON2DECENT(String workspace) {
		DECENTResourceTool tool = new DECENTResourceTool();
		Resource resource = tool.loadResourceFromJSON(workspace+"/model.decentjson","decentjson");
		tool.storeResourceContents(resource.getContents(), workspace+"/model.decent", "decent");
	}
	private void translateBIN2DECENT(String workspace) {
		DECENTResourceTool tool = new DECENTResourceTool();
		Resource resource = tool.loadResourceFromBinary(workspace+"/model.decentbin","decentbin");
		tool.storeResourceContents(resource.getContents(), workspace+"/model.decent", "decent");
	}

	private void translateDAG(String workspace) {
		DAGResourceTool dagTool = new DAGResourceTool();
		dagTool.process(workspace);
	}

	private void generateDAG(String workspace, String gitPath) {
		List<String> command = new ArrayList<String>();
		String sshURI = properties.getProperty("sshURI");
		String sshPort = properties.getProperty("sshPort");
		boolean useSSH = Boolean.parseBoolean(properties.getProperty("useSSH"));
		if (useSSH ) {
			command.add("ssh");
			command.add(sshURI);
			command.add("-p");
			command.add(sshPort);
		}
		command.add("git");
		command.add("--git-dir="+gitPath+"");
		command.add("log");
		command.add("--topo-order");
		command.add("--pretty=format:%H-%P");
		command.add("--parents");
		command.add("-M");
		command.add("-C");
		command.add("--cc");
		command.add("--decorate=full");
		command.add("--all");
		System.out.println(executeCommand(command, workspace+"/model.dagx"));;
	}

	private String executeCommand(List<String> command, String outputFilename) {

		StringBuffer output = new StringBuffer();

		try {
			ProcessBuilder pb = new ProcessBuilder(command);
			File outputFile = new File(outputFilename);
			if (outputFile.exists()) {
				outputFile.delete();
			}

			File outputFileTMP = new File(outputFilename+".tmp");
			outputFileTMP.getParentFile().mkdirs();
			pb.redirectOutput(outputFileTMP);
			Process p = pb.start();
			p.waitFor();
			BufferedReader outputReader = new BufferedReader(new InputStreamReader(
					p.getInputStream()));
			BufferedReader errorReader = new BufferedReader(new InputStreamReader(
					p.getErrorStream()));

			String line = "";
			while ((line = outputReader.readLine()) != null) {
				output.append(line + "\n");
			}
			while ((line = errorReader.readLine()) != null) {
				System.err.println(line);
			}
			outputReader.close();
			errorReader.close();
			p.destroy();
			
			FileReader fr = new FileReader(outputFileTMP);
			FileWriter fw = new FileWriter(outputFilename, true);
			String s;
		    BufferedReader br = new BufferedReader(fr);
		    while ((s = br.readLine()) != null) {
		    	String l = s.replaceAll("-", " ");
			    fw.append(l+"\n");
		    }
		    br.close();
		    fw.flush();
			fw.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return output.toString();

	} 

	private void translateDuDe(HashMap<String,String> settings,String workspace) {
		
		DuDeResourceTool dudeTool = new DuDeResourceTool();
		dudeTool.setDbUser(settings.get("dbUser"));
		dudeTool.setDbPass(settings.get("dbPass"));
		dudeTool.setDbServer(settings.get("dbServer"));
		dudeTool.setDbPort(settings.get("dbPort"));

		dudeTool.process(workspace,settings.get("dbName"), DuDeResourceTool.MODE.COMPLETE);
	}

	
	private void translateBZ(HashMap<String,String> settings, String workspace) {
		BZResourceTool bzTool = new BZResourceTool();
		bzTool.setDbUser(settings.get("dbUser"));
		bzTool.setDbPass(settings.get("dbPass"));
		bzTool.setDbServer(settings.get("dbServer"));
		bzTool.setDbPort(settings.get("dbPort"));
		bzTool.process(workspace,settings.get("dbName"));
	}

	private void translateMG(HashMap<String,String> settings, String workspace, MODE mode) {
		MGResourceTool mgTool = new MGResourceTool();
		mgTool.setDbUser(settings.get("dbUser"));
		mgTool.setDbPass(settings.get("dbPass"));
		mgTool.setDbServer(settings.get("dbServer"));
		mgTool.setDbPort(settings.get("dbPort"));
		mgTool.process(workspace,settings.get("dbName"),mode);
	}

	private void translateSplitMSE(String workspace, int size) {
		for (int i=0; i<size; i++) { 
			FAMIXResourceTool famixTool = new FAMIXResourceTool();
			famixTool.process(workspace+"/"+i, 1);
		}
	}

	private void filterMSE(String workspace, String suffix) {
		String[] filteredEntities = properties.getProperty("filteredElements").split(",");
		//String[] filteredEntities = new String[]{"Method","FileAnchor","Function","Class","Module","Package","NameSpace"};
		MSETool mseTool = new MSETool();
		mseTool.readMSE(workspace+"/model.mse");
		mseTool.filterMSE(filteredEntities, suffix);
	}

	private void splitMSE(String workspace, int size) {
		MSETool splitter = new MSETool();
		splitter.readMSE(workspace+"/model.mse");
		splitter.splitMSEi(size, SPLIT_MODE.COUNT);
	}

}
