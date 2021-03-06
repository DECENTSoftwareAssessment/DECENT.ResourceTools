package resource.tools;

import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EValidator;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EObjectValidator;

import DECENT.DECENTPackage;
import DECENT.impl.DECENTPackageImpl;
import DECENT.util.DECENTResourceFactoryImpl;
import emf.resource.tools.ResourceTool;

public class DECENTResourceTool extends ResourceTool {
	
	public DECENTResourceTool(){
		super(DECENTResourceTool.class.getName());
		DECENTPackageImpl.init();
		this.resourceFactory = new DECENTResourceFactoryImpl();
		initializeValidator();
		injector = null;
	}
	
	@Override
	protected void initializeValidator(){
		super.initializeValidator();
		EObjectValidator validator = new EObjectValidator();
	    EValidator.Registry.INSTANCE.put(DECENTPackage.eINSTANCE, validator);
	}
	
	public void process(String workspace) {
		String outputPath = workspace+"/model.decent";
		String outputPathValidated = workspace+"/model.decent";
		String extension = "decent";

		Resource fromXMI = loadResourceFromXMI(outputPath, extension);
//	    validateResource(fromXMI);
//		System.out.println("***"+((Model)fromXMI.getContents().get(0)).getAgentPool().getAgents().get(0).getActivities().get(15).getState().getValues().get(0).getOfAttribute().getName());
//		System.out.println("***"+((Model)fromXMI.getContents().get(0)).getAgentPool().getAgents().get(0).getActivities().get(15).getState().getValues().get(0).getName());

//		storeResourceContents(fromXMI.getContents(), outputPathValidated, extension);

	}
	
	public void initializeDB(String dbName) {
		EPackage[] epackages = new EPackage[] { DECENTPackage.eINSTANCE };
		super.initializeDB(dbName, epackages);
	}
	
	

}
