package app;

import java.io.File;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;

import org.apache.commons.io.FileUtils;

import resource.tools.BZResourceTool;
import resource.tools.DAGResourceTool;
import resource.tools.DECENTResourceTool;
import resource.tools.FAMIXResourceTool;
import resource.tools.MGResourceTool;

public class App {

	public static void main(String[] args) {
		String workspace = "";
		String dbName = "";

		
//		workspace = "/home/philip-iii/TEMP/fmx/famix/";
//		workspace = args[0];
//		File ws = new File(workspace);
//		String[] commits = ws.list();
//		Arrays.sort(commits, new Comparator<String>() {
//
//			@Override
//			public int compare(String o1, String o2) {
//				if (o1.equals(o2)) {
//					return 0;
//				} else if (Integer.parseInt(o1)>Integer.parseInt(o2)){
//					return 1;
//				} else {
//					return -1;
//				}
//			}
//		});
//		
//		for (String c : commits) {
//			if (c.equals("3")) {
//			System.out.println("Processing: "+c);
//			FAMIXResourceTool famixTool = new FAMIXResourceTool();
//			famixTool.process(workspace+c, Integer.parseInt(c));
//			}
//		}
		
//		for (int i = 1; i<=2; i++) {
//			int commitId = i;
//			workspace = "/home/philip-iii/TEMP/fmx/famix/"+commitId;
//
//			FAMIXResourceTool famixTool = new FAMIXResourceTool();
//			famixTool.process(workspace, commitId);
//		}
		
//		workspace = "/home/philip-iii/TEMP/fmx/dag";
//		DAGResourceTool dagTool = new DAGResourceTool();
//		dagTool.process(workspace);

		workspace = "/home/philip-iii/Dev/workspaces/emf/DECENT.Transformations/input";
		dbName = "bz_x";
		//dbName = "mg_cfa";
		BZResourceTool bzTool = new BZResourceTool();
		bzTool.process(workspace,dbName);
		
		
		System.exit(0);
		
		workspace = "/home/philip-iii/TEMP/fmx";
//		workspace = "/home/philip-iii/Dev/workspaces/emf/DECENT.Transformations/input";
		
		workspace = "/home/philip-iii/Dev/workspaces/emf/DECENT.Transformations/input";
		dbName = "MGGitWS";
		//dbName = "mg_cfa";
		MGResourceTool mgTool = new MGResourceTool();
		mgTool.process(workspace,dbName);
		
		workspace = "/home/philip-iii/Dev/workspaces/emf/DECENT.Transformations/output";
//		DECENTResourceTool decentTool = new DECENTResourceTool();
//		decentTool.process(workspace);
		
		
	}

}
