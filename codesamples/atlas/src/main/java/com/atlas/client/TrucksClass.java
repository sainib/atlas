package com.atlas.client;



import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import org.apache.atlas.AtlasClient;
import org.apache.atlas.typesystem.Referenceable;
import org.apache.atlas.typesystem.TypesDef;
import org.apache.atlas.typesystem.json.InstanceSerialization;
import org.apache.atlas.typesystem.json.TypesSerialization;
import org.apache.atlas.typesystem.persistence.Id;
import org.apache.atlas.typesystem.types.AttributeDefinition;
import org.apache.atlas.typesystem.types.ClassType;
import org.apache.atlas.typesystem.types.DataTypes;
import org.apache.atlas.typesystem.types.EnumTypeDefinition;
import org.apache.atlas.typesystem.types.HierarchicalTypeDefinition;
import org.apache.atlas.typesystem.types.IDataType;
import org.apache.atlas.typesystem.types.Multiplicity;
import org.apache.atlas.typesystem.types.StructTypeDefinition;
import org.apache.atlas.typesystem.types.TraitType;
import org.apache.atlas.typesystem.types.TypeUtils;
import org.apache.atlas.typesystem.types.utils.TypesUtil;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import java.util.List;

/**
 * A driver that sets up sample types and data for testing purposes.
 * Please take a look at QueryDSL in docs for the Meta Model.
 * todo - move this to examples module.
 */
public class TrucksClass {

	{
		System.setProperty("atlas.conf", "/Users/sdutta/Applications/conf");
	}
	

    private static final String DATABASE_TYPE = "DB";
    private static final String COLUMN_TYPE = "Column";
    private static final String TABLE_TYPE = "Table";
    private static final String VIEW_TYPE = "View";
    private static final String LOAD_PROCESS_TYPE = "LoadProcess";
    private static final String STORAGE_DESC_TYPE = "StorageDesc";
    

    private static final String[] TYPES =
            {DATABASE_TYPE, TABLE_TYPE, STORAGE_DESC_TYPE, COLUMN_TYPE, LOAD_PROCESS_TYPE, VIEW_TYPE, "JdbcAccess",
                    "ETL", "Metric", "PII", "Fact", "Dimension"};

    private final AtlasClient metadataServiceClient;


	/**
	 * 
	 * @param args
	 * @throws Exception
	 */
    public static void main(String[] args) throws Exception {
        
    	
    	String baseUrl = args[0];
    	String databasename = args[1];
    	String tablename = args[2];
    	String tablename2 = args[3];
    	String flag = args[4];
    	
        TrucksClass quickStart = new TrucksClass(baseUrl);

        // Shows how to create types in Atlas for your meta model
        
        if("createtype".equalsIgnoreCase(flag))
        		quickStart.createTypes();

        // Shows how to create entities (instances) for the added types in Atlas
        quickStart.createMysqlEntities(databasename, tablename, tablename2);

        // Shows some search queries using DSL based on types
        if("search".equalsIgnoreCase(flag))
        	quickStart.search();
    }

    
    TrucksClass(String baseUrl) {
    	
    	
        metadataServiceClient = new AtlasClient(baseUrl);
    }


    void createTypes() throws Exception {
        TypesDef typesDef = createTypeDefinitions();

        String typesAsJSON = TypesSerialization.toJson(typesDef);
        System.out.println("typesAsJSON = " + typesAsJSON);
        metadataServiceClient.createType(typesAsJSON);

        // verify types created
        verifyTypesCreated();
    }

    
    
    
    TypesDef createTypeDefinitions() throws Exception {
    
    	HierarchicalTypeDefinition<ClassType> dbClsDef = TypesUtil
                .createClassTypeDef(DATABASE_TYPE, null, attrDef("name", DataTypes.STRING_TYPE),
                        attrDef("description", DataTypes.STRING_TYPE), attrDef("locationUri", DataTypes.STRING_TYPE),
                        attrDef("owner", DataTypes.STRING_TYPE), attrDef("createTime", DataTypes.INT_TYPE));

        HierarchicalTypeDefinition<ClassType> storageDescClsDef = TypesUtil
                .createClassTypeDef(STORAGE_DESC_TYPE, null, attrDef("location", DataTypes.STRING_TYPE),
                        attrDef("inputFormat", DataTypes.STRING_TYPE), attrDef("outputFormat", DataTypes.STRING_TYPE),
                        attrDef("compressed", DataTypes.STRING_TYPE, Multiplicity.REQUIRED, false, null));

        HierarchicalTypeDefinition<ClassType> columnClsDef = TypesUtil
                .createClassTypeDef(COLUMN_TYPE, null, attrDef("name", DataTypes.STRING_TYPE),
                        attrDef("dataType", DataTypes.STRING_TYPE), attrDef("comment", DataTypes.STRING_TYPE));

        
        HierarchicalTypeDefinition<ClassType> tblClsDef = TypesUtil
                .createClassTypeDef(TABLE_TYPE, ImmutableList.of("DataSet"),
                        new AttributeDefinition("db", DATABASE_TYPE, Multiplicity.REQUIRED, false, null),
                        new AttributeDefinition("sd", STORAGE_DESC_TYPE, Multiplicity.REQUIRED, true, null),
                        attrDef("owner", DataTypes.STRING_TYPE), attrDef("createTime", DataTypes.INT_TYPE),
                        attrDef("lastAccessTime", DataTypes.INT_TYPE), attrDef("retention", DataTypes.INT_TYPE),
                        attrDef("viewOriginalText", DataTypes.STRING_TYPE),
                        attrDef("viewExpandedText", DataTypes.STRING_TYPE), attrDef("tableType", DataTypes.STRING_TYPE),
                        attrDef("temporary", DataTypes.BOOLEAN_TYPE),
                        new AttributeDefinition("columns", DataTypes.arrayTypeName(COLUMN_TYPE),
                                Multiplicity.COLLECTION, true, null));

        
        HierarchicalTypeDefinition<ClassType> loadProcessClsDef = TypesUtil
                .createClassTypeDef(LOAD_PROCESS_TYPE, ImmutableList.of("Process"),
                        attrDef("userName", DataTypes.STRING_TYPE), attrDef("startTime", DataTypes.INT_TYPE),
                        attrDef("endTime", DataTypes.INT_TYPE),
                        attrDef("queryText", DataTypes.STRING_TYPE, Multiplicity.REQUIRED),
                        attrDef("queryPlan", DataTypes.STRING_TYPE, Multiplicity.REQUIRED),
                        attrDef("queryId", DataTypes.STRING_TYPE, Multiplicity.REQUIRED),
                        attrDef("queryGraph", DataTypes.STRING_TYPE, Multiplicity.REQUIRED));

        
        HierarchicalTypeDefinition<ClassType> viewClsDef = TypesUtil
                .createClassTypeDef(VIEW_TYPE, null, attrDef("name", DataTypes.STRING_TYPE),
                        new AttributeDefinition("db", DATABASE_TYPE, Multiplicity.REQUIRED, false, null),
                        new AttributeDefinition("inputTables", DataTypes.arrayTypeName(TABLE_TYPE),
                                Multiplicity.COLLECTION, false, null));

        
        HierarchicalTypeDefinition<TraitType> dimTraitDef = TypesUtil.createTraitTypeDef("Dimension", null);

        
        HierarchicalTypeDefinition<TraitType> factTraitDef = TypesUtil.createTraitTypeDef("Fact", null);

        
        HierarchicalTypeDefinition<TraitType> piiTraitDef = TypesUtil.createTraitTypeDef("PII", null);

        
        HierarchicalTypeDefinition<TraitType> metricTraitDef = TypesUtil.createTraitTypeDef("Metric", null);

        
        HierarchicalTypeDefinition<TraitType> etlTraitDef = TypesUtil.createTraitTypeDef("ETL", null);

        HierarchicalTypeDefinition<TraitType> jdbcTraitDef = TypesUtil.createTraitTypeDef("JdbcAccess", null);

        return TypeUtils.getTypesDef(ImmutableList.<EnumTypeDefinition>of(), ImmutableList.<StructTypeDefinition>of(),
                ImmutableList.of(dimTraitDef, factTraitDef, piiTraitDef, metricTraitDef, etlTraitDef, jdbcTraitDef),
                ImmutableList.of(dbClsDef, storageDescClsDef, tblClsDef, loadProcessClsDef, viewClsDef));
    }

    AttributeDefinition attrDef(String name, IDataType dT) {
        return attrDef(name, dT, Multiplicity.OPTIONAL, false, null);
    }

    AttributeDefinition attrDef(String name, IDataType dT, Multiplicity m) {
        return attrDef(name, dT, m, false, null);
    }

    AttributeDefinition attrDef(String name, IDataType dT, Multiplicity m, boolean isComposite,
            String reverseAttributeName) {
        Preconditions.checkNotNull(name);
        Preconditions.checkNotNull(dT);
        return new AttributeDefinition(name, dT.getName(), m, isComposite, reverseAttributeName);
    }

    
    
    void createMysqlEntities(String databasename, String tablename, String tablename2) throws Exception {
        
    	Id sourceDB = database(databasename, "MySQL Database", "Oracle", "mysql -u root");


        Referenceable sd =
                rawStorageDescriptor("hdfs://host:8000/apps/warehouse/products", "TextInputFormat", "TextOutputFormat",
                        true);

        List<Referenceable> driversColumns = ImmutableList
                .of(rawColumn("model_id", "String", "model_id"), rawColumn("model_name", "String", "model name"),
                        rawColumn("max_speed", "String", "maximum speed", "Red"),
                        rawColumn("torque", "String", "torque"),
                        rawColumn("engine_type", "String", "engine diesel/gas"),
                        rawColumn("tow_capacity", "String", "towing capacity"),
                        rawColumn("model_year", "String", "model_year"));

        
        Id trucks = table(tablename, "mysql table", sourceDB, sd, "Ford Jr", "Managed", driversColumns, "Trucks");

        Id Motorcycle = table(tablename2, "mysql table", sourceDB, sd, "Ford Jr", "Managed", driversColumns, "Trucks");
       
       ;

    }

    private Id createInstance(Referenceable referenceable) throws Exception {
        String typeName = referenceable.getTypeName();

        String entityJSON = InstanceSerialization.toJson(referenceable, true);
        System.out.println("Submitting new entity= " + entityJSON);
        JSONObject jsonObject = metadataServiceClient.createEntity(entityJSON);
        String guid = jsonObject.getString(AtlasClient.GUID);
        System.out.println("created instance for type " + typeName + ", guid: " + guid);

        // return the Id for created instance with guid
        return new Id(guid, referenceable.getId().getVersion(), referenceable.getTypeName());
    }

    
    
    Id database(String name, String description, String owner, String locationUri, String... traitNames)
    throws Exception {
        Referenceable referenceable = new Referenceable(DATABASE_TYPE, traitNames);
        referenceable.set("name", name);
        referenceable.set("description", description);
        referenceable.set("owner", owner);
        referenceable.set("locationUri", locationUri);
        referenceable.set("createTime", System.currentTimeMillis());

        return createInstance(referenceable);
    }

    
    
    Referenceable rawStorageDescriptor(String location, String inputFormat, String outputFormat, boolean compressed)
    throws Exception {
        Referenceable referenceable = new Referenceable(STORAGE_DESC_TYPE);
        referenceable.set("location", location);
        referenceable.set("inputFormat", inputFormat);
        referenceable.set("outputFormat", outputFormat);
        referenceable.set("compressed", compressed);

        return referenceable;
    }

    
    
    Referenceable rawColumn(String name, String dataType, String comment, String... traitNames) throws Exception {
        Referenceable referenceable = new Referenceable(COLUMN_TYPE, traitNames);
        referenceable.set("name", name);
        referenceable.set("dataType", dataType);
        referenceable.set("comment", comment);

        return referenceable;
    }

    Id table(String name, String description, Id dbId, Referenceable sd, String owner, String tableType,
            List<Referenceable> columns, String... traitNames) throws Exception {
        Referenceable referenceable = new Referenceable(TABLE_TYPE, traitNames);
        referenceable.set("name", name);
        referenceable.set("description", description);
        referenceable.set("owner", owner);
        referenceable.set("tableType", tableType);
        referenceable.set("createTime", System.currentTimeMillis());
        referenceable.set("lastAccessTime", System.currentTimeMillis());
        referenceable.set("retention", System.currentTimeMillis());
        referenceable.set("db", dbId);
        referenceable.set("sd", sd);
        referenceable.set("columns", columns);

        return createInstance(referenceable);
    }

    Id loadProcess(String name, String description, String user, List<Id> inputTables, List<Id> outputTables,
            String queryText, String queryPlan, String queryId, String queryGraph, String... traitNames)
    throws Exception {
        Referenceable referenceable = new Referenceable(LOAD_PROCESS_TYPE, traitNames);
        // super type attributes
        referenceable.set("name", name);
        referenceable.set("description", description);
        referenceable.set("inputs", inputTables);
        referenceable.set("outputs", outputTables);

        referenceable.set("user", user);
        referenceable.set("startTime", System.currentTimeMillis());
        referenceable.set("endTime", System.currentTimeMillis() + 10000);

        referenceable.set("queryText", queryText);
        referenceable.set("queryPlan", queryPlan);
        referenceable.set("queryId", queryId);
        referenceable.set("queryGraph", queryGraph);

        return createInstance(referenceable);
    }

    Id view(String name, Id dbId, List<Id> inputTables, String... traitNames) throws Exception {
        Referenceable referenceable = new Referenceable(VIEW_TYPE, traitNames);
        referenceable.set("name", name);
        referenceable.set("db", dbId);

        referenceable.set("inputTables", inputTables);

        return createInstance(referenceable);
    }

    private void verifyTypesCreated() throws Exception {
        List<String> types = metadataServiceClient.listTypes();
        for (String type : TYPES) {
            assert types.contains(type);
        }
    }

    private String[] getDSLQueries() {
        return new String[]{"from DB", "DB", "DB where name=\"Reporting\"", "DB where DB.name=\"Reporting\"",
                "DB name = \"Reporting\"", "DB DB.name = \"Reporting\"",
                "DB where name=\"Reporting\" select name, owner", "DB where DB.name=\"Reporting\" select name, owner",
                "DB has name", "DB where DB has name", "DB, Table", "DB is JdbcAccess",
            /*
            "DB, hive_process has name",
            "DB as db1, Table where db1.name = \"Reporting\"",
            "DB where DB.name=\"Reporting\" and DB.createTime < " + System.currentTimeMillis()},
            */
                "from Table", "Table", "Table is Dimension", "Column where Column isa PII", "View is Dimension",
            /*"Column where Column isa PII select Column.name",*/
                "Column select Column.name", "Column select name", "Column where Column.name=\"customer_id\"",
                "from Table select Table.name", "DB where (name = \"Reporting\")",
                "DB where (name = \"Reporting\") select name as _col_0, owner as _col_1", "DB where DB is JdbcAccess",
                "DB where DB has name", "DB Table", "DB where DB has name",
                "DB as db1 Table where (db1.name = \"Reporting\")",
                "DB where (name = \"Reporting\") select name as _col_0, (createTime + 1) as _col_1 ",
            /*
            todo: does not work
            "DB where (name = \"Reporting\") and ((createTime + 1) > 0)",
            "DB as db1 Table as tab where ((db1.createTime + 1) > 0) and (db1.name = \"Reporting\") select db1.name
            as dbName, tab.name as tabName",
            "DB as db1 Table as tab where ((db1.createTime + 1) > 0) or (db1.name = \"Reporting\") select db1.name as
             dbName, tab.name as tabName",
            "DB as db1 Table as tab where ((db1.createTime + 1) > 0) and (db1.name = \"Reporting\") or db1 has owner
            select db1.name as dbName, tab.name as tabName",
            "DB as db1 Table as tab where ((db1.createTime + 1) > 0) and (db1.name = \"Reporting\") or db1 has owner
            select db1.name as dbName, tab.name as tabName",
            */
                // trait searches
                "Dimension",
            /*"Fact", - todo: does not work*/
                "JdbcAccess", "ETL", "Metric", "PII",
            /*
            // Lineage - todo - fix this, its not working
            "Table hive_process outputTables",
            "Table loop (hive_process outputTables)",
            "Table as _loop0 loop (hive_process outputTables) withPath",
            "Table as src loop (hive_process outputTables) as dest select src.name as srcTable, dest.name as
            destTable withPath",
            */
                "Table where name=\"sales_fact\", columns",
                "Table where name=\"sales_fact\", columns as column select column.name, column.dataType, column"
                        + ".comment",
                "from DataSet", "from Process",};
    }

    private void search() throws Exception {
        for (String dslQuery : getDSLQueries()) {
            JSONObject response = metadataServiceClient.searchEntity(dslQuery);
            JSONObject results = response.getJSONObject(AtlasClient.RESULTS);
            if (!results.isNull("rows")) {
                JSONArray rows = results.getJSONArray("rows");
                System.out.println("query [" + dslQuery + "] returned [" + rows.length() + "] rows");
            } else {
                System.out.println("query [" + dslQuery + "] failed, results:" + results.toString());
            }
        }
    }
}
