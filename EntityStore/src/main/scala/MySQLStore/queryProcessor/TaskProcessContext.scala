package MySQLStore.queryProcessor
import java.util._
import EntityInterface._
import CSON.CSONDocument
import EntityStore.Interface._
import EntityAccess.GeneralEntityToCSON
import EntityStore.Client.DSEClientAccessor
import HandlerSocket.Protocol.{HandlerSocketException, Row, Get}
import MySQLStore.client.{HsClientAccessor, HandlerSocketClient}
import EntityStore.Metadata.{EntitySet, MetadataManager}

class RequestProcessContext(val reqMessage:IEntityRandomAccess) extends SessionContext with StatementTemplate {
  def dispose: Unit = sa.finishSession
  private var readOnlyRequest = true
  private var opCursor = 0

  InitRequest(Some(reqMessage.asInstanceOf[CSONDocument]))
  loadTemplate
  private val setInfo = HandlerSocketClient.getSetInfo(operationStatements.get(0))
  private val ops = HandlerSocketClient.parseRequests(operationStatements)
  for (i<-0 until ops.length) ops(i)._2(0) match {
    case item:Get =>
    case _ => readOnlyRequest = false
  } // Scan request operations to make sure open property r/w connection
  private val sa = HsClientAccessor.getSession(setInfo.appSpace,0,readOnlyRequest)

  /**
   * format the result based on the returned schema of parseRequest, get result by calling nextResult(There may be
   * multiple requests one time) and serialize them into iterator of CSONDocument
   * @param outputWriter The general output buffer formater implemented in EntityStoreServer.getAckOutput
   * @param updateFinish Flag indicate whether all result finished no more nextResult
   */
  def fillResult(outputWriter:(Int,Long,Int,IndexedSeq[IEntityRandomAccess])=>Unit,updateFinish:(Boolean)=>Unit) = {
    def nextResult: (Boolean, AnyRef) = {
      val ret = if (opCursor < ops.length - 1)
        (false,sa.executeCommands(ops(opCursor)._1, ops(opCursor)._2))
      else if (opCursor < ops.length) (true, sa.executeCommands(ops(opCursor)._1, ops(opCursor)._2))
      else throw new Exception("Hs task processor context already finished. Wrong contine ack.")
      opCursor+=1

      ret
    }
    val cmdIdx = ops(opCursor)._1
    val (finished,results)=nextResult
    results match{
      case status:(Int,String) =>
        updateFinish(finished)
        DSEClientAccessor.log.error("Debug: HS error - "+status._2)
        outputWriter(status._1,HandlerSocketException.tryGetErrorDetail(status._2),0,null)
      case rowList:List[Row] =>
        val retList = for(i<-0 until rowList.size()) yield {
          val row = rowList.get(i)
          if (cmdIdx.columns(cmdIdx.columns.length-1)!="_raw_DATA_") {
            // Select query: do projection result format
            val query = operationStatements.get(opCursor - 1).asInstanceOf[QueryStatement]
            val indexInfo = MetadataManager.getEntitySet(query.query.queryEntitySet).asInstanceOf[EntitySet]

            val sb = new StringBuilder()
            sb.append("{")
            for (i <- 0 until cmdIdx.columns.length) {
              sb.append("\"" + cmdIdx.columns(i) + "\" : ")
              indexInfo.getIndexPropertyTypeCode(cmdIdx.columns(i)) match {
                case 1 | 8 | 0x10 | 0x12 | 0x13 | 0x15 | 0x16 | 0x17 => //Float, Bool, null, int32/64, Decimal,numbers
                  sb.append(row.column(i))
                case _ => sb.append("\"" + row.column(i) + "\" ")
              }
              if (i < cmdIdx.columns.length - 1) sb.append(" , ")
            }
            sb.append("}")
            sb.result()
          }
          else row.column(row.columnNumber-1) // return the last one as it's by default result JSON column
        }
        val jsonInCsonList = (for(i<-0 until retList.length) yield GeneralEntityToCSON.
          serializeObject(new JSONRecord(retList(i)),null)._1).toIndexedSeq
        updateFinish(finished)
        outputWriter(0,0L,retList.length,jsonInCsonList)
      case a@_ => throw new Exception("HS task process got result in "+a)
    }
  }
  def isFinished:Boolean = opCursor>=ops.length-1
}
