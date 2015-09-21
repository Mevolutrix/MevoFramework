package HandlerSocket.Protocol
import java.io.ByteArrayOutputStream
import akka.util.ByteString
import java.util

import org.slf4j.LoggerFactory

class TokenEnumerator(input:ByteString) {
  private var v:ByteString = input
  def filter(bytes:ByteString):String = {
    val out = new ByteArrayOutputStream
    var convert = false
    bytes.foreach(ch =>{
      if (ch == 0x01) convert = true
      else if (convert) {
        convert = false
        out.write(ch^0x40)
      } else out.write(ch)
    })
    new String(out.toByteArray, ResultDecoder.encoding)
  }
  def readToken(): String = {
    val tokenBytes = v.takeWhile(ch => (ch!='\t') )
    v = v.drop(tokenBytes.size+1) // skip the '\t' and sliding forward
    filter(tokenBytes)
  }
  def left = v
}
object ResultDecoder {
  val encoding = "utf-8"
  val log = LoggerFactory.getLogger("HandlerSocket")
  def assembly(input:ByteString):HsResult = {
    val source = new TokenEnumerator(input)
    val errorCode = source.readToken().toInt
    val columnNumber = source.readToken().toInt
    if (source.left.size<=1) HsResult(errorCode, columnNumber, Array[String]())
    else {
      val colList = new util.ArrayList[String]()
      //Fix: Change from >1 to >=1 as one result:"0" was dropped at last char
      while (source.left.size>=1) colList.add(source.readToken())
      val columns:Array[String] = new Array[String](colList.size())
      colList.toArray[String](columns)
      HsResult(errorCode, columnNumber,columns)
    }
  }
}