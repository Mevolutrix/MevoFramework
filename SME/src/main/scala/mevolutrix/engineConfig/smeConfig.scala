package mevolutrix.engineConfig
import CSON.CSONDocument
import CSON.Types.CSONTypes
import EntityStore.Metadata._
import mevolutrix.ServiceRT._
import SBEServiceAccess.CallDSE
import EntityAccess.GeneralEntityToCSON
import java.util.concurrent.ConcurrentHashMap
import EntityInterface.{StorageType, IndexType}
import EntityStore.Interface.{CreateEntity, LoadEntityByKey}

/**
 * Persistence Pojo class to store the alias and assemply info
 */
class SMEConfig {
import ServiceType._
  var alias:String = _
  var svcObjFullName:String = _
  var serviceType:ServiceType = ServiceType.system
  var jarPath:String = _     // Assembly jar file location
  var assembly:Array[Byte] = _    // Tiny service class file stored in the record
}

object SMEConfig extends ClassLoader {
  private val smeClassTypes = new ConcurrentHashMap[String,Class[_]]()
  private val moduleLoader = new MevoClassLoader.ByteArrayClassLoader()
  val systemAppSpace = "System.Configuration"
  val metadataSMEInfo = new SMEConfig {
    alias = "System.Metadata"
    svcObjFullName = "EntityStore.Metadata.MetadataSME"
  }
  val cfgSMEInfo = new SMEConfig {
    alias = "System.Configuration"
    svcObjFullName = "mevolutrix.ServiceRT.SecureTokenSME"
  }
  val CMSsmeInfo = new SMEConfig {
    alias = "Content.MgmtSystem"
    svcObjFullName = "Content.MgmtSystem.CMS_SME"
    serviceType = ServiceType.fileUserDefined
    jarPath = "sme4cms_2.11-0.1.0.jar"
  }
  val setName = "System.Configuration.SMEConfig"
  // already stored in persistence store
  val smeSetInfo = (new EntitySet {
    entitySetName = systemAppSpace + ".SMEConfig"
    appSpaceId = systemAppSpace
    description = "EntitySet for SME service registration data"
    pKey = "alias"
    pkeyType = CSONTypes.UTF8String.id.toByte
    index = Array[EntityQueryMark](new EntityQueryMark() {
      path = "serviceType"
      direct = true
      markType = IndexType.Default
      indexDataType = CSONTypes.Int32.id.toByte
    })
    _storageType = StorageType.Shared
    autoValue = ""
    minValueOfPKey = ""
  }).populate
  val typeInfo = classOf[SMEConfig]
  val schemaInfo = new EntitySchema(smeSetInfo, systemAppSpace,
    Array[String]("alias", "svcObjFullName", "serviceType", "jarPath", "assembly"),
    Class.forName("mevolutrix.engineConfig.SMEConfig"), null).populate()

  private def preLoadSySMEConfig = {
    smeClassTypes.putIfAbsent("System.Metadata",Class.forName(metadataSMEInfo.svcObjFullName))
    smeClassTypes.putIfAbsent("System.Configuration",Class.forName(cfgSMEInfo.svcObjFullName))
    smeClassTypes.putIfAbsent("Content.MgmtSystem",moduleLoader.loadClassFromFile(CMSsmeInfo.jarPath, CMSsmeInfo.svcObjFullName))
  }

  preLoadSySMEConfig

  def addSMEConfig(alias: String, smeConfig: SMEConfig): Boolean = {
    val binary = GeneralEntityToCSON(typeInfo).getObjectCsonBinary(smeConfig, null)._1
    CallDSE(CreateEntity(alias, binary, systemAppSpace, schemaInfo, smeSetInfo.setName)) match {
      case ret: java.util.ArrayList[CSONDocument] => true
      case _ => false
    }
  }

  def apply(appSpace: String) = {
    val ret = smeClassTypes.get(appSpace)
    if (ret == null) {
      val newSMEConfig = loadConfig(appSpace)
      if (newSMEConfig != null) smeClassTypes.get(appSpace)
      else throw new IllegalArgumentException("SME configuration load error for alias:"+appSpace)
    } else ret
  }

  private def loadConfig(appSpace: String): SMEConfig = {
    val ret = CallDSE(LoadEntityByKey(appSpace, systemAppSpace, smeSetInfo.setName, schemaInfo),
      typeInfo).asInstanceOf[SMEConfig]
    if (ret != null) {
      ServiceType(ret.serviceType.id) match {
        case ServiceType.system => smeClassTypes.putIfAbsent(appSpace,Class.forName(ret.svcObjFullName))
        case ServiceType.fileUserDefined =>
          smeClassTypes.putIfAbsent(appSpace, moduleLoader.loadClassFromFile(ret.jarPath, ret.svcObjFullName))
        case ServiceType.storeUserDefined =>
          smeClassTypes.putIfAbsent(appSpace, moduleLoader.loadClass(ret.svcObjFullName, ret.assembly))
      }
      ret
    }
    else null
  }
}