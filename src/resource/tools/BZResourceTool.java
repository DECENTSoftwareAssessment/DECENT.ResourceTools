package resource.tools;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EValidator;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EObjectValidator;
import org.eclipse.emf.teneo.PersistenceOptions;
import org.eclipse.emf.teneo.hibernate.HbDataStore;
import org.eclipse.emf.teneo.hibernate.HbHelper;
import org.hibernate.Query;
import org.hibernate.Session;

import emf.resource.tools.ResourceTool;
import AbstractDECENTProvider.AbstractDECENTProviderPackage;
import BZ.BZPackage;
import BZ.impl.BZPackageImpl;

public class BZResourceTool extends ResourceTool {

	//TODO: find a more appropriate way to handle mapping files
	private String mappingFilePath;
	
	public BZResourceTool() {
		super(BZResourceTool.class.getName());
		BZPackageImpl.init();
		initializeValidator();
	}

	@Override
	protected void initializeValidator(){
		super.initializeValidator();
		EObjectValidator validator = new EObjectValidator();
		EValidator.Registry.INSTANCE.put(AbstractDECENTProviderPackage.eINSTANCE, validator);
	    EValidator.Registry.INSTANCE.put(BZPackage.eINSTANCE, validator);
	}

	public void process(String workspace, String dbName){
		//TODO: parameterize
		String outputPath = workspace+"/model-source.bz";
		String outputPathFromDB = workspace+"/model.bz";
		String extension = "bz";
		
		String mappingRefFilePath = "/home/philip-iii/Dev/workspaces/emf/DECENT.ResourceTools/mapping/bz-ref.hbm.xml";
		mappingFilePath = "/bz.hbm-advanced.xml"; //relative to mapping which should be in the class path
		
//		Resource fromXMI = loadResourceFromXMI(outputPath, extension);
		initializeDB(dbName);
		if (!new File("mapping"+mappingFilePath).exists()) {
			saveReferenceMappings(HbHelper.INSTANCE.getDataStore(dbName), "mapping/ref"+mappingFilePath);
		}

//	    storeResourceInDB(fromXMI.getContents(),dbName);
	    Resource fromDB = loadResourceFromDB(dbName);
		storeResourceContents(fromDB.getContents(), outputPathFromDB, extension);
	}
	
	public void initializeDB(String dbName) {
		EPackage[] epackages = new EPackage[] { BZPackage.eINSTANCE};
		initializeDB(dbName, epackages);
	}

	@Override
	protected void initializeDB(String dbName, EPackage[] epackages) {
		//*************** Initialize Teneo Hibernate DataStore *************************************
		HbDataStore hbds = (HbDataStore) HbHelper.INSTANCE.createRegisterDataStore(dbName);
		//Set Database properties
		Properties props = initializeDataStoreProperties(getDbServer(), dbName, getDbUser(), getDbPass());

		HbHelper.INSTANCE.getDataStore(dbName).getDataStoreProperties();
		
		//TODO: figure out selective mapping
		if (new File("mapping"+mappingFilePath).exists()) {
			props.setProperty(PersistenceOptions.MAPPING_FILE_PATH, mappingFilePath);
		} else {
			System.out.println("Mapping file does not exist, a new one will be created...");
		}

//		props.setProperty(Environment.HBM2DDL_AUTO, "create-drop");
		hbds.setDataStoreProperties(props);
		hbds.setEPackages(epackages);
		hbds.initialize();

		//TODO: check out texo briefly, but 
		//it seems like it will be necessary to manipulate the databases in order to include 
		// - missing indices, keys, and backreference fields (for containers, but it may be possible to discard containers) 
		//  - added automatically upon database initialization
		//  - backreference contents need to be added, for simplicity all backreference links for container model shall be called "model_id"
		//  - model instance needs to be added manually as well (the backreferences should point to the same id)
		//  - other containers may need a different approach
		//and also the corresponding mappings will need to be manually adjusted
		// - more or less doable with configuration
		
		//DONE: file types need to be adapted as well in the source database, since they use the inverse of what they really do
		//files should have a type id and not file types have a file id (one to many)
		//- use filetype field instead (updated upon first run as there are other issues with the state of file types as well) 
		//TODO: similarly, handling of hunk blames is suboptimal
		
		//TODO: one index still failing
		//TODO: consider adding the new extensions to the main model tree
		//TODO: consider creating a base and extended mappings (excluding or including the extensions)
		//TODO: consider refining containment relationships (but how? revision-centric? file-centric? people-centric? these are all just views)
		
		firstRunSetup(hbds);
		
	}

	private void firstRunSetup(HbDataStore hbds) {
		hbds.getHbContext();
		Session session = hbds.getSessionFactory().openSession();

		//check if there is anything in the db and skip the rest if it is not the case as it is not applicable
		Query checkDBExistsQuery = session.createQuery("FROM BZIssue");
		if (checkDBExistsQuery.list().size() == 0){
			session.close();
			return;
		}

		int repoId = 1;
		
		//check if first run (whether the dummy top-level file has been created) and do some tune up
		Query checkQuery = session.createQuery("FROM BZIssue WHERE repoId = "+repoId+"");
		if (checkQuery.list().size() == 0){
			logInfo("First run! Adjusting database...");
			
			//fix missing repository links, 
			//TODO: need to check the proper repo id
			//TODO: figure out why BZComponent has wrong repoId
			String[] classes = new String[]{"BZIssue", "BZComponent"};
			for (String c : classes) {
				Query updateQuery = session.createQuery("UPDATE "+c+" SET repoId = "+repoId+"");
				updateQuery.executeUpdate();
			}

			logInfo("  Updating product IDs...");
			Query updateProductIdQuery = session.createSQLQuery("UPDATE BZProduct p, BZIssue i SET i.productId = p.id WHERE i.product = p.productId");
			updateProductIdQuery.executeUpdate();
			
			logInfo("  Updating component IDs...");
			Query updateComponentIdQuery = session.createSQLQuery("UPDATE BZComponent c, BZIssue i SET i.componentId = c.id WHERE i.component = c.componentId");
			updateComponentIdQuery.executeUpdate();

			logInfo("  Updating comments...");
			Query updateCommentsQuery = session.createSQLQuery("UPDATE BZComment c, BZIssue i SET c.issue = i.id WHERE c.issueId = i.issueId");
			updateCommentsQuery.executeUpdate();
			
			logInfo("  Updating events...");
			Query updateEventsQuery = session.createSQLQuery("UPDATE BZEvent e, BZIssue i SET e.issue = i.id WHERE e.issueId = i.issueId");
			updateEventsQuery.executeUpdate();

			//TODO: this should probably be included in MG as well
			logInfo("  Cleaning comments...");
			
			for (int i=0; i<32; i++) {
				//Skip 0x9 (TAB), 0xA (CR?), 0xD (LF?)
				String c = "0x"+Integer.toHexString(i);
				if (c.equals("0x9") 
						|| c.equalsIgnoreCase("0xa") 
						|| c.equalsIgnoreCase("0xD")) {
					continue;
				}
				Query cleanCommentsQuery = session.createSQLQuery(
						"UPDATE BZComment B " + 
								"SET commentText = REPLACE (commentText, CHAR("+c+" using utf8), '') " +
								"  , commentHTML = REPLACE (commentHTML, CHAR("+c+" using utf8), '') " );
				cleanCommentsQuery.executeUpdate();
			}
			
			
			logInfo("  ...done");
			
			
		}

		session.close();
	}

	private void saveReferenceMappings(HbDataStore hbds, String mappingRefFilePath) {
		try {
			FileUtils.writeStringToFile(new File(mappingRefFilePath ), hbds.getMappingXML());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	@Override
	protected Properties initializeDataStoreProperties(String dbServer, String dbName, String dbUser, String dbPass) {
		Properties props = super.initializeDataStoreProperties(dbServer, dbName, dbUser, dbPass);
		
		props.setProperty(PersistenceOptions.JOIN_TABLE_FOR_NON_CONTAINED_ASSOCIATIONS, "FALSE");
		props.setProperty(PersistenceOptions.ALWAYS_MAP_LIST_AS_BAG, "TRUE");
		
		props.setProperty(PersistenceOptions.ALWAYS_VERSION, "false");
		props.setProperty(PersistenceOptions.DISABLE_ECONTAINER_MAPPING, "true");
		props.setProperty(PersistenceOptions.INHERITANCE_MAPPING, "JOINED");
		props.setProperty(PersistenceOptions.SET_FOREIGN_KEY_NAME, "false");
		props.setProperty(PersistenceOptions.JOIN_TABLE_FOR_NON_CONTAINED_ASSOCIATIONS, "false");
		props.setProperty("teneo.naming.strategy", "none");
		props.setProperty("teneo.mapping.sql_name_escape_character", "");
		props.setProperty("teneo.mapping.cascade_policy_on_containment", "REMOVE, REFRESH, PERSIST, MERGE, CASCADE");
		//props.setProperty(PersistenceOptions.MAP_ALL_LISTS_AS_IDBAG, "true");
		props.setProperty(PersistenceOptions.ALWAYS_MAP_LIST_AS_BAG, "true");
		props.setProperty(PersistenceOptions.ADD_INDEX_FOR_FOREIGN_KEY, "false");
		props.setProperty(PersistenceOptions.ID_FEATURE_AS_PRIMARY_KEY, "true");
		//props.setProperty(PersistenceOptions.SET_CASCADE_ALL_ON_CONTAINMENT,"false");
		//props.setProperty(PersistenceOptions.EMAP_AS_TRUE_MAP,"true");
		props.setProperty("teneo.naming.default_id_column", "id"); 
		props.setProperty("teneo.mapping.disable_econtainer", "true");

		return props;
	}
}
