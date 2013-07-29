package app;

import resource.tools.FAMIXResourceTool;
import resource.tools.MGResourceTool;

public class App {

	public static void main(String[] args) {
		String workspace = "/home/philip-iii/TEMP/fmx/famix/1";
		int commitId = -1;

		FAMIXResourceTool famixTool = new FAMIXResourceTool();
		famixTool.process(workspace, commitId);
		
		workspace = "/home/philip-iii/TEMP/fmx";
		MGResourceTool mgTool = new MGResourceTool();
		mgTool.process(workspace);
	}

}
