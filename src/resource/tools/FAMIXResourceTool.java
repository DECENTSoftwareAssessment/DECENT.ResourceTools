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
import org.slf4j.LoggerFactory;


import famix.M3xStandaloneSetup;

import AbstractDECENTProvider.AbstractDECENTProviderPackage;
import AbstractDECENTProvider.AbstractElement;
import AbstractDECENTProvider.AbstractNamedElement;
import AbstractDECENTProvider.Model;
import FAMIX.FAMIXPackage;
import FAMIX.impl.FAMIXPackageImpl;

public class FAMIXResourceTool extends ResourceTool {
	
	public FAMIXResourceTool(){
		super();
		log = LoggerFactory.getLogger(FAMIXResourceTool.class);
		FAMIXPackageImpl.init();
		initializeValidator();
		injector = new M3xStandaloneSetup().createInjectorAndDoEMFRegistration();

	}
	
	@Override
	protected void initializeValidator(){
		super.initializeValidator();
		EObjectValidator validator = new EObjectValidator();
		EValidator.Registry.INSTANCE.put(AbstractDECENTProviderPackage.eINSTANCE, validator);
	    EValidator.Registry.INSTANCE.put(FAMIXPackage.eINSTANCE, validator);
	}
	
	public void process(String workspace, int commitId) {

		//TODO: extract as configuration
		String xTextLocation = workspace+"/model.mse";
		String outputPath = workspace+"/model.famix";
		String outputPathValidated = workspace+"/model.famix";
		String extension = "famix";

		Resource resource = loadResourceFromXtext(workspace,xTextLocation,false);
		virtualizeUnresolvedProxies(resource);
		setRevisionAndGUIDs(resource, commitId);
		storeResourceContents(resource.getContents(), outputPath, extension);
		Resource fromXMI = loadResourceFromXMI(outputPath, extension);
//	    validateResource(fromXMI);
		storeResourceContents(fromXMI.getContents(), outputPathValidated, extension);

	}
	
	public void virtualizeUnresolvedProxies(Resource resource) {
		Map<EObject, Collection<Setting>> unresolved = EcoreUtil.UnresolvedProxyCrossReferencer.find(resource);
		for (EObject k : unresolved.keySet()) {
			AbstractNamedElement element = (AbstractNamedElement)k;
			InternalEObject io = (InternalEObject) k;
			element.setName("unresolved_"+element.eClass().getName()+"_"+io.eProxyURI().fragment());
			io.eSetProxyURI(null);
			Model model = (Model) resource.getContents().get(0);
			model.getElements().add(element);
		}
	}
	
	public void setRevisionAndGUIDs(Resource resource, int revisionId) {
		//TODO: GUID could eventually be dropped as each resource type kinda handles this on its own 
		//and the GUIDs cannot be recreated 
		for (EObject model : resource.getContents()) {
			for (EObject element : model.eContents()) {
				((AbstractElement) element).setRevisionId(revisionId);
				((AbstractElement) element).setGUID(EcoreUtil.generateUUID()+"_"+revisionId+"_"+element.hashCode());
			}
		}
	}

	public void initializeDB(String dbName) {
		EPackage[] epackages = new EPackage[] { FAMIXPackage.eINSTANCE,AbstractDECENTProviderPackage.eINSTANCE };
		super.initializeDB(dbName, epackages);
	}
	
	

}
