package resource.tools;

import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EValidator;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EObjectValidator;

import ARFFx.ARFFxPackage;
import ARFFx.impl.ARFFxPackageImpl;
import emf.resource.tools.ResourceTool;

public class ARFFxResourceTool extends ResourceTool {
	
	public ARFFxResourceTool(){
		super(ARFFxResourceTool.class.getName());
		ARFFxPackageImpl.init();
		initializeValidator();
		injector = null;
	}
	
	@Override
	protected void initializeValidator(){
		super.initializeValidator();
		EObjectValidator validator = new EObjectValidator();
	    EValidator.Registry.INSTANCE.put(ARFFxPackage.eINSTANCE, validator);
	}
	
	public void process(String workspace) {
		String outputPath = workspace+"/model.arffx";
		String outputPathValidated = workspace+"/model.arffx";
		String extension = "cfa";

		Resource fromXMI = loadResourceFromXMI(outputPath, extension);
//	    validateResource(fromXMI);
//		System.out.println("***"+((Model)fromXMI.getContents().get(0)).getAgentPool().getAgents().get(0).getActivities().get(15).getState().getValues().get(0).getOfAttribute().getName());
//		System.out.println("***"+((Model)fromXMI.getContents().get(0)).getAgentPool().getAgents().get(0).getActivities().get(15).getState().getValues().get(0).getName());

//		storeResourceContents(fromXMI.getContents(), outputPathValidated, extension);

	}
	
	public void initializeDB(String dbName) {
		EPackage[] epackages = new EPackage[] { ARFFxPackage.eINSTANCE };
		super.initializeDB(dbName, epackages);
	}
	
	

}
