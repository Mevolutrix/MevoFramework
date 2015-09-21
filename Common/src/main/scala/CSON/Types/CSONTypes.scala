package CSON.Types

object CSONTypes extends Enumeration {
  type CSONTypes = Value
  val BSONEOF = Value(0)
  val FloatingPoint = Value(1)
  val UTF8String = Value(2)
  val EmbeddedDocument = Value(3)
  val Array = Value(4)
  val BinaryData = Value(5)
  val Undefined = Value(6)
  val ObjectId = Value(7)
  val Boolean = Value(8)
  val UTCDatetime = Value(9)
  val NullValue = Value(10)
  val RegularExpression = Value(11)
  val DBPointer = Value(12)
  val JavaScriptCode = Value(0x0D)
  val Symbol = Value(0x0E)
  val JavaScriptCodeWScope = Value(0x0F)
  /** ��ʱû��֧�� */
  val Int32 = Value(0x10)
  val Timestamp = Value(0x11)
  val Int64 = Value(0x12)
  /*    val MinKey = Value(0xFF)
    val MaxKey = Value(0x7F) */
  val Decimal = Value(0x13)
  /** <summary>
    * ��ʾ�˴�Ϊ�����ԣ���Entity�����������
    * </summary> */
  val NullElement = Value(0x14)
  val Int8 = Value(0x15)
  val Int16 = Value(0x16)
  val Single = Value(0x17)
}