package Mevolutrix.ServiceMgmt
import CSON.CSONDocument
import CSON.Types.CSONTypes
import EntityStore.Client.EntityAccessor
import org.slf4j.{Logger, LoggerFactory}
import HandlerSocket.Protocol.MySQLErrorCodes
import java.util.concurrent.ConcurrentHashMap
import EntityInterface.{IndexType, StorageType}
import EntityAccess.{GeneralEntitySchema, GeneralEntityToCSON}
import EntityStore.Metadata.{EntityQueryMark, EntitySchema, EntitySet}
import EntityStore.Interface.{ResultFormat, SystemQueryRequest, SystemRequestContext}

/**
 * Store and manager the service access control info within SBE.
 *
 */
class ServiceConfig {
  var id:Int = 0
  var appAlias:String = _
  var version:String = _
  var name:String = _
  var serviceURL:String = _
  var inSchemaId:String = _
  var outSchemaId:String = _
  var description:String = _
}
object ServiceManager {
  val log: Logger = LoggerFactory.getLogger("ServiceManager")
  val systemAppSpace = "System.Configuration"
  val cfgReqContext = new SystemRequestContext("CFG",systemAppSpace,"","",0,0)
  val setInfo = (new EntitySet {
    entitySetName=systemAppSpace+".ServiceConfig"
    appSpaceId=systemAppSpace
    description="EntitySet for Service access Configuration."
    pKey="id"
    pkeyType=CSONTypes.Int32.id.toByte
    index=Array[EntityQueryMark](new EntityQueryMark() {
      path="appAlias"
      direct=true
      markType=IndexType.Default
      indexDataType=CSONTypes.UTF8String.id.toByte
    })
    _storageType=StorageType.Shared
    autoValue="id"
    minValueOfPKey = "0"
  }).populate
  val typeInfo = classOf[ServiceConfig]
  val schemaInfo = new EntitySchema(setInfo,systemAppSpace,
    Array[String]("id","appAlias","version","name","serviceURL","inSchemaId","outSchemaId","description"),
    classOf[ServiceConfig],null).populate()
  private val svcAppSpaceCache = new ConcurrentHashMap[String,ConcurrentHashMap[String,String]]()
  private val svcCacheById = new ConcurrentHashMap[Integer,ServiceConfig]()
  private val svcCacheByUri = new ConcurrentHashMap[String,Integer]()
  def apply(alias:String) = loadConfig(alias)
  def getById(serviceId:Integer) = {
    val ret = svcCacheById.get(serviceId)
    if (ret==null) {
      val newConfig = loadConfig(serviceId)
      if (newConfig!=null) {
        svcCacheById.put(serviceId, newConfig)
        newConfig
      } else {
        log.error("load Service access configuration not found for serviceID:{}"+serviceId)
        null
      }
    } else ret
  }

  private def loadConfig(id:Integer) = {
    EntityAccessor.getResult(EntityAccessor.loadByKey(id,cfgReqContext,
      setInfo.setName,schemaInfo),typeInfo).asInstanceOf[ServiceConfig]
  }
  private def loadConfig(alias:String) = {
    if (svcAppSpaceCache.contains(alias)) svcAppSpaceCache.get(alias)
    else {
      val ret = new ConcurrentHashMap[String, String]
      val queryFilter = "appAlias = @0"
      val params = Array[Object] {alias}
      val queryReq = new SystemQueryRequest(queryFilter, null, params)
      EntityAccessor.query(queryReq, cfgReqContext, setInfo.setName, schemaInfo) match {
        case retList: java.util.List[CSONDocument] =>
          val serializer = GeneralEntityToCSON(typeInfo)
          ResultFormat.getCSONResult(retList, GeneralEntitySchema(typeInfo)).foreach(item => {
            val svcConfig = serializer.getObject(item).asInstanceOf[ServiceConfig]
            svcCacheById.putIfAbsent(svcConfig.id, svcConfig)
            svcCacheByUri.putIfAbsent(svcConfig.serviceURL, svcConfig.id)
            ret.put(svcConfig.serviceURL,"")  //Todo: load role for each serviceURL
          })
        case a: (Int, Long) => throw new Exception("load service of("+alias+") error with:"+MySQLErrorCodes(a._2))
      }
      ret
    }
  }
}
