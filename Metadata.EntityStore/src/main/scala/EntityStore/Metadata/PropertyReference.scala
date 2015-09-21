package EntityStore.Metadata
import EntityInterface._

import scala.collection.mutable

class PropertyMap {
  var key:String = null
  var description:String = null
}
class PropertyReference extends IReferenceInfo {
  var id:String = null
  var appSpaceName:String = null
  var propertyMap:Array[PropertyMap]=null
  private val local = new mutable.HashMap[String,String]()
  def refId: String = id
  def appSpace: String = appSpaceName
  def referenceMap: mutable.HashMap[String, String] =
    if (local.size>0) local
    else if (propertyMap!=null && propertyMap.length>0) {
      propertyMap.foreach(p=>local.put(p.key,p.description))
      local
    } else null
}
object PropertyReference {
import CSON.Types._
  val system_AppSpace = MetadataManager.system_AppSpace
  val setInfo = (new EntitySet() {
    entitySetName = system_AppSpace + ".PropertyReference"
    appSpaceId = system_AppSpace
    description = "EntitySet for property Reference information"
    pKey = "id"
    pkeyType = CSONTypes.UTF8String.id.toByte
    index = Array[EntityQueryMark](new EntityQueryMark() {
      path = "appSpaceName"
      direct = true
      markType = IndexType.Default
      indexDataType = CSONTypes.UTF8String.id.toByte
    })
    _storageType = StorageType.Shared
    autoValue = ""
    minValueOfPKey = ""
  }).populate
  val propertyMapSchemaInfo = new EntitySchema(null,system_AppSpace,Array[String]("key","description"),
    classOf[PropertyMap],null).populate()
  val schemaInfo = new EntitySchema(setInfo.asInstanceOf[EntitySet],system_AppSpace,
    Array[String]("id","appSpaceName","propertyMap"),classOf[PropertyReference],Array[(String,IEntitySchema)](
      ("PropertyMap",propertyMapSchemaInfo) )  ).populate()
}
