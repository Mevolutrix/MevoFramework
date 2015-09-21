package mevolutrix.ServiceRT
import java.io.{File, FileInputStream}
import java.util.concurrent.ConcurrentHashMap
import CSON.Types.CSONTypes
import EntityInterface.StorageType
import EntityStore.Interface.{SystemRequestContext, LoadEntityByKey}
import EntityStore.Metadata.{EntityQueryMark, EntitySet, EntitySchema}
import SBEServiceAccess.CallDSE
import org.slf4j.{Logger, LoggerFactory}

object ServiceType extends Enumeration {
  type ServiceType = Value
  val system = Value(1)
  val fileUserDefined = Value(2)    // User defined Service and store in file
  val storeUserDefined = Value(3)  // small user defined service which stored in configuration record
}

/**
 * Customized SVC configuration.One appSpace will have one entry and point to the JAR library.
 */
class SVCConfig {
  var alias:String = _
  var storedType:ServiceType.ServiceType = ServiceType.system
  var svcObjFullName:String = _
  var jarPath:String = _     // Assembly jar file location
  var assembly:Array[Byte] = _    // Tiny service Jar package stored in the record
}
object SVCConfig extends ClassLoader{
  val log: Logger = LoggerFactory.getLogger("SVCConfig")
  val systemAppSpace = "System.Configuration"
  val cfgReqContext = new SystemRequestContext("CFG",systemAppSpace,"","",0,0)
  val setInfo = (new EntitySet {
    entitySetName=systemAppSpace+".SVCConfig"
    appSpaceId=systemAppSpace
    description="EntitySet for Customized Service JAR Configuration."
    pKey="alias"
    pkeyType=CSONTypes.UTF8String.id.toByte
    index=Array[EntityQueryMark]()
    _storageType=StorageType.Shared
    autoValue=""
    minValueOfPKey = ""
  }).populate
  val typeInfo = classOf[SVCConfig]
  val schemaInfo = new EntitySchema(setInfo,systemAppSpace,
    Array[String]("alias","storedType","svcObjFullName","jarPath","assembly"),
    classOf[SVCConfig],null).populate()
  private val svcCache = new ConcurrentHashMap[String,SVCConfig]()
  private val svcClassTypes = new ConcurrentHashMap[String,Class[_]]()
  private val moduleLoader = new MevoClassLoader.ByteArrayClassLoader()
  def apply(alias:String) = {
    val ret = svcCache.get(alias)
    if (ret==null) {
      log.info("SVCConfig load new config for appSpace:"+alias)
      val newSVCConfig = loadConfig(alias)
      if (newSVCConfig!=null) {
        svcCache.put(alias, newSVCConfig)
        svcClassTypes.get(alias)
      } else {
        log.error("load SVC configuration not found config data for appSpace:"+alias)
        null
      }
    } else svcClassTypes.get(alias)

  }
  private def loadConfig(alias:String):SVCConfig = {
    log.info("SVC Sevice config load:"+alias)
    val ret = CallDSE(LoadEntityByKey(alias, systemAppSpace, setInfo.setName, schemaInfo),
      typeInfo).asInstanceOf[SVCConfig]
    if (ret!=null) ServiceType(ret.storedType.id) match {
      case ServiceType.system => svcClassTypes.putIfAbsent(alias,Class.forName(ret.svcObjFullName))
      case ServiceType.fileUserDefined =>
        log.info("SVC Sevice config load with Jar file:"+ret.jarPath)
        svcClassTypes.putIfAbsent(alias,moduleLoader.loadClassFromFile(ret.jarPath,ret.svcObjFullName))
      case ServiceType.storeUserDefined =>
        svcClassTypes.putIfAbsent(alias,moduleLoader.loadClass(ret.svcObjFullName,ret.assembly))
    }
    ret
  }
}