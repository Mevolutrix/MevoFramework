package MySQLStore.autoValueGenerator
import EntityStore.Metadata.MetadataManager
import akka.actor.{Actor, ActorLogging}
import DBOperation.DBOperator
import java.sql.{ResultSet, Statement, Connection, SQLException}

case class AutoKeyInit(setName:String,keyType:Boolean)
/**
 * Actor to generate 
 */
class KeyGenerator extends Actor with ActorLogging{
  private var intKey:Int = 0
  private var intLimit:Int = intKey + AutoValue.poolStep
  private var longKey:Long = 0L
  private var longLimit:Long = longKey + AutoValue.poolStep
  private var setNameOfAutoKey:String = _
  private var keyType:Boolean = true // True = Int type key, False = Long type Key
  private def initAutoKeyPool(setName:String):Unit = {
    if (keyType) {
      intKey = allocAutoKeyPool(setName, AutoValue.poolStep, keyType)._1 - 1
      intLimit = intKey+AutoValue.poolStep
    }
    else {
      longKey = allocAutoKeyPool(setName, AutoValue.poolStep, keyType)._2 - 1L
      longLimit = longKey+AutoValue.poolStep
    }
  }
  private def getKeyNewValue = {
    if (keyType) {
      if (intKey < intLimit-1) intKey += 1
      else {
        intKey = allocAutoKeyPool(setNameOfAutoKey,AutoValue.poolStep,keyType)._1
        intLimit = intKey + AutoValue.poolStep
      }
    } else {
      if (longKey < longLimit-1) longKey += 1
      else {
        longKey =  allocAutoKeyPool(setNameOfAutoKey,AutoValue.poolStep,keyType)._2
        longLimit = longKey + AutoValue.poolStep
      }
    }
    log.info("get Auto Value("+setNameOfAutoKey+") key:"+(intKey,longKey))
    (intKey,longKey)
  }
  def receive = {
    case AutoKeyInit(setName,theType) =>
      log.info("Init for "+setName)
      setNameOfAutoKey = setName
      keyType = theType
      initAutoKeyPool(setName)
      context.become({
        case "getKey" =>
          log.info("got AutoKey request for:"+setNameOfAutoKey)
          if (keyType) sender() ! getKeyNewValue._1
          else sender() ! getKeyNewValue._2
        case AutoKeyInit(setName,theType) => // Do nothing as duplicate init
          sender() ! null
      }, discardOld = false)
    case a@_ => log.error("Error: not get init but receive get auto key request->"+a)
  }
  private def allocAutoKeyPool(setName:String, poolSize:Int,intKey:Boolean) = {
    val appInfo = MetadataManager.getAppSpace("CFG")
    var intValue:Int = 0
    var longValue:Long = 0L
    if (DBOperator.executeOperation(appInfo.storeConfig.dbConnString,
      appInfo.storeConfig.usr, appInfo.storeConfig.pwd,(conn)=>{
        val pstmt = conn.prepareStatement("select * from System_Configuration_AutoValueKey where fullSetName = ?",
          ResultSet.TYPE_SCROLL_SENSITIVE,ResultSet.CONCUR_UPDATABLE)
        pstmt.setString(1,setName)
        val rs = pstmt.executeQuery()
        if (rs.next()) {
          intValue = rs.getInt("intAutoValue")
          longValue = rs.getLong("longAutoValue")
          if (intKey)rs.updateInt("intAutoValue",intValue+poolSize)
          else rs.updateLong("longAutoValue",longValue+poolSize)
          rs.updateRow()
          pstmt.close()
          true
        }
        else {
          pstmt.close()
          false
        }
      })) (intValue,longValue)
    else (-1,-1L)
  }

}
