package CSON
/**
 *
 * The CSON reader and cursor class for reading element and move to next element in a CSON data
 */
import CSON.Types._
import java.nio.ByteBuffer

object CSONStructure  {
  val IndexSegSize = 5
  val LenthFieldSize = 4
  val NumFieldSize = 4
  val TypeCodeSize = 1
}

class CSONCursor(TheReader:CSONReader, StartPos: Int) {
import CSONStructure._
  val reader = TheReader
  val LenOffset = StartPos
  val NumOffset = StartPos+LenthFieldSize
  val IndexArrayOffset = NumOffset + NumFieldSize 
  val DataOffset = IndexArrayOffset + (reader.ReadCount(this)*IndexSegSize)
  
  def GetIndexTypeOffset(Id:Int) = IndexArrayOffset + (Id * IndexSegSize)
  
  def GetIndexValueOffset(Id:Int) = IndexArrayOffset + (Id * IndexSegSize) + TypeCodeSize
  
  def GetDataValueOffset(elementOffset:Int) = DataOffset + elementOffset
}
/**
 * CSON element and structure reader.
 * Input: ByteBuffer with position() pointing to the start of CSON data
 */
class CSONReader(dataBuf : ByteBuffer) {
import CSONStructure._
  val dataBuffer : ByteBuffer = dataBuf
  val RootCursor = new CSONCursor(this,dataBuffer.position())

  private def GetElementDataOffset(cur:CSONCursor, index:Int) = {
      dataBuffer.position((cur.GetIndexValueOffset(index)))
      dataBuffer.getInt()
  }
  private def SeekElementDataPos(cur:CSONCursor,index:Int) = dataBuffer.position(cur.GetDataValueOffset(GetElementDataOffset(cur,index)))

  def ReadLength(cursor:CSONCursor) = { dataBuffer.position(cursor.LenOffset); dataBuffer.getInt() }
  
  def ReadCount(cursor:CSONCursor) = {
    try {
      dataBuffer.position(cursor.NumOffset);
      dataBuffer.getInt()
    }
    catch {
      case e: IllegalArgumentException => throw new IllegalArgumentException("cursor:"+cursor+" | " + cursor.NumOffset)
      case _:Throwable => throw new Exception("Unknow exception.")
    }
  }
  /**
   * Read the type code of element pointed by index
   */
  def ReadElementType(cur:CSONCursor,index:Int) = {
    dataBuffer.position(cur.GetIndexTypeOffset(index));
    dataBuffer.get()
  }
  def ComplexElementOffset(cur:CSONCursor,index:Int) =  GetElementDataOffset(cur,index)

  def GetChildElementCursor(cur:CSONCursor, childElementIndex:Int):Option[CSONCursor] = {
    val ceTypeCode = CSONTypes(ReadElementType(cur,childElementIndex))
    if ( ceTypeCode==CSONTypes.Array || ceTypeCode==CSONTypes.EmbeddedDocument )
      Some(new CSONCursor(this,ComplexElementOffset(cur,childElementIndex)+cur.DataOffset))
    else None
  }
  def ReadBool(cur:CSONCursor, childElementIndex:Int) = {
    dataBuffer.position(cur.IndexArrayOffset+childElementIndex*IndexSegSize+1)
    dataBuffer.getInt()==1
  }
  def ReadInt(cur:CSONCursor, index:Int) = GetElementDataOffset(cur,index)
 
  def ReadStrBytes(cur:CSONCursor,index:Int) = {
    SeekElementDataPos(cur,index)
    val strBytes = new Array[Byte](dataBuffer.getInt())
    dataBuffer.get(strBytes)
    new String(strBytes,"UTF-8")
  }
  def ReadBuf(cur:CSONCursor,index:Int) = {
    SeekElementDataPos(cur,index)
    val len=dataBuffer.getInt()
    val buf : Array[Byte] = new Array[Byte](len)
    dataBuffer.get(buf)
    buf
  }
  def ReadFloat(cur:CSONCursor,index:Int) = {
    SeekElementDataPos(cur,index)
    dataBuffer.getFloat()
  }
  def ReadDouble(cur:CSONCursor,index:Int) = {
    SeekElementDataPos(cur,index)
    dataBuffer.getDouble()
  }
  def ReadLong(cur:CSONCursor,index:Int) = {
    SeekElementDataPos(cur,index)
    dataBuffer.getLong()
  }
  private def retrieveDecimal() : BigDecimal = {
    val signPart = dataBuffer.getInt()
    if (signPart == 0) return BigDecimal(signPart)
    val longPart : BigDecimal = BigDecimal(dataBuffer.getLong())
    val mantissaPart : BigDecimal = BigDecimal(dataBuffer.getInt().toLong,4)
    if (signPart<0) -(longPart+mantissaPart)
    else longPart+mantissaPart
  }
  def ReadDecimal(cur:CSONCursor,index:Int) = {
    SeekElementDataPos(cur,index)
    retrieveDecimal()
  }
  def ReadDatetime(cur:CSONCursor,index:Int) = {
    new java.util.Date(ReadLong(cur,index))
  }
  def ReadGuid(cur:CSONCursor,index:Int) = {
    SeekElementDataPos(cur,index)
    val highBits = dataBuffer.getLong()
    val LowBits = dataBuffer.getLong()
    new java.util.UUID(highBits,LowBits)
  }
  def ReadRawBuf(cur:CSONCursor,index:Int, length:Int, typeCode:Byte) = {
    val retBuf = new Array[Byte](length+1)
    retBuf(0) = typeCode
    SeekElementDataPos(cur,index)
    dataBuffer.get(retBuf, 1, length)
    retBuf
  }
  /**
   * Read variable size value in CSON raw value mode
   */
  def ReadRawLargeVaue(cur:CSONCursor,index:Int, typeCode:Byte) = {
    SeekElementDataPos(cur,index)
    val length = dataBuffer.getInt()
    val retBuf = new Array[Byte](length+1+4)
    retBuf(0) = typeCode
    SeekElementDataPos(cur,index)
    dataBuffer.get(retBuf,1,length+4)
    retBuf
  }
}