package com.atlas.client;

/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */



import org.apache.atlas.AtlasClient;
import org.apache.atlas.AtlasServiceException;
import org.apache.atlas.hive.model.HiveDataModelGenerator;
import org.apache.atlas.hive.model.HiveDataTypes;
import org.apache.atlas.typesystem.Referenceable;
import org.apache.atlas.typesystem.Struct;
import org.apache.atlas.typesystem.json.InstanceSerialization;
import org.apache.atlas.typesystem.persistence.Id;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.metastore.api.Database;
import org.apache.hadoop.hive.metastore.api.FieldSchema;
import org.apache.hadoop.hive.metastore.api.Index;
import org.apache.hadoop.hive.metastore.api.Order;
import org.apache.hadoop.hive.metastore.api.SerDeInfo;
import org.apache.hadoop.hive.metastore.api.StorageDescriptor;
import org.apache.hadoop.hive.metastore.api.hive_metastoreConstants;
import org.apache.hadoop.hive.ql.metadata.Hive;
import org.apache.hadoop.hive.ql.metadata.Partition;
import org.apache.hadoop.hive.ql.metadata.Table;
import org.apache.hadoop.security.UserGroupInformation;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * A Bridge Utility that imports metadata from the Hive Meta Store
 * and registers then in Atlas.
 */
public class HiveMetaDataGenerator {
	
	{
		System.setProperty("atlas.conf", "/Users/sdutta/Applications/conf");
	}
	
	
    private static final String DEFAULT_DGI_URL = "http://localhost:21000/";
    
    public static final String DEFAULT_CLUSTER_NAME = "primary";
    private static String clusterName = "atlasdemo";

    public static final String DGI_URL_PROPERTY = "hive.hook.dgi.url";

    private static final Logger LOG = LoggerFactory.getLogger(HiveMetaDataGenerator.class);

    private final Hive hiveClient = null;
    private  AtlasClient atlasClient;
    private static String databasename = null;
    private static String tablename = null;
    
    public static void main(String[] args) throws Exception {
    	
    	clusterName = args[1];
    	String baseurl = args[0];
    	databasename = args[2];
    	tablename = args[3];
    	
    	
    	
    	HiveMetaDataGenerator hmg = new HiveMetaDataGenerator(baseurl);
    	
    	Referenceable db = hmg.registerDatabase(databasename, clusterName);
    	hmg.registerTable(db, databasename, tablename);
    	
    }

    /**
     * 
     * @param baseurl
     */
    public HiveMetaDataGenerator(String baseurl) {
    	
    	atlasClient = new AtlasClient(baseurl);
    
    }
    
    
    public AtlasClient getAtlasClient() {
        return atlasClient;
    }


      

    public Referenceable registerDatabase(String databaseName, String clusterName) throws Exception {
        Referenceable dbRef = getDatabaseReference(databaseName, clusterName);
        
        if (dbRef == null) {
            LOG.info("Importing objects from databaseName : " + databaseName);
            //Database hiveDB = hiveClient.getDatabase(databaseName);

            dbRef = new Referenceable(HiveDataTypes.HIVE_DB.getName());
            dbRef.set(HiveDataModelGenerator.NAME, databaseName);
            dbRef.set(HiveDataModelGenerator.CLUSTER_NAME, clusterName);
            dbRef.set("description", "this is a default database");
            dbRef.set("locationUri", "/hive/default");
            dbRef.set("parameters", "key1=name1,key2=name2");
            dbRef.set("ownerName", "Hortonworks");
            dbRef = createInstance(dbRef);
            
            
        } else {
            LOG.info("Database {} is already registered with id {}", databaseName, dbRef.getId().id);
        }
        
        return dbRef;
    }

    public Referenceable createInstance(Referenceable referenceable) throws Exception {
        String typeName = referenceable.getTypeName();
        LOG.debug("creating instance of type " + typeName);

        String entityJSON = InstanceSerialization.toJson(referenceable, true);
        LOG.debug("Submitting new entity {} = {}", referenceable.getTypeName(), entityJSON);
        JSONObject jsonObject = atlasClient.createEntity(entityJSON);
        String guid = jsonObject.getString(AtlasClient.GUID);
        LOG.debug("created instance for type " + typeName + ", guid: " + guid);

        return new Referenceable(guid, referenceable.getTypeName(), null);
    }

   

    /**
     * Gets reference for the database
     *
     *
     * @param databaseName  database Name
     * @param clusterName    cluster name
     * @return Reference for database if exists, else null
     * @throws Exception
     */
    private Referenceable getDatabaseReference(String databaseName, String clusterName) throws Exception {
        LOG.debug("Getting reference for database {}", databaseName);
        String typeName = HiveDataTypes.HIVE_DB.getName();

        String dslQuery = String.format("%s where %s = '%s' and %s = '%s'", typeName, HiveDataModelGenerator.NAME,
                databaseName.toLowerCase(), HiveDataModelGenerator.CLUSTER_NAME, clusterName);
        return getEntityReferenceFromDSL(typeName, dslQuery);
    }

    public Referenceable getProcessReference(String queryStr) throws Exception {
        LOG.debug("Getting reference for process with query {}", queryStr);
        String typeName = HiveDataTypes.HIVE_PROCESS.getName();

        //todo enable DSL
        //        String dslQuery = String.format("%s where queryText = \"%s\"", typeName, queryStr);
        //        return getEntityReferenceFromDSL(typeName, dslQuery);

        String gremlinQuery =
                String.format("g.V.has('__typeName', '%s').has('%s.queryText', \"%s\").toList()", typeName, typeName,
                        StringEscapeUtils.escapeJava(queryStr));
        return getEntityReferenceFromGremlin(typeName, gremlinQuery);
    }

    private Referenceable getEntityReferenceFromDSL(String typeName, String dslQuery) throws Exception {
        AtlasClient dgiClient = getAtlasClient();
        JSONArray results = dgiClient.searchByDSL(dslQuery);
        if (results.length() == 0) {
            return null;
        } else {
            String guid;
            JSONObject row = results.getJSONObject(0);
            if (row.has("$id$")) {
                guid = row.getJSONObject("$id$").getString("id");
            } else {
                guid = row.getJSONObject("_col_0").getString("id");
            }
            return new Referenceable(guid, typeName, null);
        }
    }

    public static String getTableName(String clusterName, String dbName, String tableName) {
        return String.format("%s.%s@%s", dbName.toLowerCase(), tableName.toLowerCase(), clusterName);
    }

    /**
     * Gets reference for the table
     *
     * @param dbName database name
     * @param tableName table name
     * @return table reference if exists, else null
     * @throws Exception
     */
    private Referenceable getTableReference(String dbName, String tableName) throws Exception {
        LOG.debug("Getting reference for table {}.{}", dbName, tableName);

        String typeName = HiveDataTypes.HIVE_TABLE.getName();
        String entityName = getTableName(clusterName, dbName, tableName);
        String dslQuery = String.format("%s as t where name = '%s'", typeName, entityName);
        return getEntityReferenceFromDSL(typeName, dslQuery);
    }

    private Referenceable getEntityReferenceFromGremlin(String typeName, String gremlinQuery)
    throws AtlasServiceException, JSONException {
        AtlasClient client = getAtlasClient();
        JSONObject response = client.searchByGremlin(gremlinQuery);
        JSONArray results = response.getJSONArray(AtlasClient.RESULTS);
        if (results.length() == 0) {
            return null;
        }
        String guid = results.getJSONObject(0).getString("__guid");
        return new Referenceable(guid, typeName, null);
    }

    private Referenceable getPartitionReference(String dbName, String tableName, List<String> values) throws Exception {
        String valuesStr = "['" + StringUtils.join(values, "', '") + "']";
        LOG.debug("Getting reference for partition for {}.{} with values {}", dbName, tableName, valuesStr);
        String typeName = HiveDataTypes.HIVE_PARTITION.getName();

        //todo replace gremlin with DSL
        //        String dslQuery = String.format("%s as p where values = %s, tableName where name = '%s', "
        //                        + "dbName where name = '%s' and clusterName = '%s' select p", typeName, valuesStr,
        // tableName,
        //                dbName, clusterName);

        String datasetType = AtlasClient.DATA_SET_SUPER_TYPE;
        String tableEntityName = getTableName(clusterName, dbName, tableName);

        String gremlinQuery = String.format("g.V.has('__typeName', '%s').has('%s.values', %s).as('p')."
                        + "out('__%s.table').has('%s.name', '%s').back('p').toList()", typeName, typeName, valuesStr,
                typeName, datasetType, tableEntityName);

        return getEntityReferenceFromGremlin(typeName, gremlinQuery);
    }

    private Referenceable getSDForTable(String dbName, String tableName) throws Exception {
        Referenceable tableRef = getTableReference(dbName, tableName);
        if (tableRef == null) {
            throw new IllegalArgumentException("Table " + dbName + "." + tableName + " doesn't exist");
        }

        AtlasClient dgiClient = getAtlasClient();
        Referenceable tableInstance = dgiClient.getEntity(tableRef.getId().id);
        Id sdId = (Id) tableInstance.get("sd");
        return new Referenceable(sdId.id, sdId.getTypeName(), null);
    }

    /**
     * 
     * @param dbName
     * @param tableName
     * @return
     * @throws Exception
     */
    public Referenceable registerTable(String dbName, String tableName) throws Exception {
        Referenceable dbReferenceable = registerDatabase(dbName, clusterName);
        return registerTable(dbReferenceable, dbName, tableName);
    }

    
    /**
     * 
     * @param dbReference
     * @param dbName
     * @param tableName
     * @return
     * @throws Exception
     */
    public Referenceable registerTable(Referenceable dbReference, String dbName, String tableName) throws Exception {
        LOG.info("Attempting to register table [" + tableName + "]");
        Referenceable tableRef = getTableReference(dbName, tableName);
        
        if (tableRef == null) {
            LOG.info("Importing objects from " + dbName + "." + tableName);

            //Table hiveTable = hiveClient.getTable(dbName, tableName);

            tableRef = new Referenceable(HiveDataTypes.HIVE_TABLE.getName());
            tableRef.set(HiveDataModelGenerator.NAME,
                    getTableName(clusterName, dbName, tableName));
            
            tableRef.set(HiveDataModelGenerator.TABLE_NAME,tableName.toLowerCase());
            tableRef.set("owner", "Hortonworks");

            tableRef.set("createTime", System.currentTimeMillis());
            tableRef.set("lastAccessTime",System.currentTimeMillis());
            tableRef.set("retention", System.currentTimeMillis());

            tableRef.set(HiveDataModelGenerator.COMMENT, "This is loaded by Sqoop job");

            // add reference to the database
            tableRef.set(HiveDataModelGenerator.DB, dbReference);
            
            List<Referenceable> timeDimColumns = ImmutableList
                    .of(rawColumn("driver_id", "String", "Driver Id"), rawColumn("driver_name", "String", "Driver Name"),
                            rawColumn("certified", "String", "certified_Y/N"), rawColumn("wageplan", "String", "hours of weekly"));
            
            
            tableRef.set("columns", timeDimColumns);
            
            // add reference to the StorageDescriptor
            //StorageDescriptor storageDesc = hiveTable.getSd();
            //Referenceable sdReferenceable = fillStorageDescStruct(storageDesc, colList);
            //tableRef.set("sd", sdReferenceable);

            // add reference to the Partition Keys
            //List<Referenceable> partKeys = getColumns(hiveTable.getPartitionKeys());
            //tableRef.set("partitionKeys", partKeys);

           // tableRef.set("parameters", "params");

            
            tableRef.set("viewOriginalText", "Original text");
           

            
           tableRef.set("viewExpandedText", "Expanded Text");
            

            tableRef.set("tableType", "Sqoop generated table");
            tableRef.set("temporary", "false");


            tableRef = createInstance(tableRef);
            
        } else {
            LOG.info("Table {}.{} is already registered with id {}", dbName, tableName, tableRef.getId().id);
        }
        return tableRef;
    }

    
    /**
     * 
     * @param db
     * @param tableName
     * @param tableReferenceable
     * @param sdReferenceable
     * @throws Exception
     */
    private void registerPartitions(String db, String tableName, Referenceable tableReferenceable,
            Referenceable sdReferenceable) throws Exception {
        Set<Partition> tableParts = hiveClient.getAllPartitionsOf(new Table(Table.getEmptyTable(db, tableName)));

        if (tableParts.size() > 0) {
            for (Partition hivePart : tableParts) {
                registerPartition(hivePart, tableReferenceable, sdReferenceable);
            }
        }
    }

    /**
     * 
     * @param partition
     * @return
     * @throws Exception
     */
    public Referenceable registerPartition(Partition partition) throws Exception {
        String dbName = partition.getTable().getDbName();
        String tableName = partition.getTable().getTableName();
        Referenceable tableRef = registerTable(dbName, tableName);
        Referenceable sdRef = getSDForTable(dbName, tableName);
        return registerPartition(partition, tableRef, sdRef);
    }

    private Referenceable registerPartition(Partition hivePart, Referenceable tableReferenceable,
            Referenceable sdReferenceable) throws Exception {
        LOG.info("Registering partition for {} with values {}", tableReferenceable,
                StringUtils.join(hivePart.getValues(), ","));
        String dbName = hivePart.getTable().getDbName();
        String tableName = hivePart.getTable().getTableName();

        Referenceable partRef = getPartitionReference(dbName, tableName, hivePart.getValues());
        if (partRef == null) {
            partRef = new Referenceable(HiveDataTypes.HIVE_PARTITION.getName());
            partRef.set("values", hivePart.getValues());

            partRef.set(HiveDataModelGenerator.TABLE, tableReferenceable);

            //todo fix
            partRef.set("createTime", hivePart.getLastAccessTime());
            partRef.set("lastAccessTime", hivePart.getLastAccessTime());

            // sdStruct = fillStorageDescStruct(hivePart.getSd());
            // Instead of creating copies of the sdstruct for partitions we are reusing existing
            // ones will fix to identify partitions with differing schema.
            partRef.set("sd", sdReferenceable);

            partRef.set("parameters", hivePart.getParameters());
            partRef = createInstance(partRef);
        } else {
            LOG.info("Partition {}.{} with values {} is already registered with id {}", dbName, tableName,
                    StringUtils.join(hivePart.getValues(), ","), partRef.getId().id);
        }
        return partRef;
    }

    private void importIndexes(String db, String table, Referenceable dbReferenceable, Referenceable tableReferenceable)
    throws Exception {
        List<Index> indexes = hiveClient.getIndexes(db, table, Short.MAX_VALUE);
        if (indexes.size() > 0) {
            for (Index index : indexes) {
                importIndex(index, dbReferenceable, tableReferenceable);
            }
        }
    }

    //todo should be idempotent
    private void importIndex(Index index, Referenceable dbReferenceable, Referenceable tableReferenceable)
            throws Exception {
        LOG.info("Importing index {} for {}.{}", index.getIndexName(), dbReferenceable, tableReferenceable);
        Referenceable indexRef = new Referenceable(HiveDataTypes.HIVE_INDEX.getName());

        indexRef.set(HiveDataModelGenerator.NAME, index.getIndexName());
        indexRef.set("indexHandlerClass", index.getIndexHandlerClass());

        indexRef.set(HiveDataModelGenerator.DB, dbReferenceable);

        indexRef.set("createTime", index.getCreateTime());
        indexRef.set("lastAccessTime", index.getLastAccessTime());
        indexRef.set("origTable", index.getOrigTableName());
        indexRef.set("indexTable", index.getIndexTableName());

        Referenceable sdReferenceable = fillStorageDescStruct(index.getSd(), null);
        indexRef.set("sd", sdReferenceable);

        indexRef.set("parameters", index.getParameters());

        tableReferenceable.set("deferredRebuild", index.isDeferredRebuild());

        createInstance(indexRef);
    }

    private Referenceable fillStorageDescStruct(StorageDescriptor storageDesc, List<Referenceable> colList)
    throws Exception {
        LOG.debug("Filling storage descriptor information for " + storageDesc);

        Referenceable sdReferenceable = new Referenceable(HiveDataTypes.HIVE_STORAGEDESC.getName());

        SerDeInfo serdeInfo = storageDesc.getSerdeInfo();
        LOG.debug("serdeInfo = " + serdeInfo);
        // SkewedInfo skewedInfo = storageDesc.getSkewedInfo();

        String serdeInfoName = HiveDataTypes.HIVE_SERDE.getName();
        Struct serdeInfoStruct = new Struct(serdeInfoName);

        serdeInfoStruct.set(HiveDataModelGenerator.NAME, serdeInfo.getName());
        serdeInfoStruct.set("serializationLib", serdeInfo.getSerializationLib());
        serdeInfoStruct.set("parameters", serdeInfo.getParameters());

        sdReferenceable.set("serdeInfo", serdeInfoStruct);
        sdReferenceable.set(HiveDataModelGenerator.STORAGE_NUM_BUCKETS, storageDesc.getNumBuckets());
        sdReferenceable
                .set(HiveDataModelGenerator.STORAGE_IS_STORED_AS_SUB_DIRS, storageDesc.isStoredAsSubDirectories());

        //Use the passed column list if not null, ex: use same references for table and SD
        List<FieldSchema> columns = storageDesc.getCols();
        if (columns != null && !columns.isEmpty()) {
            if (colList != null) {
                sdReferenceable.set("cols", colList);
            } else {
                sdReferenceable.set("cols", getColumns(columns));
            }
        }

        List<Struct> sortColsStruct = new ArrayList<>();
        for (Order sortcol : storageDesc.getSortCols()) {
            String hiveOrderName = HiveDataTypes.HIVE_ORDER.getName();
            Struct colStruct = new Struct(hiveOrderName);
            colStruct.set("col", sortcol.getCol());
            colStruct.set("order", sortcol.getOrder());

            sortColsStruct.add(colStruct);
        }
        if (sortColsStruct.size() > 0) {
            sdReferenceable.set("sortCols", sortColsStruct);
        }

        sdReferenceable.set("location", storageDesc.getLocation());
        sdReferenceable.set("inputFormat", storageDesc.getInputFormat());
        sdReferenceable.set("outputFormat", storageDesc.getOutputFormat());
        sdReferenceable.set("compressed", storageDesc.isCompressed());

        if (storageDesc.getBucketCols().size() > 0) {
            sdReferenceable.set("bucketCols", storageDesc.getBucketCols());
        }

        sdReferenceable.set("parameters", storageDesc.getParameters());
        sdReferenceable.set("storedAsSubDirectories", storageDesc.isStoredAsSubDirectories());

        return createInstance(sdReferenceable);
    }

    private List<Referenceable> getColumns(List<FieldSchema> schemaList) throws Exception {
        List<Referenceable> colList = new ArrayList<>();
        for (FieldSchema fs : schemaList) {
            LOG.debug("Processing field " + fs);
            Referenceable colReferenceable = new Referenceable(HiveDataTypes.HIVE_COLUMN.getName());
            colReferenceable.set(HiveDataModelGenerator.NAME, fs.getName());
            colReferenceable.set("type", fs.getType());
            colReferenceable.set(HiveDataModelGenerator.COMMENT, fs.getComment());

            colList.add(createInstance(colReferenceable));
        }
        return colList;
    }

    public synchronized void registerHiveDataModel() throws Exception {
        HiveDataModelGenerator dataModelGenerator = new HiveDataModelGenerator();
        AtlasClient dgiClient = getAtlasClient();

        //Register hive data model if its not already registered
        if (dgiClient.getType(HiveDataTypes.HIVE_PROCESS.getName()) == null) {
            LOG.info("Registering Hive data model");
            dgiClient.createType(dataModelGenerator.getModelAsJson());
        } else {
            LOG.info("Hive data model is already registered!");
        }
    }

    

	Referenceable rawColumn(String name, String dataType, String comment, String... traitNames) throws Exception {
	        Referenceable referenceable = new Referenceable(HiveDataTypes.HIVE_COLUMN.getName(), traitNames);
	        referenceable.set("name", name);
	        referenceable.set("type", dataType);
	        referenceable.set("comment", comment);

	        return referenceable;
	    }
	  
	
   
    public void updateTable(Referenceable tableReferenceable, Table newTable) throws AtlasServiceException {
        AtlasClient client = getAtlasClient();
        client.updateEntity(tableReferenceable.getId()._getId(), HiveDataModelGenerator.TABLE_NAME,
                newTable.getTableName().toLowerCase());
        client.updateEntity(tableReferenceable.getId()._getId(), HiveDataModelGenerator.NAME,
                getTableName(clusterName, newTable.getDbName(), newTable.getTableName()));
    }
}