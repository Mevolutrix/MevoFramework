package JSON
import java.text.SimpleDateFormat
import EnumHelper.EnumType.EnumType

object JsonString {
  def binaryToString(b:Array[Byte]):String = {
    val s = new StringBuilder(b.length*2)
    for(char<-b) {s.append((char & 0xff).formatted("%02x"))}
    s.toString()
  }
  def hexStrToBinary(s:String):Array[Byte] ={
    val b = new Array[Byte](s.length/2)
    for (i<-0 until b.length) b(i) = Integer.parseInt(s.substring(i*2,i*2+2), 16).toByte
    b
  }
  def apply(e:Any):String = e match {
    case null => "null"
    case b:Boolean => b.toString
    case n:Byte => n.toString
    case n: Int => n.toString
    case n: Long => n.toString
    case n: Float => n.toString
    case n: Double => n.toString
    case n: BigInt => n.toString()
    case n: BigDecimal => n.toString()
    case binary:Array[Byte] => "\"" + binaryToString(binary) + "\""
    case s: String => "\"" + s.replaceAll("\\\\", "\\\\\\\\").replaceAll("\\\"", "\\\\\"") + "\""
    case date:java.util.Date => "\"" + new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(date) + "\""
    case enum:EnumType => enum.id.toString
    case a@_ => "\"" + a.toString + "\""
  }
  def getString(e:Any):String = {
    e match {
      case null => "null"
      case b:Boolean => b.toString
      case n:Byte => n.toString
      case n: Int => n.toString
      case n: Long => n.toString
      case n: Float => n.toString
      case n: Double => n.toString
      case n: BigInt => n.toString()
      case n: BigDecimal => n.toString()
      case binary:Array[Byte] => binaryToString(binary)
      case date:java.util.Date => new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(date)
      case a@_ => a.toString
    }
  }
  def getHS_Str(e:Any):String = {
    e match {
      case null => "null"
      case b:Boolean => if (b) "1" else "0"
      case b:java.lang.Boolean => if (b) "1" else "0"
      case n:Byte => n.toString()
      case n: Int => n.toString()
      case n: Long => n.toString()
      case n: Float => n.toString()
      case n: Double => n.toString()
      case n: BigInt => n.toString()
      case n: BigDecimal => n.toString()
      case binary:Array[Byte] => binaryToString(binary)
      case date:java.util.Date => new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(date)
      case a@_ => a.toString
    }
  }
}