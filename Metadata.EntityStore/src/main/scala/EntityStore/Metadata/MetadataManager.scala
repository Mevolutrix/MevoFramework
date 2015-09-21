package EntityStore.Metadata
import java.util
import DBOperation.DBOperator
import EntityAccess._
import EntityInterface.EntityType
import EntityInterface._
import akka.util.Timeout
import CSON.Types.{CSONTypesArray, CSONTypes}
import EntityStore.Interface._
import com.mysql.jdbc.exceptions.MySQLSyntaxErrorException
import scala.concurrent.duration._
import SBEServiceAccess.CallDSE
import java.sql.{Statement, Connection}
import java.util.concurrent.ConcurrentHashMap
import CSON.{CSONComplexElement, CSONElementArray, CSONDocument}

class FutureCache[K,V](loader:(K)=>V,populator:(K,V)=>Unit=null) {
  implicit val timeout = Timeout(15.seconds)
  private val cache = new ConcurrentHashMap[K,V]()
  def size():Int =cache.size()
  def get(k:K):V = {
    if (cache.containsKey(k)) cache.get(k)
    else {
      val ret = loader(k)
      cache.put(k,ret)
      if (populator!=null)populator(k,ret)
      ret
    }
  }
  def set(k:K, v:V) = cache.put(k,v)
}
object MetadataManager {
  import EntityType._
  import org.slf4j.{LoggerFactory, Logger}
  var log: Logger = LoggerFactory.getLogger("MetadataManager")

  val system_AppSpace = "System.Metadata"
  val schemaSerializer = GeneralEntityToCSON(classOf[EntitySchema])
  val setSerializer = GeneralEntityToCSON(classOf[EntitySet])
  val asSerializer = GeneralEntityToCSON(classOf[AppSpaceInfo])
  val autoKeySerializer = GeneralEntityToCSON(classOf[AutoValueKey])
  val schemaForLoadAppSet = new DynamicSchema("schemaForLoadAppSet")
  schemaForLoadAppSet.add("id", CSONTypesArray.CSONElementTypes(CSONTypes.UTF8String.id))
  val schemaForLoadAppSchema = new DynamicSchema("schemaForLoadAppSchema")
  schemaForLoadAppSchema.add("entitySetName", CSONTypesArray.CSONElementTypes(CSONTypes.UTF8String.id))

  private val schemaCache = new FutureCache[(String, EntityType), EntitySchema](loadSchema,
    (key,v)=>{
      // Populate here! Otherwise it will cause loop when someone define a recursive schema
      v.populate(key._2)
    })
  private val entitySetCache = new FutureCache[String, EntitySet](loadEntitySet)
  private val appSpaceCache = new FutureCache[String, AppSpaceInfo](loadAppSpace)

  private def getResult(input: Any): Boolean = {
    input match {
      case a: util.ArrayList[CSONDocument] => true
      case err:(Int,Long) => log.error("Return error number:"+err._2); false
      case _ => false
    }
  }

  private def loadSchema(schemaKey:(String,EntityType)):EntitySchema = {
    log.info("Load entitySchema for:"+schemaKey)
    (schemaKey._1 match {
      case "System.Metadata.EntitySchema" => EntitySchema.schemaInfo
      case "System.Metadata.EntityProperty" =>EntitySchema.propertySchema
      case "System.Metadata.EntitySet" => EntitySet.schemaInfo
      case "System.Metadata.SetMetadata" => SetMetadata.schemaInfo
      case "System.Metadata.SetUpdateRecord" => SetUpdateRecord.schemaInfo
      case "System.Metadata.AutoValueKey" => AutoValueKey.autoKeySchemaInfo
      case "System.Metadata.AppSpaceInfo" => AppSpaceInfo.appSpaceConfigSchemaInfo
      case "System.Metadata.ValidationRule" => EntitySchema.validationRuleSchema
      case "System.Metadata.EntityQueryMark" => EntitySet.IndexSchema
      case "System.Metadata.PropertyReference" => PropertyReference.schemaInfo
      case _ => CallDSE(LoadEntityByKey(schemaKey._1,system_AppSpace,
        EntitySchema.setInfo.setName,EntitySchema.schemaInfo),classOf[EntitySchema])
    }).asInstanceOf[EntitySchema] // DO NOT populate here! Otherwise it will cause loop when someone define a recursive schema
  }
  private def loadEntitySet(setKey:String):EntitySet = {
    log.info("Load entitySet for:"+setKey)
    (setKey match {
      case "System.Metadata.EntitySchema" => EntitySchema.setInfo
      case "System.Metadata.EntitySet" => EntitySet.setInfo
      case "System.Metadata.SetMetadata" => SetMetadata.setInfo
      case "System.Metadata.AutoValueKey" => AutoValueKey.autoKeySetInfo
      case "System.Metadata.SetUpdateRecord" => SetUpdateRecord.setInfo
      case "System.Metadata.AppSpaceInfo" => AppSpaceInfo.setInfo
      case "System.Metadata.ValidationRule" => EntitySchema.validationRuleSetInfo
      case "System.Metadata.PropertyReference" => PropertyReference.setInfo
      case _ =>
        CallDSE(LoadEntityByKey(setKey,system_AppSpace,EntitySet.setInfo.setName,
          EntitySet.schemaInfo),classOf[EntitySet])
    }).asInstanceOf[EntitySet].populate
  }
  private def loadAppSpace(alias:String):AppSpaceInfo = alias match {
    case "MDE"|"System.Metadata" => System_Definition_Metadata.MDE_AppSpaceInfo
    case "CFG"|"System.Configuration" => System_Definition_Metadata.CFG_AppSpaceInfo
    case _ =>
      if (alias.contains('.')) {
        CallDSE(EntityQuery("appSpaceName=@0",Array[Object](alias),null,"MDE",system_AppSpace,AppSpaceInfo.
          setInfo.setName,AppSpaceInfo.appSpaceConfigSchemaInfo),classOf[AppSpaceInfo]).asInstanceOf[AppSpaceInfo]
      } else CallDSE(LoadEntityByKey(alias, system_AppSpace, AppSpaceInfo.setInfo.setName,
        AppSpaceInfo.appSpaceConfigSchemaInfo), classOf[AppSpaceInfo]).asInstanceOf[AppSpaceInfo]
  }

  def getAllSchemaOfApp(appSpaceName:String):IndexedSeq[String] = {
    val ret = ResultFormat.getCSONResult(CallDSE(EntityQuery("appSpaceId=@0",
      Array[Object](appSpaceName),"id","MDE",system_AppSpace, EntitySchema.setInfo.setName,
      EntitySchema.schemaInfo)).asInstanceOf[util.ArrayList[CSONDocument]],schemaForLoadAppSet)
    for (i<-0 until ret.length) yield {
      ret(i).getValue("id").asInstanceOf[String]
    }
  }
  def getAllSetOfApp(appSpaceName:String):IndexedSeq[String] = {
    val ret = ResultFormat.getCSONResult(CallDSE(EntityQuery("appSpaceId=@0",Array[Object](appSpaceName),
      "entitySetName","MDE",system_AppSpace,EntitySet.setInfo.setName,EntitySet.schemaInfo)).
      asInstanceOf[util.ArrayList[CSONDocument]],schemaForLoadAppSchema)
    for (i<-0 until ret.length) yield {
      val setName=ret(i).getValue("entitySetName").asInstanceOf[String]
      log.debug("Load Schema for appSpace:{}",  setName)
      setName
    }
  }
  def getSchema(schemaId: String, metaType: EntityType = EntityType.Data):IEntitySchema =
    schemaCache.get((schemaId,metaType))

  def createSchema(id:(String,EntityType),schemaData:IEntitySchema) = {
    schemaCache.set(id,schemaData.asInstanceOf[EntitySchema])
    val insertItem = schemaSerializer.getObjectCsonBinary(schemaData,null)._1
    getResult(CallDSE(CreateEntity(id._1,insertItem,system_AppSpace,EntitySchema.schemaInfo,
      EntitySchema.setInfo.setName)))
  }
  def updateSchema(id:(String,EntityType),schemaData:IEntitySchema,updateOperator:(EntitySchema)=>EntitySchema = null) = {
    val updateItem : EntitySchema = if (schemaData==null) {
      val es = (schemaCache.get(id))
      updateOperator(EntitySchema.clone(es, es.entityType))
    } else schemaData.asInstanceOf[EntitySchema]
    schemaCache.set(id,updateItem)

    val item = schemaSerializer.getObjectCsonBinary(updateItem,null)._1
    getResult(CallDSE(UpdateEntity(id._1,item,system_AppSpace,EntitySchema.schemaInfo,
      EntitySchema.setInfo.setName)))
  }

  def getEntitySet(entitySetName: String): IEntitySet = entitySetCache.get(entitySetName)
  def createSet(setName:String,setData:IEntitySet) = {
    entitySetCache.set(setName,setData.asInstanceOf[EntitySet])
    log.debug("CreatSet to store JSON:"+JSONSerializer(setData))
    val binary = setSerializer.getObjectCsonBinary(setData,null)._1

    val result=getResult(CallDSE(CreateEntity(setName,binary,system_AppSpace,EntitySet.schemaInfo,  EntitySet.setInfo.setName)))

    log.debug(" return result is: {} ",result)
    if (result)
      SetUpdateRecord.insertSet(setData.appSpace,setName,setData.asInstanceOf[EntitySet])
    else false
  }
  def updateSet(setName:String,setData:IEntitySet,updator:(EntitySet)=>EntitySet = null): Boolean = {
    val updateItem = if (setData!=null) setData
    else updator(entitySetCache.get(setName))
    entitySetCache.set(setName,updateItem.asInstanceOf[EntitySet])
    val item = setSerializer.getObjectCsonBinary(updateItem,null)._1

    getResult(CallDSE(UpdateEntity(setName,item,system_AppSpace,EntitySet.schemaInfo,
      EntitySet.setInfo.setName)))
  }
  def getAppSpace(alias:String):AppSpaceInfo = appSpaceCache.get(alias)
  def addAppSpace(id:String,appSpace:String,appSpaceConfig:StoreConfiguration): Boolean = {
    val ret = new AppSpaceInfo(){alias=id;appSpaceName=appSpace;storeConfig=appSpaceConfig}
    appSpaceCache.set(id,ret)
    val binary = asSerializer.getObjectCsonBinary(ret,null)._1
    getResult(CallDSE(CreateEntity(id,binary,system_AppSpace,AppSpaceInfo.appSpaceConfigSchemaInfo,
      AppSpaceInfo.setInfo.setName)))
  }
  def deploySets(alias:String,setName:String=null) = {
    def deployUpdateInfo(doc:CSONDocument):String = {
      def deployIndexInfo(doc:CSONDocument,sb:StringBuilder) = {
        val indexArr = doc.getValue("indexUpdated")
        if (indexArr!=null) {
          val indexList = indexArr.asInstanceOf[CSONElementArray]
          for (i<-0 until indexList.length) {
            val index = indexList.getValue(i).asInstanceOf[CSONComplexElement]
            sb.append("`").append(index.getValue("path").asInstanceOf[String]).append("` ").
              append(getFieldType(index.getValue("indexDataType").asInstanceOf[Byte].toInt)).
              append(" NOT NULL, ")
          }
        }
      }
      val sb = new StringBuilder()
      sb.append("CREATE TABLE IF NOT EXISTS ").append("`").
        append(doc.getValue("setName").asInstanceOf[String].replace('.','_')).
        append("` ( ")
      sb.append("`").append(doc.getValue("pKey").asInstanceOf[String]).append("` ").
        append(getFieldType(doc.getValue("primaryKeyType").asInstanceOf[Byte].toInt))
      sb.append(" NOT NULL, ")
      deployIndexInfo(doc,sb)
      if (!doc.getValue("setIsFlat").asInstanceOf[Boolean])
        sb.append("`_raw_DATA_` TEXT NOT NULL, ")
      sb.append("PRIMARY KEY (`").append(doc.getValue("pKey").asInstanceOf[String]).append("`) ")
        .append(") ENGINE=TokuDB DEFAULT CHARSET=utf8")
      //  .append(") ENGINE=MEMORY DEFAULT CHARSET=utf8")
      sb.result()
    }
    def getFieldType(typeCode:Int):String = CSONTypes(typeCode) match {
      case CSONTypes.Boolean|CSONTypes.Int8 => "TINYINT"
      case CSONTypes.Int16|CSONTypes.Int32 => "INT"
      case CSONTypes.Int64 => "BIGINT"
      case CSONTypes.FloatingPoint => "DOUBLE"
      case CSONTypes.Single => "FLOAT"
      case CSONTypes.Decimal => "DECIMAL[64,5]"
      case CSONTypes.ObjectId => "VARCHAR(36)"
      case CSONTypes.BinaryData => "TEXT"
      case CSONTypes.UTF8String => "VARCHAR(160)"
      case CSONTypes.Timestamp|CSONTypes.UTCDatetime => "VARCHAR(20)"
    }
    val appInfo = MetadataManager.getAppSpace(alias)
    val (conn: Connection, stmt: Statement) = DBOperator.getStatement(
      appInfo.storeConfig.dbConnString, appInfo.storeConfig.usr, appInfo.storeConfig.pwd)
    val updateList = SetUpdateRecord.getSetUpdateInfo(appInfo.appSpaceName)
    val autoKeySets = new util.ArrayList[String]()
    val retS = new StringBuilder()
    updateList.foreach(doc =>{
      try {
        val targetSetName = doc.getValue("setName").asInstanceOf[String]
        if (setName==null || targetSetName==setName) {
          val createSt = deployUpdateInfo(doc)
          log.info("Create Set:{} ", createSt)

          retS.append("{\"setName\":\"" + targetSetName + "\"},")
          val autoKeyName = getEntitySet(targetSetName).asInstanceOf[EntitySet].autoValue
          if (autoKeyName != null && !autoKeyName.isEmpty) {
            autoKeySets.add(targetSetName)
            log.info("Add auto value for:" + targetSetName)
          }

          stmt.execute(createSt)
        }
      }
      catch {
        case e:MySQLSyntaxErrorException => log.error(e.getMessage)
        case e:Exception => log.error(e.getMessage,e)
      }
    })
    if (retS.length>0) retS.deleteCharAt(retS.length-1)
    if (stmt!=null) stmt.close()
    if (conn!=null) conn.close()

    deployAutoKeyforSets(autoKeySets)
    retS.insert(0,'[')
    retS.append("]")
    retS.result()
  }
  private def deployAutoKeyforSets(sets:util.ArrayList[String]) = {
    if (sets.size()>0) {
      val appInfo = MetadataManager.getAppSpace("CFG")
      DBOperator.executeOperation(appInfo.storeConfig.dbConnString,
        appInfo.storeConfig.usr, appInfo.storeConfig.pwd,(conn)=>{
          val getStmt = conn.prepareStatement("SELECT * from System_Configuration_AutoValueKey where fullSetName=?")
          val pstmt = conn.prepareStatement("INSERT INTO System_Configuration_AutoValueKey VALUES(?,0,0)")
          for(i<-0 until sets.size()) {
            val setName = sets.get(i)
            getStmt.setString(1,setName)
            val rs = getStmt.executeQuery()
            if (!rs.next()) {
              pstmt.setString(1, setName)
              val ret = pstmt.executeUpdate()
              log.info("Insert autoValueKey for:" + setName + " result: " + ret)
            }
            rs.close()
          }
          pstmt.close()
          true
        },false)
    }
  }
}

object System_Definition_Metadata {
  import StoreType._
  val MDE_AppSpaceInfo = new AppSpaceInfo() {alias="MDE";appSpaceName="System.Metadata";storeConfig =
    new StoreConfiguration() {hostName="dseServer";storeType=dataBase;dbConnString="dseServer/MevoSystem";
      usr="root";pwd="123456"}
  }
  val CFG_AppSpaceInfo = new AppSpaceInfo() {alias="CFG";appSpaceName="System.Configuration";storeConfig =
    new StoreConfiguration() {hostName="dseServer";storeType=dataBase;dbConnString="dseServer/MevoSystem";
      usr="root";pwd="123456"}
  }
  val CMS_AppSpaceInfo = new AppSpaceInfo() {alias="CMS";appSpaceName="Content.MgmtSystem";storeConfig =
    new StoreConfiguration() {hostName="dseServer";storeType=dataBase;dbConnString="dseServer/CMS";
      usr="root";pwd="123456"}
  }
  val XEDU_AppSpaceInfo = new AppSpaceInfo() {alias="XEDU";appSpaceName="Content.XEDU";storeConfig =
    new StoreConfiguration() {hostName="dseServer";storeType=dataBase;dbConnString="dseServer/XEDU";
      usr="root";pwd="123456"}
  }
}