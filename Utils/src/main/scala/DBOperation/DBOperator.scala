package DBOperation
import java.sql.{SQLException, DriverManager, Statement, Connection}

object DBOperator {
  def getStatement(dbConnString:String,usr:String,pwd:String): (Connection, Statement) = {
    val conn = getConn(dbConnString:String,usr:String,pwd:String)
    (conn,conn.createStatement())
  }
  def getConn(dbConnString:String,usr:String,pwd:String): Connection = {
    val conn_str = "jdbc:mysql://" + dbConnString
    Class.forName("com.mysql.jdbc.Driver")
    val conn = DriverManager.getConnection(conn_str, usr, pwd)
    conn
  }
  def executeOperation(dbConnString:String,usr:String,pwd:String,op:(Connection)=>Boolean,trx:Boolean=true):Boolean = {
    var ret:Boolean = false
    val conn = getConn(dbConnString, usr, pwd)
    try {
      conn.setAutoCommit(!trx)  // Set false means all operations are in one transaction
      if (trx) {
        if (op(conn)) {
          conn.commit()
          ret = true
        }
        else {
          conn.rollback()
        }
      } else op(conn)
    } catch {
      case e:SQLException => if (trx) {conn.rollback(); throw e}
    } finally {
      conn.close()
    }
    ret
  }
}
