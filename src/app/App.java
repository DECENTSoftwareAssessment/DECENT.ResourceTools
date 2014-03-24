package app;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.xtext.EcoreUtil2;

import resource.tools.BZResourceTool;
import resource.tools.ConfigurationResourceTool;
import resource.tools.DAGResourceTool;
import resource.tools.FAMIXResourceTool;
import resource.tools.MGResourceTool;
import resource.tools.MGResourceTool.MODE;
import Configuration.Setting;
import app.MSETool.SPLIT_MODE;

public class App {

	
	public static void main(String[] args) {
		String workspace = "";
		String ws = "/home/philip-iii/Dev/workspaces/emf/DECENT.Transformations/input/";
		//ws = "./";
		
		ConfigurationResourceTool configurationTool = new ConfigurationResourceTool();
		configurationTool.process(ws,args[0]);
		HashMap<String, String> settings = configurationTool.getSettings();
		translateBZ(settings,"/home/philip-iii/Dev/workspaces/emf/DECENT.Data/input/yakuake");
		//translateMG(settings,ws,MODE.NO_LINEBLAME_CONTENT_PATCH);

		System.exit(0);

		
		String gitPath = "/Users/philip-iii/Dev/workspaces/emf/DECENT.ResourceTools/.git";
		
		gitPath = "/home/msr/Dev/Resources/git/copies/.git";
		workspace = "/Users/philip-iii/Dev/workspaces/emf/DECENT.Transformations/input/dag";
		generateDAG(workspace, gitPath);
		translateDAG(workspace);

		//translateMG("172.16.179.143","3306","mg_copies","/Users/philip-iii/Dev/workspaces/emf/DECENT.Transformations/input/copies", MODE.NO_LINEBLAME_CONTENT);

		System.exit(0);
		
		
		int size = 20;
		splitMSE(workspace, size);

		translateSplitMSE(workspace, size);
				
		filterMSE(workspace);

		//TODO: figure out cross-file linking as at the moment this results in  a ton of junk!
		translateFAMIX("/Users/philip-iii/Dev/workspaces/emf/DECENT.Transformations/input/fmx/mahr-sample-o/filtered/");
		
		translateDAG("/Users/philip-iii/Dev/workspaces/emf/DECENT.Transformations/input/dag");

		//translateBZ("localhost","3306","bz_x","/home/philip-iii/Dev/workspaces/emf/DECENT.Transformations/input/bz/");
		
		
		workspace = "/Users/philip-iii/Dev/workspaces/emf/DECENT.Transformations/output";
//		DECENTResourceTool decentTool = new DECENTResourceTool();
//		decentTool.process(workspace);
		
		translateFAMIXcomplete(args[0]);
		
	}

	private static void translateFAMIXcomplete(String workspace) {
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
//			if (c.equals("3")) {
//				System.out.println("Processing: "+c);
				FAMIXResourceTool famixTool = new FAMIXResourceTool();
				famixTool.process(workspace+c, Integer.parseInt(c));
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

	private static void translateFAMIX(String workspace) {
		FAMIXResourceTool famixTool = new FAMIXResourceTool();
		famixTool.process(workspace, 1);
	}

	private static void translateDAG(String workspace) {
		DAGResourceTool dagTool = new DAGResourceTool();
		dagTool.process(workspace);
	}

	private static void generateDAG(String workspace, String gitPath) {
		List<String> command = new ArrayList<String>();

		command.add("ssh");
		command.add("msr@172.16.179.143");
		
		command.add("git");
		command.add("--git-dir="+gitPath+"");
		command.add("log");
		command.add("--topo-order");
		command.add("--pretty=format:%H\\ %P");
		command.add("--parents");
		command.add("-M");
		command.add("-C");
		command.add("--cc");
		command.add("--decorate=full");
		command.add("--all");
		System.out.println(executeCommand(command, workspace+"/model.dagx"));;
	}

	private static String executeCommand(List<String> command, String outputFilename) {

		StringBuffer output = new StringBuffer();

		try {
			ProcessBuilder pb = new ProcessBuilder(command);
			File outputFile = new File(outputFilename);
			outputFile.getParentFile().mkdirs();
			pb.redirectOutput(outputFile);
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
			FileWriter fw = new FileWriter(outputFilename, true);
			fw.append("\n");
			fw.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return output.toString();

	} 
	
	private static void translateBZ(HashMap<String,String> settings, String workspace) {
		BZResourceTool bzTool = new BZResourceTool();
		bzTool.setDbUser(settings.get("dbUser"));
		bzTool.setDbPass(settings.get("dbPass"));
		bzTool.setDbServer(settings.get("dbServer"));
		bzTool.setDbPort(settings.get("dbPort"));
		bzTool.process(workspace,settings.get("BZdbName"));
	}

	private static void translateMG(HashMap<String,String> settings, String workspace, MODE mode) {
		MGResourceTool mgTool = new MGResourceTool();
		mgTool.setDbUser(settings.get("dbUser"));
		mgTool.setDbPass(settings.get("dbPass"));
		mgTool.setDbServer(settings.get("dbServer"));
		mgTool.setDbPort(settings.get("dbPort"));
		mgTool.process(workspace,settings.get("dbName"),mode);
	}

	private static void translateSplitMSE(String workspace, int size) {
		for (int i=0; i<size; i++) { 
			FAMIXResourceTool famixTool = new FAMIXResourceTool();
			famixTool.process(workspace+"/"+i, 1);
		}
	}

	private static void filterMSE(String workspace) {
		String[] filteredEntities = new String[]{"Method","FileAnchor","Function","Class","Module","Package","NameSpace"};
		MSETool mseTool = new MSETool();
		mseTool.readMSE(workspace+"/model.mse");
		mseTool.filterMSE(filteredEntities);
	}

	private static void splitMSE(String workspace, int size) {
		MSETool splitter = new MSETool();
		splitter.readMSE(workspace+"/model.mse");
		splitter.splitMSEi(size, SPLIT_MODE.COUNT);
	}

}
