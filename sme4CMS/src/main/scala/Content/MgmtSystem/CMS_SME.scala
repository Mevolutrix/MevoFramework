package Content.MgmtSystem
import java.util
import EntityAccess.JSONSerializer
import ExpressionParser.EntityRequest
import mevolutrix.Interface.ISmeHandler
import EntityInterface.IEntityRandomAccess
import EntityStore.Metadata.MetadataManager
import HandlerSocket.Protocol.MySQLErrorCodes
import EntityStore.Client.{EntityAccessor, EntityPreProcessor}
import CSON.{CSONComplexElement, CSONDocument, CSONElementArray}
import EntityStore.Interface.{ResultFormat, SystemRequestContext}

class CMS_SME extends ISmeHandler {
  val reqContext = new SystemRequestContext("CMS", "Content.MgmtSystem", "", "", 0, 0)
  val channelSetName = "Content.MgmtSystem.Channel"
  val contentSetName = "Content.MgmtSystem.Content"
  val channelSchema = MetadataManager.getSchema(channelSetName)
  val contentSchema = MetadataManager.getSchema(contentSetName)
  val contentSet = MetadataManager.getEntitySet(contentSetName)
  def getRequest(funcName:String,params:Map[String,String]):String = {
    null
  }
  def postRequest(funcName:String,params:Map[String,String],jsonBody:String):String = {
    funcName match {
      case "publishToCMS" =>
        val schemaName = EntityRequest.getFieldValue(params,"$schemaName").asInstanceOf[String]
        val channelId = EntityRequest.getFieldValue(params,"$channelId").asInstanceOf[Int]
        val title = EntityRequest.getFieldValue(params,"$title").asInstanceOf[String]
        val content = EntityRequest.getFieldValue(params,"$content").asInstanceOf[String]
        val author = EntityRequest.getFieldValue(params,"$author").asInstanceOf[String]
        val dataDoc = JSONSerializer.unapply(jsonBody,MetadataManager.getSchema(schemaName)).asInstanceOf[IEntityRandomAccess]

        def getInsertContent(schemaDefs:CSONElementArray) = {
          val sb = new StringBuilder()
          sb.append("{").append("\"id\":").append("0").append(",")
          sb.append("\"type\":").append("1").append(",")
          sb.append("\"title\":\"").append(title).append("\",")
          sb.append("\"parentId\":").append(channelId.toString).append(",")
          sb.append("\"beginData\":").append("null").append(",")
          sb.append("\"endData\":").append("null").append(",")
          sb.append("\"createDate\":").append("null").append(",")
          sb.append("\"content\":\"").append(content).append("\",")
          sb.append("\"author\":\"").append(author).append("\",")
          sb.append("\"extentProperty\":").append("[")
          schemaDefs.iterator.foreach(item => {
            val propertyDef = item.asInstanceOf[CSONComplexElement]
            val propertyName = propertyDef.getValue("name").asInstanceOf[String]
            val value = dataDoc.getValue(propertyName)
            sb.append("{").append("\"name\":\"").append(propertyName).append("\",")
            sb.append("\"value\":").append("\"").append(value.toString).append("\"}").append(",")
          })
          sb.deleteCharAt(sb.length - 1).append("]").append("}")
          println("CMS SME, insert value:" + sb.result())
          sb.result()
        }
        val channelDoc = EntityAccessor.loadByKey(channelId.asInstanceOf[Object],reqContext,
          channelSetName, channelSchema) match {
          case retList: util.List[CSONDocument] =>
            val csonList = ResultFormat.getCSONResult(retList, channelSchema)
            if (csonList.length > 0) csonList(0)
            else null
          case _ => null
        }
        try {
          if (channelDoc != null) {
            val schemaDefs = channelDoc.getElement("extentPropertySchema") match {
              case schemaArray: CSONElementArray => if (schemaArray.length > 0) schemaArray else null
              case _ => null
            }
            if (schemaDefs != null) {
              val newEntity = EntityPreProcessor.preCreate(getInsertContent(schemaDefs),
                contentSchema, contentSet)
              val binary = newEntity.getBytes()
              val pKeyRawValue = newEntity.getRawValue(contentSet.primaryKey)
              EntityAccessor.insert(binary, reqContext, contentSetName, contentSchema, pKeyRawValue) match {
                case ret: java.util.List[CSONDocument] =>
                  if (ret.size() > 0) ResultFormat.getJsonResult(ret)(0)
                  else EntityAccess.CSON2JSON(newEntity.toCSONElement)
                case a: (Int, Long) => """{"Result":"""" + MySQLErrorCodes(a._2) + """"}"""
              }
            } else "{\"Result\":" + "\"Channel no extentPropertySchema!\"}"
          }
          else "{\"Result\":" + "\"Channel not found!\"}"
        }catch {
          case e:IllegalArgumentException => "{\"Result\":\"" + e.getMessage +"\"}"
          case e:Exception => throw e
        }
    }
  }
}
