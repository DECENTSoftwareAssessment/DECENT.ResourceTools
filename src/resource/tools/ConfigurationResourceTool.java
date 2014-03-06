package resource.tools;

import java.util.Collection;
import java.util.Map;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EStructuralFeature.Setting;
import org.eclipse.emf.ecore.EValidator;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EObjectValidator;
import org.eclipse.emf.ecore.util.EcoreUtil;

import emf.resource.tools.ResourceTool;
import Configuration.ConfigurationPackage;
import Configuration.impl.ConfigurationPackageImpl;

public class ConfigurationResourceTool extends ResourceTool {
	
	public ConfigurationResourceTool(){
		super(ConfigurationResourceTool.class.getName());
		ConfigurationPackageImpl.init();
		initializeValidator();
		injector = null;
	}
	
	@Override
	protected void initializeValidator(){
		super.initializeValidator();
		EObjectValidator validator = new EObjectValidator();
	    EValidator.Registry.INSTANCE.put(ConfigurationPackage.eINSTANCE, validator);
	}
	
	public void process(String workspace) {
		String outputPath = workspace+"/model.decent";
		String outputPathValidated = workspace+"/model.decent";
		String extension = "configuration";

		Resource fromXMI = loadResourceFromXMI(outputPath, extension);
//	    validateResource(fromXMI);

//		storeResourceContents(fromXMI.getContents(), outputPathValidated, extension);

	}
	
	public void initializeDB(String dbName) {
		EPackage[] epackages = new EPackage[] { ConfigurationPackage.eINSTANCE };
		super.initializeDB(dbName, epackages);
	}
	
	

}
