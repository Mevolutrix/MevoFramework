package CSON
import CSON.Types._
import java.util.{Date,UUID}
import java.nio.ByteBuffer
import EnumHelper.EnumType

/**
 * Writer to serialize a complex structure(Complex Element/Array) with specified number properties(or items in array)
 */
class WriteCursor(w:CSONWriter, StartPos: Int,count:Int) {
  import CSONStructure._
  import CSONTypes._
  val writer = w
  val LenOffset = StartPos
  val NumOffset = StartPos+LenthFieldSize
  val IndexArrayOffset = NumOffset + NumFieldSize
  val elementCount = count
  val DataOffset = IndexArrayOffset + (elementCount*IndexSegSize)
  private var lastDataPos = DataOffset

  def GetIndexTypeOffset(Id:Int) = IndexArrayOffset + (Id * IndexSegSize)
  //def GetIndexValueOffset(Id:Int) = IndexArrayOffset + (Id * IndexSegSize) + 1
  /**
   * Get the current available position in data area
   */
  def GetDataValueOffset = lastDataPos
  /**
   * Track the last writable position in this cursor's data area
   */
  def UpdateDataValueOffset(lastPos:Int) = lastDataPos=lastPos
  /**
   * Get the relative offset for current large size element(Used to save in the index area)
   */
  def DataAreaLength = lastDataPos-DataOffset
}

class CSONWriter(dBuf:ByteBuffer) {
  val dataBuffer = dBuf
  val beginOffset:Int = dataBuffer.position()
  private var lastDataPos : Int = 0
  private val divisor = BigDecimal(10000L)
  private def Seek2Index(cur:WriteCursor,index:Int) = dataBuffer.position(cur.GetIndexTypeOffset(index))
  /**
   * Seek to the current writable position in data Buffer(Current tail of DataBuffer area)
   */
  private def seek2DataBuf(cur:WriteCursor) = dataBuffer.position(cur.GetDataValueOffset)

  def beginWriteComplexBody(startPos:Int,count:Int):WriteCursor = new WriteCursor(this,startPos,count)
  /**
   * End the writing of specified complex element pointed by cursor
   */
  def endWriteComplexBody(cur:WriteCursor) = {
    seek2DataBuf(cur)
    dataBuffer.put(CSONTypes.BSONEOF.id.toByte)
    // get the total length by using last position of Data Area - Count field
    writeLength(cur, cur.GetDataValueOffset-cur.NumOffset)
  }
  def writeLength(cur:WriteCursor,len:Int) = { dataBuffer.position(cur.LenOffset); dataBuffer.putInt(len) }
  def writeCount(cur:WriteCursor,count:Int) = {dataBuffer.position(cur.NumOffset);dataBuffer.putInt(count) }
  def WriteElementType(typeCode:Byte,cur:WriteCursor,index:Int) = {
    Seek2Index(cur,index)
    dataBuffer.put(typeCode)
  }
  /**
   * Write the index area with element type code and relative offset for this element in data area
   */
  def writeComplexValueIndex(typeCode:Byte,cur:WriteCursor,index:Int) = {
    Seek2Index(cur,index)
    dataBuffer.put(typeCode)
    if (typeCode==CSONTypes.NullValue.id.toByte) dataBuffer.putInt(0)
    else {
      dataBuffer.putInt(cur.DataAreaLength)
    }
  }
  def writeValue(typeCode:Byte, value:Any, cur:WriteCursor, index:Int):Unit = {
    var tc = typeCode
    var elementType = CSONTypes(tc)
    if (value==null) { tc = CSONTypes.NullValue.id.toByte; elementType = CSONTypes.NullValue }
    WriteElementType(tc,cur,index)
    // Write the 4 Byte size value or the offset in data area
    var continue = false
    elementType match {
      case CSONTypes.Boolean => dataBuffer.putInt(if (value.asInstanceOf[Boolean])1 else 0)
      case CSONTypes.Int8 => dataBuffer.putInt(value.asInstanceOf[Byte])
      case CSONTypes.Int16 => dataBuffer.putInt(value.asInstanceOf[Short])
      case CSONTypes.Int32=> dataBuffer.putInt({
        if (value.isInstanceOf[Int])value.asInstanceOf[Int]
        else {
          val classType=value.getClass
          if (classType.isEnum ||classType.getName()=="scala.Enumeration$Val"
              ||classType.getName()=="scala.Enumeration$Value")
            value.asInstanceOf[CSONTypes.CSONTypes].id
          else throw new IllegalArgumentException("Unknown int type serialization for:"+classType.getName)
        }

      })
      case CSONTypes.NullElement => dataBuffer.putInt(0)
      case CSONTypes.NullValue => dataBuffer.putInt(0)
      case _ => dataBuffer.putInt(cur.DataAreaLength); continue = true
    }
    if (continue) writeDataArea(elementType,value, cur)
  }

  private def writeDataArea(elementType: CSONTypes.Value,value: Any, cur: CSON.WriteCursor): Unit = {
    seek2DataBuf(cur)
    elementType match {
      case CSONTypes.Int64 => dataBuffer.putLong(value.asInstanceOf[Long])
      case CSONTypes.Timestamp => dataBuffer.putLong(value.asInstanceOf[Long])
      case CSONTypes.UTCDatetime=> dataBuffer.putLong(value.asInstanceOf[Date].getTime())
      case CSONTypes.FloatingPoint => dataBuffer.putDouble(value.asInstanceOf[Double])
      case CSONTypes.Single => dataBuffer.putFloat(value.asInstanceOf[Float])
      case CSONTypes.ObjectId => {
        dataBuffer.putLong(value.asInstanceOf[UUID].getMostSignificantBits())
        dataBuffer.putLong(value.asInstanceOf[UUID].getLeastSignificantBits())
      }
      case CSONTypes.DBPointer => {
        dataBuffer.putLong(value.asInstanceOf[UUID].getMostSignificantBits())
        dataBuffer.putLong(value.asInstanceOf[UUID].getLeastSignificantBits())
      }
      case CSONTypes.Decimal=> {
        var v = value.asInstanceOf[BigDecimal]
        val signPart = v.signum
        v = v.abs
        dataBuffer.putInt(signPart)
        dataBuffer.putLong(v.longValue())
        dataBuffer.putInt(v.underlying().movePointRight(4).remainder(divisor).intValue())
      }
      case CSONTypes.UTF8String => {
        val strByteBuf = value.asInstanceOf[String].getBytes("UTF-8")
        dataBuffer.putInt(strByteBuf.length)
        dataBuffer.put(strByteBuf)
      }
      case CSONTypes.JavaScriptCode => {
        val strByteBuf = value.asInstanceOf[String].getBytes("UTF-8")
        dataBuffer.putInt(strByteBuf.length)
        dataBuffer.put(strByteBuf)
      }
      case CSONTypes.BinaryData => {
        val buf = value.asInstanceOf[Array[Byte]]
        dataBuffer.putInt(buf.length)
        dataBuffer.put(buf)
      }
      case _ => throw new Exception("Not implemented for this type")
    }
    cur.UpdateDataValueOffset(dataBuffer.position())
  }
}