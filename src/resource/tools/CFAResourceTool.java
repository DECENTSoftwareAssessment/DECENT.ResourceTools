package resource.tools;

import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EValidator;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EObjectValidator;

import CFA.CFAPackage;
import CFA.impl.CFAPackageImpl;
import emf.resource.tools.ResourceTool;

public class CFAResourceTool extends ResourceTool {
	
	public CFAResourceTool(){
		super(CFAResourceTool.class.getName());
		CFAPackageImpl.init();
		initializeValidator();
		injector = null;
	}
	
	@Override
	protected void initializeValidator(){
		super.initializeValidator();
		EObjectValidator validator = new EObjectValidator();
	    EValidator.Registry.INSTANCE.put(CFAPackage.eINSTANCE, validator);
	}
	
	public void process(String workspace) {
		String outputPath = workspace+"/model.cfa";
		String outputPathValidated = workspace+"/model.cfa";
		String extension = "decent";

		Resource fromXMI = loadResourceFromXMI(outputPath, extension);
//	    validateResource(fromXMI);
//		System.out.println("***"+((Model)fromXMI.getContents().get(0)).getAgentPool().getAgents().get(0).getActivities().get(15).getState().getValues().get(0).getOfAttribute().getName());
//		System.out.println("***"+((Model)fromXMI.getContents().get(0)).getAgentPool().getAgents().get(0).getActivities().get(15).getState().getValues().get(0).getName());

//		storeResourceContents(fromXMI.getContents(), outputPathValidated, extension);

	}
	
	public void initializeDB(String dbName) {
		EPackage[] epackages = new EPackage[] { CFAPackage.eINSTANCE };
		super.initializeDB(dbName, epackages);
	}
	
	

}
