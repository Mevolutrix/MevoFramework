package CSON.Types
import EntityInterface._
import java.nio.ByteBuffer
import CSON.{CSONWriter,WriteCursor,CSONCursor,CSONReader}
import CSON.CSONComplexElement

class CSONComplexType(typeSchema:IEntitySchema) extends IElementType {
  typeCode = CSONTypes.EmbeddedDocument.id.toByte
  val schema = typeSchema
    //
  getValue = null
    /**
     * ���Complex Element��RawValue��ȡֵ����
     */
  writeRawFunc = (value:Any,outBB:ByteBuffer)=> {
    val writer = new CSONWriter(outBB)
    writeElement(value,writer.beginWriteComplexBody(writer.beginOffset,schema.count),-1)
  }
  addValue = (e1:Any,e2:Any)=> throw new Exception("Complex element can't be applied with add operation.")
  getCompareValue = (x:Any,y:Any) => throw new Exception("Complex element can't be applied with compare operation.")

  /**
   * CSON���л�ComplexElement����index<0��cur���α꼴��ָ�����DataAre�������index ����
   */
  writeElement = (value:Any,cur:WriteCursor,index:Int) => {
    var tc : Byte = CSONTypes.EmbeddedDocument.id.toByte
    val thisWriter = cur.writer
    if (value==null) tc = CSONTypes.NullValue.id.toByte

    val complexElementCursor = if (index>=0) {
      thisWriter.writeComplexValueIndex(tc, cur, index)
      thisWriter.beginWriteComplexBody(cur.GetDataValueOffset, schema.count)
    }else cur

    // Begin write this complex element in data area
    // Write a temp length value for this Complex Element(Need to updated later)
    thisWriter.writeLength(complexElementCursor, 0)
    thisWriter.writeCount(complexElementCursor, schema.count)

    var n = 0
    value.asInstanceOf[CSONComplexElement].iterator.foreach( (item:IElementValue) => {
      try {
        if (item!=null)
          item.elementType.writeElement(item.getValue,complexElementCursor,n)
        else CSONTypesArray.NullType.writeElement(null,complexElementCursor,n)
        n+=1
      } catch {
        case e:Exception =>
          throw new IllegalArgumentException("Property:"+ value.asInstanceOf[CSONComplexElement]
          .getSchema.getPropertyName(n)+" with error:"+e.toString)
      }
    })
      
    thisWriter.endWriteComplexBody(complexElementCursor)
    // Update the parent cursor last available data area offset
    if (index>=0) cur.UpdateDataValueOffset(complexElementCursor.GetDataValueOffset)
    }
}