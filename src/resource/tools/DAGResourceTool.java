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

import AbstractDECENTProvider.AbstractDECENTProviderPackage;
import AbstractDECENTProvider.AbstractElement;
import AbstractDECENTProvider.AbstractNamedElement;
import AbstractDECENTProvider.Model;
import DAG.DAGPackage;
import DAG.impl.DAGPackageImpl;
import DAGx.DAGxStandaloneSetup;
import FAMIX.FAMIXPackage;
import FAMIX.impl.FAMIXPackageImpl;

public class DAGResourceTool extends ResourceTool {
	
	public DAGResourceTool(){
		super();
		log = LoggerFactory.getLogger(DAGResourceTool.class);
		DAGPackageImpl.init();
		initializeValidator();
		injector = new DAGxStandaloneSetup().createInjectorAndDoEMFRegistration();
	}
	
	@Override
	protected void initializeValidator(){
		super.initializeValidator();
		EObjectValidator validator = new EObjectValidator();
	    EValidator.Registry.INSTANCE.put(DAGPackage.eINSTANCE, validator);
	}
	
	public void process(String workspace) {

		String xTextLocation = workspace+"/model.dagx";
		String outputPath = workspace+"/model.dag";
		String outputPathValidated = workspace+"/model.dag";
		String extension = "dag";

		Resource resource = loadResourceFromXtext(workspace,xTextLocation,true);
		storeResourceContents(resource.getContents(), outputPath);
		Resource fromXMI = loadResourceFromXMI(outputPath, extension);
//	    validateResource(fromXMI);
		storeResourceContents(fromXMI.getContents(), outputPathValidated);

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
		EPackage[] epackages = new EPackage[] { DAGPackage.eINSTANCE };
		super.initializeDB(dbName, epackages);
	}
	
	

}