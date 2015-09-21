package EntityStore.Metadata
import SBEServiceAccess.CallSAE
import EntityInterface.EntityType._
import mevolutrix.Interface.SAE_GetReq
import EntityAccess.GeneralEntityToCSON
import EntityInterface.{IEntitySet, IEntitySchema, EntityType}

/**
 * Cache for SBE to load and store schema info.
 */
object SBEMetadata {
/*  import org.slf4j.{LoggerFactory, Logger}
  var logger: Logger = LoggerFactory.getLogger("MetadataManager")*/
  val system_AppSpace = "System.Metadata"
  val schemaSerializer = GeneralEntityToCSON(classOf[EntitySchema])
  private val schemaCache = new FutureCache[(String, EntityType), EntitySchema](loadSchema,
    (key,v)=>{
      // Populate here! Otherwise it will cause loop when someone define a recursive schema
      v.sbePopulate(key._2)
    })
  private val entitySetCache = new FutureCache[String, EntitySet](loadEntitySet)
  private val appSpaceCache = new FutureCache[String, AppSpaceInfo](loadAppSpace)

  private def loadSchema(schemaKey:(String,EntityType)):EntitySchema = {
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
      case _ =>
        CallSAE(SAE_GetReq("MDE","loadSchema","schemaKey -> '"+schemaKey._1+"',type -> '"
          +schemaKey._2.toString+"'",false), classOf[EntitySchema])
    }).asInstanceOf[EntitySchema] // DO NOT populate here! Otherwise it will cause loop when someone define a recursive schema
  }
  private def loadEntitySet(setKey:String):EntitySet = {
    (setKey match {
      case "System.Metadata.EntitySchema" => EntitySchema.setInfo
      case "System.Metadata.EntitySet" => EntitySet.setInfo
      case "System.Metadata.SetMetadata" => SetMetadata.setInfo
      case "System.Metadata.AutoValueKey" => AutoValueKey.autoKeySetInfo
      case "System.Metadata.SetUpdateRecord" => SetUpdateRecord.setInfo
      case "System.Metadata.AppSpaceInfo" => AppSpaceInfo.setInfo
      case "System.Metadata.ValidationRule" => EntitySchema.validationRuleSetInfo
      case "System.Metadata.PropertyReference" => PropertyReference.setInfo
      case _ => CallSAE(SAE_GetReq("MDE","loadSet","setName -> '"+setKey+"'",false),classOf[EntitySet])
    }).asInstanceOf[EntitySet].populate
  }
  private def loadAppSpace(alias:String):AppSpaceInfo = alias match {
    case "MDE" | "System.Metadata" => System_Definition_Metadata.MDE_AppSpaceInfo
    case "CFG" | "System.Configuration" => System_Definition_Metadata.CFG_AppSpaceInfo
    case _ => CallSAE(SAE_GetReq("MDE", "loadAppSpace", "alias -> '" + alias+"'", false),
      classOf[AppSpaceInfo]).asInstanceOf[AppSpaceInfo]
  }
  def getAppSpace(alias:String):AppSpaceInfo = appSpaceCache.get(alias)
  def getSchema(schemaId: String, metaType: EntityType = EntityType.Data):IEntitySchema =
    schemaCache.get((schemaId,metaType))
  def getEntitySet(entitySetName: String): IEntitySet = entitySetCache.get(entitySetName)
}
