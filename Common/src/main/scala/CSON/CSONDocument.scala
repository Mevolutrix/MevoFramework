package CSON
import CSON.Types._
import EntityInterface._
import java.nio.{ByteOrder, ByteBuffer}

class CSONDocument extends IEntityRandomAccess with IEntitySequenceAccess {
  private var isDirty = false
  private var reader : CSONReader = null
  private var schema : IEntitySchema = null
  private var docBody : IEntityRandomAccess = new CSONElementArray(CSONTypesArray.NullType,None)
    def this(docSchema:IEntitySchema, buffer:Option[ByteBuffer]) {
    this
    schema = docSchema
    if (buffer.isDefined) {
      val dataBB:ByteBuffer = buffer.get.order(ByteOrder.LITTLE_ENDIAN)
      reader = new CSONReader(dataBB)
      docBody = new CSONComplexElement(schema,Some(reader.RootCursor))
    }
    else { isDirty=true; docBody = new CSONComplexElement(schema,None) }
  }
  override def getSchema : IEntitySchema = schema
  override def getValue(index:Any): Any = docBody.getValue(index)
  //get element instead of value.  e.g. Array, Complextype etc.
  override def getElement(index:Any):IElementValue = docBody.getElement(index)
  override def getRawValue(index:Any,NeedTypeCode:Boolean=true):Array[Byte] = docBody.getRawValue(index, NeedTypeCode)
  override def setValue(index:Any, value:AnyRef) = { docBody.setValue(index,value); isDirty=true }

  override def iterator:Iterator[IElementValue] = docBody.asInstanceOf[IEntitySequenceAccess].iterator
  override def append(value:Any) : Boolean = { isDirty=true; docBody.asInstanceOf[IEntitySequenceAccess].append(value) }
  override def length : Int = docBody.asInstanceOf[IEntitySequenceAccess].length

  private def retryToByteBuffer(dataBuf:ByteBuffer)(op:(ByteBuffer)=>ByteBuffer,retryCount:Int=2): ByteBuffer = {
    val lastPos = dataBuf.position()
    var ret = dataBuf
    var retrySign = true
    var retryTimes = 0 // loop 2 times
    while (retrySign && retryCount >= retryTimes) {
      try {
        op(ret)
        retrySign = false
      }
      catch {
        case e: Exception =>
          retryTimes += 1
          if (retryCount < retryTimes) throw e
          else {
            ret = ByteBuffer.allocate(ret.capacity() * 10).order(ByteOrder.LITTLE_ENDIAN)
            // Enlarge the double size buffer and copy the contents
            if (lastPos>0) {
              dataBuf.position(lastPos).flip()
              ret.put(dataBuf)
            }
          }
      }
    }
    ret
  }

  private def serialize(dataBuffer:ByteBuffer) = {
    val writer = new CSONWriter(dataBuffer)
    val cur = writer.beginWriteComplexBody(writer.beginOffset,length)
    docBody.asInstanceOf[IElementValue].elementType.writeElement(docBody,cur,-1)
    // Move the current position to the latest data area byte+1 position
    dataBuffer.position(cur.GetDataValueOffset)
    dataBuffer
  }
  /**
   * Get the document's body as one IElementValue, thus we can insert it into another CSON structure or read it into an object
   * Read ONLY. Don't try to set value or remove content inside this ElementValue. Because all your modification can't serialized.
   */
  def toCSONElement:IElementValue = docBody.asInstanceOf[IElementValue]

  def toByteBuffer(dataBuffer:ByteBuffer):ByteBuffer = {
    // Document was updated or appended with new element, then serialize
    if (reader != null) {
      if (isDirty) {
        isDirty=false
        retryToByteBuffer(dataBuffer)(dataBuf=>{this.serialize(dataBuf)})
      }else {
        // As the source ByteBuffer may be shared by multiple csonDocs in one IO buffer
        val csonBufferLength = reader.ReadLength(reader.RootCursor) + 4
        val arrayBuf = new Array[Byte](csonBufferLength)
        reader.dataBuffer.position(reader.RootCursor.LenOffset)
        reader.dataBuffer.get(arrayBuf)
        // defensive way is to copy the contents out to the new ByteBuffer
        if ((dataBuffer.capacity() - dataBuffer.position()) >= arrayBuf.length) dataBuffer.put(arrayBuf)
        else retryToByteBuffer(dataBuffer)(dataBuf => {
          dataBuf.put(arrayBuf)
        })
      }
    }
    else retryToByteBuffer(dataBuffer)(dataBuf=>{this.serialize(dataBuf)})
  }
  // As the csonDoc may be in dirty status(need to redo serialization) or with an long IO buffer read by multiple item
  // and may be read by not only one thread(this method is not thread safe), we copy the bytes to new array every time
  def getBytes() : Array[Byte] = {
    def getByteArrayBuffer():Tuple3[ByteBuffer,Int,Int] = {
      if (isDirty || reader==null) {
        var length = 4096
        val retByteBuf = toByteBuffer(ByteBuffer.allocate(length).order(ByteOrder.LITTLE_ENDIAN))
        length = retByteBuf.position()
        (retByteBuf,0,length)
      }
      else (reader.dataBuffer,reader.RootCursor.LenOffset,reader.ReadLength(reader.RootCursor) + 4)
    }
    // If there is original buffer and no dirty for this cson, just use null as buffer input as it return the original buf
    val (retByteBuffer,offset,length) = getByteArrayBuffer()
    val retByteArray = new Array[Byte](length)
    retByteBuffer.position(offset)
    retByteBuffer.get(retByteArray)
    retByteArray
  }
  /**
   *  if we got a ByteString with sequence of CSON objects, we must call this method after construct each cson docuement
   *  from this byte buffer (Thus the byteBuffer cursor moved the correct posion of next item. Do NOT FORGET!!!!)
   *  Called when finished reading CSONBuffer ByteBuffer, move the position of this ByteBuffer to CSONBuffer.end+1
   */
  def completeRead = reader.dataBuffer.position(reader.RootCursor.NumOffset +
                      reader.ReadLength(reader.RootCursor)).asInstanceOf[ByteBuffer]
}