package CSON.Types
import CSON._
import EntityInterface._
import CSON.{CSONWriter,WriteCursor}
import java.nio.ByteBuffer

class CSONArrayType(childSchemaOrType:Any) extends IElementType {
    typeCode = CSONTypes.Array.id.toByte
  val elementSchema = CSONElement.getSchema(childSchemaOrType)
  val elementType = CSONElement.getElementType(childSchemaOrType).getOrElse(null)
  // This function pointer should not be used, as this operation was performed in CSONElementArray constructor
    getValue = null
  writeRawFunc = (value:Any,outBB:ByteBuffer)=> {
    val writer = new CSONWriter(outBB)
    writeElement(value,writer.beginWriteComplexBody(outBB.position(),value.asInstanceOf[IEntitySequenceAccess].length),-1)
  }
  addValue = null
  getCompareValue = null

  writeElement = (value:Any,cur:WriteCursor,index:Int) => {
    var tc : Byte = CSONTypes.Array.id.toByte
    val thisWriter = cur.writer
    val itemCount = value.asInstanceOf[IEntitySequenceAccess].length
    if (value==null) tc = CSONTypes.NullValue.id.toByte

    val arrayCursor = if (index>=0) {
      thisWriter.writeComplexValueIndex(tc, cur, index)
      thisWriter.beginWriteComplexBody(cur.GetDataValueOffset, itemCount)
    }else cur

    // Begin write this element array in data area
    // Write a temp length value for this Element Array(Need to updated later)
    thisWriter.writeLength(arrayCursor, 0)
    thisWriter.writeCount(arrayCursor, itemCount)

    var n = 0
    value.asInstanceOf[CSONElementArray].iterator.foreach( (item:IElementValue) =>
      {item.elementType.writeElement(item.getValue,arrayCursor,n);n+=1})
      
    thisWriter.endWriteComplexBody(arrayCursor)
    // Update the parent cursor last available data area offset
    if (index>=0) cur.UpdateDataValueOffset(arrayCursor.GetDataValueOffset)
    }
}