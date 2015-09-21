package HandlerSocket.Protocol

object MySQLErrorCodes {
  val errCodeMap = Array[(String,Long,String)] (("121",100L,"Primary key conflict"),
    ("open_table",101L,"Table not exist."),("stmtnum",102L,"Statement number incorrect."),
    ("135",103L,"No more room in record file."),("fld",104L,"Index of set not match db table definition."))
  def apply(errCode:Long) = errCodeMap.find(p=>p._2==errCode).
        getOrElse((null,-1L,"No error matched")._3.asInstanceOf[String]
      )
}
object HandlerSocketException {
  def tryGetErrorDetail(details:String):Long = {
    MySQLErrorCodes.errCodeMap.find(p=>p._1==details).
      getOrElse((null,-1L,"No error matched"))._2.asInstanceOf[Long]
  }
}
