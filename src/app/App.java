package app;

import resource.tools.DAGResourceTool;
import resource.tools.DECENTResourceTool;
import resource.tools.FAMIXResourceTool;
import resource.tools.MGResourceTool;

public class App {

	public static void main(String[] args) {
		String workspace = "";

		for (int i = 1; i<=50; i++) {
			int commitId = i;
			workspace = "/home/philip-iii/TEMP/fmx/famix/"+commitId;

			FAMIXResourceTool famixTool = new FAMIXResourceTool();
			famixTool.process(workspace, commitId);
		}
		
		workspace = "/home/philip-iii/TEMP/fmx/dag";
//		DAGResourceTool dagTool = new DAGResourceTool();
//		dagTool.process(workspace);

		workspace = "/home/philip-iii/TEMP/fmx";
//		workspace = "/home/philip-iii/Dev/workspaces/emf/DECENT.Transformations/input";
		
		String dbName = "MGGitWS";
		dbName = "mg_rekonq";
//		MGResourceTool mgTool = new MGResourceTool();
//		mgTool.process(workspace,dbName);
		
		workspace = "/home/philip-iii/Dev/workspaces/emf/DECENT.Transformations/output";
//		DECENTResourceTool decentTool = new DECENTResourceTool();
//		decentTool.process(workspace);
		
		
	}

}
