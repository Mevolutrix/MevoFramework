import mevolutrix.serviceMediator.{SAEReqTaskProcessor, SAEServer}
import Mevolutrix.portal.{PortalServer, RESTSvcHandler}
import java.sql.{Connection, SQLException, Statement}
import MySQLStore.operationExecutor.HsTaskProcessor
import EntityStore.Communication.EntityStoreServer
import Mevolutrix.ServiceMgmt.ServiceManager
import mevolutrix.ServiceRT.SVCConfig
import Mevolutrix.serviceBusEngine._
import EntityInterface.EntityType
import mevolutrix.engineConfig._
import EntityStore.Metadata._

object RunApp extends App {
  def withExecuteTime(func: () => Unit, prompt: String) = {
    val beforeTime = System.nanoTime()
    func()
    val executeTime = (System.nanoTime() - beforeTime) / 1000
    println(prompt + executeTime + "us")
  }

  implicit val system = SBEServiceAccess.ServiceConfig.appSystem
  EntityStoreServer.start(HsTaskProcessor)
  SAEServer.start(SAEReqTaskProcessor)
  PortalServer.start(classOf[RESTSvcHandler])
  println("Press \"q\" to quit:")
  var s: String = " "
  do {
    s = io.StdIn.readLine()

    s match {
      case "i" => {
        createDefaultTable()
        initAppSpace()
        initSchema()
        initSet()
        println("deploy db :" + MetadataManager.deploySets("System.Metadata"))
        println("deploy db :" + MetadataManager.deploySets("System.Configuration"))
        initSMEConfig()
      }
      /*case "t" => {
        printf("test set begin")
        val setInfo: IEntitySet = (new EntitySet {
          entitySetName = "System.Metadata.DefaultValueTest"
          appSpaceId = "System.Metadata"
          description = "DefaultValueTest"
          pKey = "entitySetName"
          pkeyType = CSONTypes.UTF8String.id.toByte
          index = Array[EntityQueryMark](new EntityQueryMark() {
            path = "appSpaceId"
            direct = true
            markType = IndexType.Default
            indexDataType = pkeyType
          })
          defaultValues = Array[PropertyDefaultValue](new PropertyDefaultValue() {
            name = "testDate"
            dType = Preset
            defaultValue = null
            evalExp = null
          })
          _storageType = StorageType.Shared
          autoValue = ""
          minValueOfPKey = ""
        }).populate

        println("Set for SetMetadata :" + MetadataManager.createSet("System.Metadata.DefaultValueTest", setInfo))
        println("create set:" + MetadataManager.deploySets("System.Metadata.DefaultValueTest"))
      }*/
      case "q" => PortalServer.stop; system.shutdown()
      case _ => println("Press \"q\" to quit:")
    }

  } while (s!="q")


  def initAppSpace() = {
    println("AppInfo MDE:" + MetadataManager.addAppSpace("MDE", "System.Metadata",
      System_Definition_Metadata.MDE_AppSpaceInfo.storeConfig))
    println("AppInfo CFG:" + MetadataManager.addAppSpace("CFG", "System.Configuration",
      System_Definition_Metadata.CFG_AppSpaceInfo.storeConfig))
    println("AppInfo CMS:" + MetadataManager.addAppSpace("CMS", "Content.MgmtSystem",
      System_Definition_Metadata.CMS_AppSpaceInfo.storeConfig))
    println("AppInfo XEDU:" + MetadataManager.addAppSpace("XEDU", "Content.XEDU",
      System_Definition_Metadata.XEDU_AppSpaceInfo.storeConfig))
  }

  def createDefaultTable() = {
    val storeConfig = System_Definition_Metadata.MDE_AppSpaceInfo.storeConfig
    val (conn: Connection, stmt: Statement) = DBOperation.DBOperator.getStatement(
      storeConfig.dbConnString, storeConfig.usr, storeConfig.pwd)
    val setStmt = """CREATE TABLE `MevoSystem`.`System_Metadata_EntitySet` (""" +
      """`entitySetName` varchar(160) NOT NULL,`appSpaceId` varchar(40) NOT NULL,""" +
      """`_raw_DATA_` TEXT NOT NULL, PRIMARY KEY (`entitySetName`)""" +
      """) ENGINE=TokuDB DEFAULT CHARSET=utf8"""
    val schemaStmt = """CREATE TABLE `MevoSystem`.`System_Metadata_EntitySchema` (""" +
      """`id` varchar(160) NOT NULL,""" +
      """`appSpaceId` varchar(40) NOT NULL,""" +
      """`_raw_DATA_` TEXT NOT NULL,""" +
      """PRIMARY KEY (`id`)""" +
      """) ENGINE=TokuDB DEFAULT CHARSET=utf8"""

    val setUpdateRecordStmt = """CREATE TABLE `MevoSystem`.`System_Metadata_SetUpdateRecord` (""" +
      """`id` BIGINT NOT NULL,`appSpace` VARCHAR(160) NOT NULL,`setName` VARCHAR(160) NOT NULL,""" +
      """`_raw_DATA_` TEXT NOT NULL, PRIMARY KEY (`id`)) ENGINE=TokuDB DEFAULT CHARSET=utf8"""
    val appSpaceStmt = """CREATE TABLE `MevoSystem`.`System_Metadata_AppSpaceInfo` (""" +
      """`alias` varchar(40) NOT NULL,`appSpaceName` VARCHAR(160) NOT NULL,`_raw_DATA_` TEXT NOT NULL,""" +
      """PRIMARY KEY (`alias`)) ENGINE=TokuDB DEFAULT CHARSET=utf8"""
    val newTables = Array[String](setStmt, schemaStmt, setUpdateRecordStmt, appSpaceStmt)
    newTables.foreach(createSt => {
      try {
        println("Create Set:" + createSt)
        stmt.execute(createSt)
      }
      catch {
        case e: SQLException => println(e.getMessage, e)
      }
    })
    if (stmt != null) stmt.close()
    if (conn != null) conn.close()
    System.console().readLine()
  }

  def initSet() = {
    println("Set for EntitySet :" + MetadataManager.createSet(EntitySet.setInfo.setName, EntitySet.setInfo))
    println("Set for EntitySchema :" + MetadataManager.createSet(EntitySchema.setInfo.setName, EntitySchema.setInfo))
    println("Set for SetMetadata :" + MetadataManager.createSet(SetMetadata.setInfo.setName, SetMetadata.setInfo))
    println("Set for SMEConfig :" + MetadataManager.createSet(SMEConfig.smeSetInfo.setName, SMEConfig.smeSetInfo))
    println("Set for DSEConfig :" + MetadataManager.createSet(DSESvcConfig.setInfo.setName, DSESvcConfig.setInfo))
    println("Set for SVCConfig :" + MetadataManager.createSet(SVCConfig.setInfo.setName, SVCConfig.setInfo))
    println("Set for Service access Config :" + MetadataManager.createSet(
      ServiceManager.setInfo.setName, ServiceManager.setInfo))
    println("Set for Property Reference :" + MetadataManager.createSet(
      PropertyReference.setInfo.setName, PropertyReference.setInfo))
    println("Set for ValidationRule :"+ MetadataManager.createSet(
      EntitySchema.validationRuleSetInfo.setName,EntitySchema.validationRuleSetInfo))
    println("Set for AutoValueKey :"+ MetadataManager.createSet(
      AutoValueKey.autoKeySetInfo.setName,AutoValueKey.autoKeySetInfo))
  }

  def initSchema() = {
    println("Schema for EntitySet :" + MetadataManager.createSchema((EntitySet.schemaInfo.entityId, EntityType.Data), EntitySet.schemaInfo))
    println("Schema for EntityQueryMark :" + MetadataManager.createSchema((EntitySet.IndexSchema.entityId, EntityType.View), EntitySet.IndexSchema))
    println("Schema for defaultValue :" + MetadataManager.createSchema((EntitySet.defaultValueSchema.entityId, EntityType.View), EntitySet.defaultValueSchema))
    println("Schema for EntitySchema :" + MetadataManager.createSchema((EntitySchema.schemaInfo.entityId, EntityType.Data), EntitySchema.schemaInfo))
    println("Schema for property :" + MetadataManager.createSchema((EntitySchema.propertySchema.entityId, EntityType.View), EntitySchema.propertySchema))
    println("Schema for propertyReference :" + MetadataManager.createSchema((PropertyReference.schemaInfo.entityId, EntityType.Data), PropertyReference.schemaInfo))
    println("Schema for propertyMap :" + MetadataManager.createSchema((PropertyReference.propertyMapSchemaInfo.entityId, EntityType.View), PropertyReference.propertyMapSchemaInfo))
    println("Schema for AppSpaceInfo :" + MetadataManager.createSchema((AppSpaceInfo.appSpaceConfigSchemaInfo.entityId, EntityType.Data), AppSpaceInfo.appSpaceConfigSchemaInfo))
    println("Schema for StoreConfig :" + MetadataManager.createSchema((AppSpaceInfo.storeConfigSchemaInfo.entityId, EntityType.ComplexType), AppSpaceInfo.storeConfigSchemaInfo))
    println("Schema for SetMetadata :" + MetadataManager.createSchema((SetMetadata.schemaInfo.entityId, EntityType.Data), SetMetadata.schemaInfo))
    println("Schema for SetUpdateRecord :" + MetadataManager.createSchema((SetUpdateRecord.schemaInfo.entityId, EntityType.Data), SetUpdateRecord.schemaInfo))
    println("Schema for SMEConfig :" + MetadataManager.createSchema( (SMEConfig.schemaInfo.entityId, EntityType.Data), SMEConfig.schemaInfo))
    println("Schema for DSEConfig :" + MetadataManager.createSchema((DSESvcConfig.schemaInfo.entityId, EntityType.Data), DSESvcConfig.schemaInfo))
    println("Schema for SVCConfig :" + MetadataManager.createSchema((SVCConfig.schemaInfo.entityId, EntityType.Data), SVCConfig.schemaInfo))
    println("Schema for Service access Config :" + MetadataManager.createSchema(
      (ServiceManager.schemaInfo.entityId, EntityType.Data), ServiceManager.schemaInfo))
    println("Schema for ValidationRule :" + MetadataManager.createSchema(
      (EntitySchema.validationRuleSchema.entityId, EntityType.Data), EntitySchema.validationRuleSchema))
    println("Schema for autoValueKey :" + MetadataManager.createSchema(
      (AutoValueKey.autoKeySchemaInfo.entityId, EntityType.Data), AutoValueKey.autoKeySchemaInfo))
  }

  def initSMEConfig() = {
    println("SME for Metadata :" + SMEConfig.addSMEConfig("MDE", SMEConfig.metadataSMEInfo))
  }
}
