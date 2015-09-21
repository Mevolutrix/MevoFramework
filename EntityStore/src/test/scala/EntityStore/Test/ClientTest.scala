package EntityStore.Test
import CSON.Types.CSONTypes
import CSON.CSONDocument
import EntityAccess.{GeneralEntitySchema, GeneralEntityToCSON, JSONSerializer}
import EntityInterface.DefaultValueType
import EntityInterface.DefaultValueType._
import EntityInterface._
import EntityStore.Client.EntityPreProcessor
import EntityStore.Communication.EntityStoreServer
import EntityStore.Interface.{LoadEntityByKey, ResultFormat, DelEntity}
import EntityStore.Metadata._
import MySQLStore.operationExecutor.HsTaskProcessor
import SBEServiceAccess.CallDSE
import akka.actor.Props
import akka.testkit.TestProbe
import org.scalatest.FunSuite
import java.util

class ClientTest  extends FunSuite {
  test("Metadata operation test:") {
    implicit val system = SBEServiceAccess.ServiceConfig.appSystem
    EntityStoreServer.start(HsTaskProcessor)
    val autoKey = new  AutoValueKey()
    val testProxy = TestProbe()
    //val mdeSME = system.actorOf(Props[MetaDataSME])
    val setName = AutoValueKey.autoKeySetInfo.setName
    val postData = JSONSerializer(AutoValueKey.autoKeySetInfo)
    val params = Map[String, String]("setName" -> "'System.Configuration.AutoValueKey'")
    //testProxy.send(mdeSME, SME_PostRequest("createSet", params, postData))
    testProxy.expectMsg(false)
    assert((CallDSE(DelEntity("System.Configuration.AutoValueKey", "System.Metadata",
      EntitySet.schemaInfo, "System.Metadata.EntitySet")) match {
      case ret: java.util.List[CSONDocument] => """{"Delete":true}"""
      case _ => """{"Delete":false}"""
    }) == """{"Delete":true}""")
    //testProxy.send(mdeSME, SME_PostRequest("createSet", params, postData))
    testProxy.expectMsg(true)
    /*assert((CallDSE(EntityQuery("appSpaceId=@0", Array[Object]{"System.Configuration"},
      "entitySetName", "System.Metadata", "System.Metadata.EntitySet", EntitySet.schemaInfo)) match {
      case ret: java.util.List[CSONDocument] => ResultFormat.getJsonResult(ret).mkString("[", ",", "]")
      case a:(Int,Long) => """{"Result":"""" +MySQLErrorCodes(a._2) + """"}"""
    })!=null)*/
    val mdeAppSpaceInfo = MetadataManager.getAppSpace("MDE")
    MetadataManager.getAllSetOfApp(mdeAppSpaceInfo.appSpaceName).foreach(sName=>{
      println("Set :"+sName)
      assert(MetadataManager.getEntitySet(sName)!=null)
    })
    val cfgAppSpaceInfo = MetadataManager.getAppSpace("CFG")
    MetadataManager.getAllSchemaOfApp(mdeAppSpaceInfo.appSpaceName).foreach(schemaName=>{
      println("Schema :"+schemaName)
      if(schemaName!="System.Metadata.data"
        &&schemaName!="System.Metadata.form_fields"
        &&schemaName!="System.Metadata.operateBtn"
        &&schemaName!="System.Metadata.pageValue"
        &&schemaName!="System.Metadata.parameter"
        &&schemaName!="System.Metadata.Script"
        &&schemaName!="System.Metadata.scriptParam"
        &&schemaName!="System.Metadata.field_options")assert(MetadataManager.getSchema(schemaName)!=null)
    })
    val xeduAppSpaceInfo = MetadataManager.getAppSpace("XEDU")
    val xedu1AppSpaceInfo = MetadataManager.getAppSpace("Content.XEDU")
    assert(MetadataManager.createSchema((EntitySchema.validationRuleSchema.entityId,
      EntityType.Data), EntitySchema.validationRuleSchema)==false)
    assert(MetadataManager.updateSchema((EntitySchema.validationRuleSchema.entityId,
      EntityType.Data), EntitySchema.validationRuleSchema))

    val schemaName = EntitySchema.validationRuleSchema.entityId
    val pData = JSONSerializer(EntitySchema.validationRuleSchema)
    val param = Map[String, String]("id" -> "'System.Metadata.ValidationRule'","type"->"'Data'")
    //testProxy.send(mdeSME, SME_PostRequest("updateSchema",param,pData))
    testProxy.expectMsg(true)
    // Test update set for System.Configuration.AutoValueKey
    //testProxy.send(mdeSME, SME_PostRequest("updateSet",params,postData))
    testProxy.expectMsg(true)
    // Test deploy set
    val paramSets = Map[String, String]("appSpace" -> "'Content.XEDU'")
    //testProxy.send(mdeSME, SME_GetRequest("deploySets",paramSets))
    testProxy.expectMsg("[{\"setName\":\"Content.XEDU.UserInfo\"},{\"setName\":\"Content.XEDU.Survey\"},{\"setName\":\"Content.XEDU.Master\"},{\"setName\":\"Content.XEDU.Message\"},{\"setName\":\"Content.XEDU.Course\"},{\"setName\":\"Content.XEDU.Notice\"},{\"setName\":\"Content.XEDU.Test3\"},{\"setName\":\"Content.XEDU.Test\"},{\"setName\":\"Content.XEDU.Answer\"},{\"setName\":\"Content.XEDU.Test2\"},{\"setName\":\"Content.XEDU.Test4\"},{\"setName\":\"Content.XEDU.Test5\"},{\"setName\":\"Content.XEDU.AccountPoint\"},{\"setName\":\"Content.XEDU.UserRoleConfig\"},{\"setName\":\"Content.XEDU.PersonApply\"},{\"setName\":\"Content.XEDU.InstitutionApply\"},{\"setName\":\"Content.XEDU.SensitiveWords\"},{\"setName\":\"Content.XEDU.User\"},{\"setName\":\"Content.XEDU.Institution\"}]")
    // Delete one AutoValue record for "Answer"
    assert((CallDSE(DelEntity("Content.XEDU.Answer", "System.Configuration",
      AutoValueKey.autoKeySchemaInfo, "System.Configuration.AutoValueKey")) match {
      case ret: java.util.List[CSONDocument] => """{"Delete":true}"""
      case _ => """{"Delete":false}"""
    }) == """{"Delete":true}""")
    // Deploy "Answer" set to create the corresponding AutoValue record
    val paramSet = Map[String, String]("appSpace" -> "'Content.XEDU'","setName"->"'Content.XEDU.Answer'")
    //testProxy.send(mdeSME, SME_GetRequest("deploySets",paramSet))
    testProxy.expectMsg("[{\"setName\":\"Content.XEDU.Answer\"}]")
    // Query for XEDU PropertyReference
    /*assert((CallDSE(EntityQuery("appSpaceName=@0", Array[Object]{"Content.XEDU"},
      "id", "System.Metadata", "System.Metadata.PropertyRefence", PropertyReference.schemaInfo)) match {
      case ret: java.util.List[CSONDocument] => ResultFormat.getJsonResult(ret).mkString("[", ",", "]")
      case a:(Int,Long) => """{"Result":"""" +MySQLErrorCodes(a._2) + """"}"""
    })!=null)*/
    // load ReferenceData
    println("Prepare to get serializer for:"+PropertyReference.schemaInfo.entityId)
    val serializer = GeneralEntityToCSON(classOf[PropertyReference])
    val ret = CallDSE(LoadEntityByKey("XEDU.Chapter.type","System.Metadata","System.Metadata.PropertyRefence",
      PropertyReference.schemaInfo)).asInstanceOf[util.ArrayList[CSONDocument]]
    assert(serializer.getObject(ResultFormat.getCSONResult(ret,GeneralEntitySchema(classOf[PropertyReference]))(0))!=null)
    val refObj = serializer.getObject(ResultFormat.getCSONResult(ret,GeneralEntitySchema(classOf[PropertyReference]))(0)).asInstanceOf[PropertyReference]
    assert("XEDU.Chapter.type" ==refObj.refId)
    assert(refObj.appSpace=="Content.XEDU")
    assert(refObj.referenceMap!=null)
    // Test Preprocessor
    assert(EntityPreProcessor(ResultFormat.getJsonResult(ret)(0),
      PropertyReference.schemaInfo, PropertyReference.setInfo)!=null)
    assert(EntityPreProcessor.preCreate(ResultFormat.getJsonResult(ret)(0),
      PropertyReference.schemaInfo, PropertyReference.setInfo)!=null)
    val validRule = new ValidationRule()
    validRule.errMsg=""
    validRule.name=""
    validRule.pattern=""
    // SetMetadata
    val setMeta=AppSpaceInfo.metaInfo
    setMeta.populate()
    assert(setMeta.getName=="System.Metadata.AppSpaceInfo")
    assert(setMeta.baseSchema!=null)
    assert(setMeta.supportSchemas!=null)
    assert(setMeta.entitySchemaList(0).schema!=null)
    system.shutdown()
    // Cover the IEntitySet interface for EntitySchema
    val setForSchema = EntitySet.schemaInfo.asInstanceOf[IEntitySet]
    assert(setForSchema.appSpace=="System.Metadata")
    assert(setForSchema.primaryKey=="entitySetName" )
    assert(setForSchema.storageType!=StorageType.NoStore)
    assert(setForSchema.indexProperties.length>0)
    assert(setForSchema.defaultValueProperties!=null)
    assert(setForSchema.isIndex("appSpaceId"))
    assert(setForSchema.getIndexType("appSpaceId")==IndexType.Default)
    assert(setForSchema.getDefaultValueType("appSpaceId")==DefaultValueType.Non)
    assert(setForSchema.getDefaultValueDefinition("appSpaceId")==null)
    assert(setForSchema.getAutomaticProperty=="")
    assert(setForSchema.getPrimaryKeyMin!=null)
    assert(setForSchema.getGZIPPropertyName==null)
    assert(EntitySet.schemaInfo.schemaType ==EntityType.Data)
    assert(EntitySet.schemaInfo.containsProperty("appSpaceId"))
    assert(EntitySet.schemaInfo.getTypeCode("appSpaceId")>0)
    assert(EntitySet.schemaInfo.objType!=null)
    assert(EntitySet.setInfo.getGZIPPropertyName==null)
    assert(EntitySchema.clone(EntitySet.schemaInfo.asInstanceOf[EntitySchema],EntityType.View)!=null)
    assert(EntitySchema(pData)!=null)
    // Cover to default values
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
        defaultValue = ""
        evalExp = null
      })
      _storageType = StorageType.Shared
      autoValue = ""
      minValueOfPKey = ""
    }).populate
    assert(setInfo.getDefaultValueType("testDate")==DefaultValueType.Preset)
    assert(setInfo.getDefaultValueDefinition("testDate")!=null)
  }
}
