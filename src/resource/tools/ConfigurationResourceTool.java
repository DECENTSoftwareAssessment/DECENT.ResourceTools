package resource.tools;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EValidator;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EObjectValidator;
import org.eclipse.emf.ecore.util.EcoreUtil;

import emf.resource.tools.ResourceTool;
import Configuration.ConfigurationPackage;
import Configuration.Setting;
import Configuration.impl.ConfigurationPackageImpl;

public class ConfigurationResourceTool extends ResourceTool {
	private HashMap<String,String> settings = new HashMap<>();

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
	
	public void process(String workspace, String filename) {
		String outputPath = workspace+"/"+filename;
		String outputPathValidated = workspace+"/"+filename;
		String extension = "configuration";

		Resource fromXMI = loadResourceFromXMI(outputPath, extension);
		Setting settingsContainer = (Setting) fromXMI.getContents().get(0);
		for (Setting s : settingsContainer.getSettings()) {
			getSettings().put(s.getName(), s.getValue());
		}
//	    validateResource(fromXMI);

//		storeResourceContents(fromXMI.getContents(), outputPathValidated, extension);

	}
	
	public void initializeDB(String dbName) {
		EPackage[] epackages = new EPackage[] { ConfigurationPackage.eINSTANCE };
		super.initializeDB(dbName, epackages);
	}

	public HashMap<String,String> getSettings() {
		return settings;
	}

	public void setSettings(HashMap<String,String> settings) {
		this.settings = settings;
	}
	
	

}
