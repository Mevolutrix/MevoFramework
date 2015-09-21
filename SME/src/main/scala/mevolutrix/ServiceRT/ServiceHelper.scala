package mevolutrix.ServiceRT
import java.sql.Connection
import DBOperation.DBOperator
import EntityAccess.JSONSerializer
import SVCInterface.ISvcHelper
import EntityInterface.{IEntityRandomAccess, IEntitySchema}
//import collection.mutable.{HashMap=>SHashMap}
import EntityStore.Metadata.{MetadataManager, AppSpaceInfo}
import java.util.concurrent.{ConcurrentLinkedQueue,ConcurrentHashMap}

import scala.collection.mutable

/**
 * Helper to provide connection pool and schema accessor for external Service plugins.
 */
class ServiceHelper(appSpace:String) extends ISvcHelper{
  private val appSpaceInfo:AppSpaceInfo = MetadataManager.getAppSpace(appSpace)
  private val pool = new ConcurrentLinkedQueue[Connection]()
  private def getNewConn():Connection = {
    val dbConfig = appSpaceInfo.storeConfig
    DBOperator.getConn(dbConfig.dbConnString,dbConfig.usr,dbConfig.pwd)
  }
  override def getSchema(schemaId: String): IEntitySchema = MetadataManager.getSchema(schemaId)
  override def rawData2CSON (rawData: String, schemaId: String): IEntityRandomAccess =
    JSONSerializer.unapply(rawData,getSchema(schemaId)).asInstanceOf[IEntityRandomAccess]
  override def getConn: Connection = {
    val ret = pool.poll()
    if (ret!=null && !ret.isClosed) ret
    else getNewConn()
  }
  override def releaseConn(conn:Connection):Unit = {
    pool.add(conn)
    if (pool.size()>ServiceHelper.minConnLimit) {
      val conn = pool.poll()
      if (conn!=null) conn.close()
    }
  }
}
object ServiceHelper {
  val minConnLimit = 2
  private val connPool = new ConcurrentHashMap[String,ServiceHelper]()
  // putIfAbsent will return the existing value if key found. Put new value into if key doesn't exist,
  // and return null. So the Option().getOrElse is used to retrieve the new value put into the cache.
  def apply(alias:String):ServiceHelper = Option(connPool.
    putIfAbsent(alias,new ServiceHelper(alias))).getOrElse(connPool.get(alias))

}

  

