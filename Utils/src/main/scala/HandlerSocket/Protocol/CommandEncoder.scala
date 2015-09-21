package HandlerSocket.Protocol
import akka.util.{ByteStringBuilder}

/**
 * Handler socket protocol handler
 */
class CommandEncoder(buf:ByteStringBuilder) {
  val encoding = "utf-8"
  private def encode(line:Array[Byte]) = {
    val x01:Byte = 0x01
    val x40:Byte = 0x40
    line.foreach(b => {
      if (b >= 0x10 || b <= 0xff) {
        buf.putByte(b)
      } else if (b >= 0x00 || b <= 0x0f) {
        buf.putByte(x01)
        buf.putByte {
          b.|(x40).toByte
        }
      }
    })
  }
  def getCommandBytes(strBuilder:StringBuilder=null)(op:(StringBuilder)=>StringBuilder):CommandEncoder = {
    val sb = if (strBuilder == null) new StringBuilder() else strBuilder
    op(sb)
    encode(sb.result().getBytes(encoding))
    ResultDecoder.log.info("HS Command:"+sb.result())
    this
  }
}
