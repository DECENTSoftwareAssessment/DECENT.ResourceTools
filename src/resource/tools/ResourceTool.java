package resource.tools;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.eclipse.emf.common.util.BasicDiagnostic;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EOperation;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EValidator;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.Diagnostician;
import org.eclipse.emf.ecore.util.EObjectValidator;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceImpl;
import org.eclipse.emf.teneo.PersistenceOptions;
import org.eclipse.emf.teneo.hibernate.HbDataStore;
import org.eclipse.emf.teneo.hibernate.HbHelper;
import org.eclipse.emf.teneo.hibernate.resource.HibernateResource;
import org.eclipse.ocl.common.OCLConstants;
import org.eclipse.ocl.ecore.delegate.OCLInvocationDelegateFactory;
import org.eclipse.ocl.ecore.delegate.OCLSettingDelegateFactory;
import org.eclipse.ocl.ecore.delegate.OCLValidationDelegateFactory;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.resource.XtextResourceSet;
import org.hibernate.cfg.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.impl.SimpleLogger;

import AbstractDECENTProvider.AbstractDECENTProviderPackage;
import FAMIX.FAMIXPackage;

import com.google.inject.Injector;

import famix.M3xStandaloneSetup;

public class ResourceTool {

	protected static Logger log;
	protected String dbServer = "localhost";
	protected String dbUser = "cvsanaly";
	protected String dbPass = "cvsanaly";
	protected String dbPort = "3306";
	protected Injector injector;

	public ResourceTool() {
		System.setProperty(Logger.class.getName(),SimpleLogger.class.getName());
		System.setProperty("org.slf4j.simpleLogger.logFile","validation.log");
		System.setProperty("org.slf4j.simpleLogger.logFile","System.out");
		log = LoggerFactory.getLogger(ResourceTool.class);	
	}

	public Resource loadResourceFromXtext(String workspace, String pathName, boolean resolveAll) {
		// "workspace" is a string that contains the path to the workspace containing the DSL program.
		new org.eclipse.emf.mwe.utils.StandaloneSetup().setPlatformUri(workspace);
		
		XtextResourceSet resourceSet = injector.getInstance(XtextResourceSet.class);

		//TODO:why is this not needed for FAMIX but needed for DAG?
		if (resolveAll) {
			resourceSet.addLoadOption(XtextResource.OPTION_RESOLVE_ALL, Boolean.TRUE);
		}

		Resource resource = resourceSet.getResource(URI.createURI(pathName), true);
		for (org.eclipse.emf.ecore.resource.Resource.Diagnostic diagnostic : resource.getErrors()) {
			log.warn("xtext: "+diagnostic.getLine() +" :"+diagnostic.getMessage());
		}
		return resource;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Resource loadResourceFromXMI(String inputPath, String extension) {
	    Resource.Factory.Registry reg = Resource.Factory.Registry.INSTANCE;
	    Map<String, Object> m = reg.getExtensionToFactoryMap();
		m.put(extension, new XMIResourceFactoryImpl());
	    ResourceSet resSetIn = new ResourceSetImpl();
	    Resource inputResource = resSetIn.createResource(URI.createURI(inputPath));
	    try {
	    	Map options = new HashMap<>();
	    	options.put(XMIResourceImpl.OPTION_DEFER_IDREF_RESOLUTION, Boolean.TRUE);
//	    	options.put(XMIResourceImpl.OPTION_PROCESS_DANGLING_HREF, XMIResourceImpl.OPTION_PROCESS_DANGLING_HREF_DISCARD);
			inputResource.load(options);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return inputResource;
	}

	protected void initializeValidator() {
	//		OCL.initialize(null);
			String oclDelegateURI = OCLConstants.OCL_DELEGATE_URI+"/Pivot";
			
		    EOperation.Internal.InvocationDelegate.Factory.Registry.INSTANCE.put(oclDelegateURI,
		        new OCLInvocationDelegateFactory(oclDelegateURI));
		    EStructuralFeature.Internal.SettingDelegate.Factory.Registry.INSTANCE.put(oclDelegateURI,
		        new OCLSettingDelegateFactory(oclDelegateURI));
		    EValidator.ValidationDelegate.Registry.INSTANCE.put(oclDelegateURI,
		        new OCLValidationDelegateFactory(oclDelegateURI));
		    
	//	    EStructuralFeature.Internal.SettingDelegate.Factory.Registry.INSTANCE.put(oclDelegateURI, 
	//	    	new OCLSettingDelegateFactory.Global());
	//	    QueryDelegate.Factory.Registry.INSTANCE.put(oclDelegateURI, new OCLQueryDelegateFactory.Global());
		    
		}

	public void validateResource(Resource resource) {
	    BasicDiagnostic diagnostics = new BasicDiagnostic();
	    boolean valid = true;
	    for (EObject eo : resource.getContents())
	    {
	    	Map<Object, Object> context = new HashMap<Object, Object>();
	    	boolean validationResult = Diagnostician.INSTANCE.validate(eo, diagnostics, context);
	    	showDiagnostics(diagnostics, "");
			valid &= validationResult;
	    }
	    
	    if (!valid){
	    	System.out.println("Problem with validation!");
	    }
	}

	protected void showDiagnostics(Diagnostic diagnostics, String indent) {
		indent+="  ";
		for (Diagnostic d : diagnostics.getChildren()){
			log.warn(indent+d.getSource());
			log.warn(indent+"  "+d.getMessage());
			showDiagnostics(d,indent);
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected void storeResourceContents(EList<EObject> contents, String outputPath, String extension) {
		//TODO: duplicated from loadResourceFromXMI => move to a more appropriate location
		Resource.Factory.Registry reg = Resource.Factory.Registry.INSTANCE;
	    Map<String, Object> m = reg.getExtensionToFactoryMap();
		m.put(extension, new XMIResourceFactoryImpl());
		
	    ResourceSet resSet = new ResourceSetImpl();
		Resource outputResource = resSet.createResource(URI.createURI(outputPath));
	    outputResource.getContents().addAll(contents);
	    try {
	      Map options = new HashMap<>();
	      options.put(XMIResourceImpl.OPTION_ENCODING, "UTF-8");
//	      options.put(XMIResourceImpl.OPTION_PROCESS_DANGLING_HREF, XMIResourceImpl.OPTION_PROCESS_DANGLING_HREF_DISCARD);
	      outputResource.save(options);
	    } catch (IOException e) {
	      e.printStackTrace();
	    }
	}

	//**************************** OPTIONAL Hibernate DataStore *************************************
	
	//TODO: alternatively in a subclass
	//TODO: export properties
	protected void initializeDB(String dbName, EPackage[] epackages) {
		//*************** Initialize Teneo Hibernate DataStore *************************************
		HbDataStore hbds = (HbDataStore) HbHelper.INSTANCE.createRegisterDataStore(dbName);
		//Set Database properties
		Properties props = initializeDataStoreProperties(dbServer, dbName, dbUser, dbPass);
		
		hbds.setDataStoreProperties(props);
		hbds.setEPackages(epackages);
		hbds.initialize();
	}

	protected Properties initializeDataStoreProperties(String dbServer, String dbName, String dbUser, String dbPass) {
		Properties props = new Properties();
		props.setProperty(Environment.DRIVER, "com.mysql.jdbc.Driver");
		props.setProperty(Environment.URL, "jdbc:mysql://"+dbServer+":"+dbPort+"/"+dbName);
		props.setProperty(Environment.USER, dbUser);
		props.setProperty(Environment.PASS, dbPass);
		props.setProperty(Environment.DIALECT,  org.hibernate.dialect.MySQL5InnoDBDialect.class.getName());
		props.setProperty(Environment.SHOW_SQL, "true");
		props.setProperty(Environment.HBM2DDL_AUTO, "update");
		props.setProperty(PersistenceOptions.INHERITANCE_MAPPING, "JOINED");
		props.setProperty(PersistenceOptions.MAXIMUM_SQL_NAME_LENGTH, "60");
		props.setProperty(PersistenceOptions.SET_GENERATED_VALUE_ON_ID_FEATURE, "TRUE");
		// props.setProperty(Environment.HBM2DDL_AUTO, "create-drop");
		return props;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void storeResourceInDB(EList<EObject> contents, String dbName){
		String uriStr = "hibernate://?"+HibernateResource.DS_NAME_PARAM+"="+dbName;

		final URI uri = URI.createURI(uriStr);
		ResourceSet resourceSet = new ResourceSetImpl();
		Resource resource = resourceSet.createResource(uri);
		try {
		    resource.load(null);
		    
		    Map options = new HashMap<>();
		    options.put(HibernateResource.OPTION_SAVE_ONLY_IF_CHANGED, HibernateResource.OPTION_SAVE_ONLY_IF_CHANGED_MEMORY_BUFFER);

		    if (resource.getContents().size() == 0) {
		    	resource.getContents().addAll(contents);
		        resource.save(options);
		    } else {
		    	resource.save(options);
		    }
		} catch (IOException e) {
		    e.printStackTrace();
		}
	}
	
	@SuppressWarnings({ "rawtypes", "unused" })
	public Resource loadResourceFromDB(String dbName) {
		String uriStr = "hibernate://?"+HibernateResource.DS_NAME_PARAM+"="+dbName;

		final URI uri = URI.createURI(uriStr);
		ResourceSet resourceSet = new ResourceSetImpl();
		Resource resource = resourceSet.createResource(uri);
	    try {
	    	Map options = new HashMap<>();
			resource.load(options);
			EList<org.eclipse.emf.ecore.resource.Resource.Diagnostic> errors = resource.getErrors();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return resource;
	}

}