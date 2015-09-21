package Mevolutrix.serviceBusEngine
import java.util
import EntityAccess._
import CSON.CSONDocument
import CSON.Types.CSONTypes
import EntityStore.Client.EntityAccessor
import EntityInterface.{IndexType, StorageType}
import EntityStore.Interface.{SystemRequestContext, RequestMessage, ResultFormat}
import EntityStore.Metadata.{EntityQueryMark, EntitySchema, EntitySet, FutureCache}

class DSEServiceConfig {
  var id:Int = -1
  var appSpace:String = _
  var description:String = _
  var stmt:String = _
}
object DSESvcConfig {
  val systemAppSpace = "System.Configuration"
  val cfgReqContext = new SystemRequestContext("CFG",systemAppSpace,"","",0,0)
  val reqMsgSchema = EntityAccess.GeneralEntitySchema(classOf[RequestMessage])
  private val svc_ConfigCache = new FutureCache[Int,CSONDocument](loadSvcConfig)
  private def loadSvcConfig(id:Int):CSONDocument = {
    val typeInfo = classOf[DSEServiceConfig]
    val serializer = GeneralEntityToCSON(typeInfo)
    val schema = GeneralEntitySchema(typeInfo)
    val ret:IndexedSeq[CSONDocument] =
      EntityAccessor.loadByKey(id.asInstanceOf[Object],cfgReqContext,schemaInfo.setName,schemaInfo) match {
        case retList: util.List[CSONDocument] => ResultFormat.getCSONResult(retList, schema)
        case _ => new Array[CSONDocument](0)
      }
    if (ret.length>0) getStmt(ret(0).getValue("stmt").asInstanceOf[String])
    else null
  }
  val setInfo = (new EntitySet() {
    entitySetName = systemAppSpace + ".DSEServiceConfig"
    appSpaceId = systemAppSpace
    description = "EntitySet for DSE service configuration"
    pKey = "id"
    pkeyType = CSONTypes.Int32.id.toByte
    index = Array[EntityQueryMark](new EntityQueryMark() {
      path = "appSpace"
      direct = true
      markType = IndexType.Default
      indexDataType = CSONTypes.UTF8String.id.toByte
    })
    _storageType = StorageType.Shared
    autoValue = ""
    minValueOfPKey = "-1"
  }).populate
  val schemaInfo = new EntitySchema(setInfo.asInstanceOf[EntitySet],systemAppSpace,Array[String]
      ("id","appSpace","description","stmt"),classOf[DSEServiceConfig],null ).populate()
  def saveStmt(svcId:Int,appSpaceName:String,comment:String,reqMsg:String):DSEServiceConfig =
    new DSEServiceConfig {
      id = svcId
      appSpace = appSpaceName
      description = comment
      stmt = reqMsg
    }
  def getStmt(stJson:String):CSONDocument = JSONSerializer.unapply(stJson,reqMsgSchema).
                                            asInstanceOf[CSONDocument]
}
