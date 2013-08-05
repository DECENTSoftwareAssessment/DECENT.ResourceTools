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
import org.slf4j.LoggerFactory;

import AbstractDECENTProvider.AbstractDECENTProviderPackage;
import MGStandalone.MGStandalonePackage;
import MGStandalone.impl.MGStandalonePackageImpl;

public class MGResourceTool extends ResourceTool {

	//TODO: find a more appropriate way to handle mapping files
	private String mappingFilePath;
	
	public MGResourceTool() {
		super();
		MGStandalonePackageImpl.init();
		log = LoggerFactory.getLogger(MGResourceTool.class);
		initializeValidator();
	}

	@Override
	protected void initializeValidator(){
		super.initializeValidator();
		EObjectValidator validator = new EObjectValidator();
		EValidator.Registry.INSTANCE.put(AbstractDECENTProviderPackage.eINSTANCE, validator);
	    EValidator.Registry.INSTANCE.put(MGStandalonePackage.eINSTANCE, validator);
	}

	public void process(String workspace){
		//TODO: parameterize
		String dbName = "MGGitWS";
		String outputPath = workspace+"/model-source.mg";
		String outputPathFromDB = workspace+"/model.mg";
		String extension = "mg";
		
		String mappingRefFilePath = "/home/philip-iii/Dev/workspaces/emf/MininGit.m1x/mapping/mg-ref.hbm.xml";
		mappingFilePath = "/mg.hbm.xml"; //relative to mapping which should be in the class path
//		mappingFilePath = "/mg-core.hbm.xml"; //core
		
//		Resource fromXMI = loadResourceFromXMI(outputPath, extension);
		initializeDB(dbName);

		saveReferenceMappings(HbHelper.INSTANCE.getDataStore(dbName), mappingRefFilePath);

//	    storeResourceInDB(fromXMI.getContents(),dbName);
	    Resource fromDB = loadResourceFromDB(dbName);
		storeResourceContents(fromDB.getContents(), outputPathFromDB);
	}
	
	public void initializeDB(String dbName) {
		EPackage[] epackages = new EPackage[] { MGStandalonePackage.eINSTANCE};
		initializeDB(dbName, epackages);
	}

	@Override
	protected void initializeDB(String dbName, EPackage[] epackages) {
		//*************** Initialize Teneo Hibernate DataStore *************************************
		HbDataStore hbds = (HbDataStore) HbHelper.INSTANCE.createRegisterDataStore(dbName);
		//Set Database properties
		Properties props = initializeDataStoreProperties(dbServer, dbName, dbUser, dbPass);

		HbHelper.INSTANCE.getDataStore(dbName).getDataStoreProperties();
		
		//TODO: figure out selective mapping
		props.setProperty(PersistenceOptions.MAPPING_FILE_PATH, mappingFilePath);

		// props.setProperty(Environment.HBM2DDL_AUTO, "create-drop");
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

		//check if first run (whether the dummy top-level file has been created) and do some tune up
		Query checkQuery = session.createQuery("FROM File WHERE id = '-1'");
		if (checkQuery.list().size() == 0){
			log.info("First run! Adjusting database...");
			int repository_id = 1;
			String directoryFileType = "directory";

			//insert a dummy top-level file
			Query insertQuery = session.createSQLQuery("INSERT INTO files (id, file_name, repository_id) VALUES('-1', 'ROOT', '"+repository_id +"')");
			insertQuery.executeUpdate();
			
			//add file type field values 
			Query fileTypeUpdateQuery = session.createSQLQuery("UPDATE files f SET file_type = IFNULL((SELECT type FROM file_types t WHERE t.file_id = f.id),'"+directoryFileType +"')");
			fileTypeUpdateQuery.executeUpdate();
			
			//TODO: some storage engine fixes since InnoDB and MyISAM don't seem to work together well
			//ideally will be done by MininGit directly
			//needs to be done before the initialization
			String[] tables = new String[]{"actions","blame","branches","file_copies","file_links","file_types","files","scmlog","people","tags","hunk_blames","line_blames","patch_lines","repositories","tag_revisions"};
			for (String c : tables) {
				Query alterQuery = session.createSQLQuery("ALTER TABLE "+c+" ENGINE = InnoDB");
				alterQuery.executeUpdate();
			}

			//fix missing repository links, 
			//TODO: need to check the proper repo id
			//TODO: need to extend with other classes too
			String[] classes = new String[]{"Action","Branch","FileCopy","FileLink","People","Tag"};
			for (String c : classes) {
				Query updateQuery = session.createQuery("UPDATE "+c+" SET repository = '"+repository_id+"'");
				updateQuery.executeUpdate();
			}
			
			//add a commit_id column to store a copy of scmlog.id
			Query alterQuery = session.createSQLQuery("ALTER TABLE scmlog ADD COLUMN commit_id INTEGER NOT NULL;");
			alterQuery.executeUpdate();
			
			//set commit_id = id
			Query updateQuery = session.createQuery("UPDATE scmlog s SET s.commit_id = s.id");
			updateQuery.executeUpdate();
			
			log.info("  ...done");
			
			
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
		props.setProperty("teneo.naming.strategy", "lowercase");
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
