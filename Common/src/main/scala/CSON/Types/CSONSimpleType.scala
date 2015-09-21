package CSON.Types
import CSON._
import EntityInterface._
import java.nio.ByteBuffer
import java.util.{Date,UUID}
import java.math.BigDecimal

class CSONSimpleType(elementTypeCode:Byte) extends IElementType {
  type Binary = Array[Byte]
    typeCode = elementTypeCode
    getValue = getValueFunc(elementTypeCode)
    writeRawFunc = getWriteValueFunc(elementTypeCode)
    addValue = getAddFunc(elementTypeCode)
    getCompareValue = getCompareFunc(elementTypeCode)
    writeElement = (value:Any,cur:WriteCursor,index:Int) => cur.writer.writeValue(typeCode, value, cur, index)
    def getTypeCode = typeCode
    val readArrayValue:(CSONElementArray,AnyRef) => Unit = getSimpleTypeArrayFunc(elementTypeCode)
    val writeArrayValue:(CSONElementArray)=> Any = getSimpleTypeArrayWriter(elementTypeCode)
    
    private def getSimpleTypeArrayWriter(typeCode:Byte): (CSONElementArray)=> Any = {
      CSONTypes(typeCode) match {
        case CSONTypes.Boolean => (elementArr:CSONElementArray)=> {
          val ret = new Array[Boolean](elementArr.length)
          for (i <- 0 until elementArr.length) ret.update(i,elementArr.getValue(i).asInstanceOf[Boolean])
          ret }
        case CSONTypes.Int8 => (elementArr:CSONElementArray)=> {
          val ret = new Array[Byte](elementArr.length)
          for (i <- 0 until elementArr.length) ret.update(i,elementArr.getValue(i).asInstanceOf[Byte])
          ret }
        case CSONTypes.Int16=> (elementArr:CSONElementArray)=> {
          val ret = new Array[Short](elementArr.length)
          for (i <- 0 until elementArr.length) ret.update(i,elementArr.getValue(i).asInstanceOf[Short])
          ret }
        case CSONTypes.Int32=> (elementArr:CSONElementArray)=> {
          val ret = new Array[Int](elementArr.length)
          for (i <- 0 until elementArr.length) ret.update(i,elementArr.getValue(i).asInstanceOf[Int])
          ret }
        case CSONTypes.FloatingPoint => (elementArr:CSONElementArray)=> {
          val ret = new Array[Double](elementArr.length)
          for (i <- 0 until elementArr.length) ret.update(i,elementArr.getValue(i).asInstanceOf[Double])
          ret }
        case CSONTypes.Single => (elementArr:CSONElementArray)=> {
          val ret = new Array[Float](elementArr.length)
          for (i <- 0 until elementArr.length) ret.update(i,elementArr.getValue(i).asInstanceOf[Float])
          ret }
        case CSONTypes.UTF8String => (elementArr:CSONElementArray)=> {
          val ret = new Array[String](elementArr.length)
          for (i <- 0 until elementArr.length) ret.update(i,elementArr.getValue(i).asInstanceOf[String])
          ret }
        case CSONTypes.BinaryData => (elementArr:CSONElementArray)=> {
          val ret = new Array[Binary](elementArr.length)
          for (i <- 0 until elementArr.length) ret.update(i,elementArr.getValue(i).asInstanceOf[Array[Byte]])
          ret }
        case CSONTypes.Decimal => (elementArr:CSONElementArray)=> {
          val ret = new Array[BigDecimal](elementArr.length)
          for (i <- 0 until elementArr.length) ret.update(i,elementArr.getValue(i).asInstanceOf[BigDecimal])
          ret }
        case CSONTypes.Int64 => (elementArr:CSONElementArray)=> {
          val ret = new Array[Long](elementArr.length)
          for (i <- 0 until elementArr.length) ret.update(i,elementArr.getValue(i).asInstanceOf[Long])
          ret }
        case CSONTypes.Timestamp => (elementArr:CSONElementArray)=> {
          val ret = new Array[Long](elementArr.length)
          for (i <- 0 until elementArr.length) ret.update(i,elementArr.getValue(i).asInstanceOf[Long])
          ret }
        case CSONTypes.UTCDatetime => (elementArr:CSONElementArray)=> {
          val ret = new Array[Date](elementArr.length)
          for (i <- 0 until elementArr.length) ret.update(i,elementArr.getValue(i).asInstanceOf[Date])
          ret }
        case CSONTypes.ObjectId => (elementArr:CSONElementArray)=> {
          val ret = new Array[UUID](elementArr.length)
          for (i <- 0 until elementArr.length) ret.update(i,elementArr.getValue(i).asInstanceOf[UUID])
          ret }
        case CSONTypes.DBPointer => (elementArr:CSONElementArray)=> {
          val ret = new Array[UUID](elementArr.length)
          for (i <- 0 until elementArr.length) ret.update(i,elementArr.getValue(i).asInstanceOf[UUID])
          ret }
        case CSONTypes.JavaScriptCode => (elementArr:CSONElementArray)=> {
          val ret = new Array[String](elementArr.length)
          for (i <- 0 until elementArr.length) ret.update(i,elementArr.getValue(i).asInstanceOf[String])
          ret }
        case _ => null
      }
    }
    private def getSimpleTypeArrayFunc(typeCode:Byte): (CSONElementArray,AnyRef) => Unit = {
      CSONTypes(typeCode) match {
        case CSONTypes.Boolean => (elementArr:CSONElementArray,arr:AnyRef)=> {
          val valueArr = arr.asInstanceOf[Array[Boolean]]
          for (v <- valueArr) elementArr.append(v)
        }
        case CSONTypes.Int8 => (elementArr:CSONElementArray,arr:AnyRef)=> {
          val valueArr = arr.asInstanceOf[Array[Byte]]
          for (v <- valueArr) elementArr.append(v)
        }
        case CSONTypes.Int16=> (elementArr:CSONElementArray,arr:AnyRef)=> {
          val valueArr = arr.asInstanceOf[Array[Short]]
          for (v <- valueArr) elementArr.append(v)
        }
        case CSONTypes.Int32=> (elementArr:CSONElementArray,arr:AnyRef)=> {
          val valueArr = arr.asInstanceOf[Array[Int]]
          for (v <- valueArr) elementArr.append(v)
        }
        case CSONTypes.FloatingPoint => (elementArr:CSONElementArray,arr:AnyRef)=> {
          val valueArr = arr.asInstanceOf[Array[Double]]
          for (v <- valueArr) elementArr.append(v)
        }
        case CSONTypes.Single => (elementArr:CSONElementArray,arr:AnyRef)=> {
          val valueArr = arr.asInstanceOf[Array[Float]]
          for (v <- valueArr) elementArr.append(v)
        }
        case CSONTypes.UTF8String => (elementArr:CSONElementArray,arr:AnyRef)=> {
          val valueArr = arr.asInstanceOf[Array[String]]
          for (v <- valueArr) elementArr.append(v)
        }
        case CSONTypes.BinaryData => (elementArr:CSONElementArray,arr:AnyRef)=> {
          val valueArr = arr.asInstanceOf[Array[Binary]]
          for (v <- valueArr) elementArr.append(v)
        }
        case CSONTypes.Decimal => (elementArr:CSONElementArray,arr:AnyRef)=> {
          val valueArr = arr.asInstanceOf[Array[BigDecimal]]
          for (v <- valueArr) elementArr.append(v)
        }
        case CSONTypes.Int64 => (elementArr:CSONElementArray,arr:AnyRef)=> {
          val valueArr = arr.asInstanceOf[Array[Long]]
          for (v <- valueArr) elementArr.append(v)
        }
        case CSONTypes.Timestamp => (elementArr:CSONElementArray,arr:AnyRef)=> {
          val valueArr = arr.asInstanceOf[Array[Long]]
          for (v <- valueArr) elementArr.append(v)
        }
        case CSONTypes.UTCDatetime => (elementArr:CSONElementArray,arr:AnyRef)=> {
          val valueArr = arr.asInstanceOf[Array[Date]]
          for (v <- valueArr) elementArr.append(v)
        }
        case CSONTypes.ObjectId => (elementArr:CSONElementArray,arr:AnyRef)=> {
          val valueArr = arr.asInstanceOf[Array[UUID]]
          for (v <- valueArr) elementArr.append(v)
        }
        case CSONTypes.DBPointer => (elementArr:CSONElementArray,arr:AnyRef)=> {
          val valueArr = arr.asInstanceOf[Array[Long]]
          for (v <- valueArr) elementArr.append(v)
        }
        case CSONTypes.JavaScriptCode => (elementArr:CSONElementArray,arr:AnyRef)=> {
          val valueArr = arr.asInstanceOf[Array[String]]
          for (v <- valueArr) elementArr.append(v)
        }
        case _ => null
      }
    }
  private def getValueFunc(typeCode:Byte):(CSONCursor, Int)=>Any = {
      CSONTypes(typeCode) match {
        case CSONTypes.Boolean => (cur:CSONCursor,index:Int)=> { cur.reader.ReadBool(cur, index) }
        case CSONTypes.Int8 => (cur:CSONCursor,index:Int)=> { cur.reader.ReadInt(cur, index).toByte }
        case CSONTypes.Int16=> (cur:CSONCursor,index:Int)=> { cur.reader.ReadInt(cur, index).toShort }
        case CSONTypes.Int32=> (cur:CSONCursor,index:Int)=> { cur.reader.ReadInt(cur, index) }
        case CSONTypes.FloatingPoint => (cur:CSONCursor,index:Int)=> {cur.reader.ReadDouble(cur, index)}
        case CSONTypes.Single => (cur:CSONCursor,index:Int)=> {cur.reader.ReadFloat(cur, index)}
        case CSONTypes.UTF8String => (cur:CSONCursor,index:Int)=> {cur.reader.ReadStrBytes(cur, index)}
        case CSONTypes.BinaryData => (cur:CSONCursor,index:Int)=> {cur.reader.ReadBuf(cur, index)}
        case CSONTypes.Decimal => (cur:CSONCursor,index:Int)=> {cur.reader.ReadDecimal(cur, index)}
        case CSONTypes.Int64 => (cur:CSONCursor,index:Int)=> {cur.reader.ReadLong(cur, index)}
        case CSONTypes.Timestamp => (cur:CSONCursor,index:Int)=> {cur.reader.ReadLong(cur, index)}
        case CSONTypes.UTCDatetime => (cur:CSONCursor,index:Int)=> {cur.reader.ReadDatetime(cur, index)}
        case CSONTypes.ObjectId => (cur:CSONCursor,index:Int)=> {cur.reader.ReadGuid(cur, index)}
        case CSONTypes.DBPointer => (cur:CSONCursor,index:Int)=> {cur.reader.ReadGuid(cur, index)}
        case CSONTypes.JavaScriptCode => (cur:CSONCursor,index:Int)=> {cur.reader.ReadStrBytes(cur, index)}
        case CSONTypes.NullValue =>(cur:CSONCursor,index:Int)=> {null}
        case CSONTypes.NullElement =>(cur:CSONCursor,index:Int)=> {null}
        case _ => null
      }
  }

    private def getWriteValueFunc(typeCode:Byte):Function2[Any,ByteBuffer,Unit] = {
      CSONTypes.apply(typeCode) match {
        case CSONTypes.Boolean => (value:Any,outBB:ByteBuffer)=> outBB.putInt(if (value.asInstanceOf[Boolean])1 else 0 )
        case CSONTypes.Int8 => (value:Any,outBB:ByteBuffer)=> outBB.putInt(value.asInstanceOf[Byte])
        case CSONTypes.Int16=> (value:Any,outBB:ByteBuffer)=> outBB.putInt(value.asInstanceOf[Short])
        case CSONTypes.Int32=> (value:Any,outBB:ByteBuffer)=> outBB.putInt(value.asInstanceOf[Int])
        case CSONTypes.FloatingPoint => (value:Any,outBB:ByteBuffer)=> outBB.putDouble(value.asInstanceOf[Double])
        case CSONTypes.Single => (value:Any,outBB:ByteBuffer)=> outBB.putFloat(value.asInstanceOf[Float])
        case CSONTypes.UTF8String => (value:Any,outBB:ByteBuffer)=> {
            val strByteBuf = value.asInstanceOf[String].getBytes("UTF-8")
            outBB.putInt(strByteBuf.length)
            outBB.put(strByteBuf)}
        case CSONTypes.JavaScriptCode => (value:Any,outBB:ByteBuffer)=> {
            val strByteBuf = value.asInstanceOf[String].getBytes("UTF-8")
            outBB.putInt(strByteBuf.length)
            outBB.put(strByteBuf)}
        case CSONTypes.BinaryData => (value:Any,outBB:ByteBuffer)=> {
          outBB.putInt(value.asInstanceOf[Array[Byte]].length)
          outBB.put(value.asInstanceOf[Array[Byte]])}
        case CSONTypes.Decimal => (value:Any,outBB:ByteBuffer)=> {
          var v = value.asInstanceOf[BigDecimal]
          val signPart : Int = v.signum
          v = v.abs
          val longPart : Long = v.longValue
          val mantissaPart : Int = v.movePointRight(4).remainder(new BigDecimal("10000")).intValue()
          outBB.putLong(longPart)
          outBB.putInt(mantissaPart)
          outBB.putInt(signPart)
        }
        case CSONTypes.Int64 => (value:Any,outBB:ByteBuffer)=> outBB.putLong(value.asInstanceOf[Long])
        case CSONTypes.Timestamp => (value:Any,outBB:ByteBuffer)=> outBB.putLong(value.asInstanceOf[Long])
        case CSONTypes.UTCDatetime => (value:Any,outBB:ByteBuffer)=> outBB.putLong(value.asInstanceOf[Date].getTime())
        case CSONTypes.ObjectId => (value:Any,outBB:ByteBuffer)=> {
          val v = value.asInstanceOf[java.util.UUID]
          outBB.putLong(v.getMostSignificantBits())
          outBB.putLong(v.getLeastSignificantBits())}
        case CSONTypes.DBPointer => (value:Any,outBB:ByteBuffer)=> {
          val v = value.asInstanceOf[java.util.UUID]
          outBB.putLong(v.getMostSignificantBits())
          outBB.putLong(v.getLeastSignificantBits())}

        case CSONTypes.NullValue => (value:Any,outBB:ByteBuffer)=> outBB.putInt(0)

        case CSONTypes.NullElement => (value:Any,outBB:ByteBuffer)=> outBB.putInt(0)
        case _ => null
      }
    }
    private def getAddFunc(typeCode:Byte):(Any,Any)=>Any = {
        CSONTypes.apply(typeCode) match {
        case CSONTypes.Int8 => (e1:Any,e2:Any)=> {e1.asInstanceOf[Byte]+e2.asInstanceOf[Byte]}
        case CSONTypes.Int16=> (e1:Any,e2:Any)=> {e1.asInstanceOf[Short]+e2.asInstanceOf[Short]}
        case CSONTypes.Int32=> (e1:Any,e2:Any)=> {e1.asInstanceOf[Int]+e2.asInstanceOf[Int]}
        case CSONTypes.FloatingPoint => (e1:Any,e2:Any)=> {e1.asInstanceOf[Double]+e2.asInstanceOf[Double]}
        case CSONTypes.Single => (e1:Any,e2:Any)=> {e1.asInstanceOf[Float]+e2.asInstanceOf[Float]}
        case CSONTypes.UTF8String => (e1:Any,e2:Any)=> {e1.asInstanceOf[String]+e2.asInstanceOf[String]}
        case CSONTypes.Decimal => (e1:Any,e2:Any)=> {e1.asInstanceOf[BigDecimal].add(e2.asInstanceOf[BigDecimal])}
        case CSONTypes.Int64 => (e1:Any,e2:Any)=> {e1.asInstanceOf[Long]+e2.asInstanceOf[Long]}
        //case CSONTypes.JavaScriptCode => (e1:Any,e2:Any)=> {e1.asInstanceOf[String]+e2.asInstanceOf[String]}
        case _ => null
      }
  }
    private def getCompareFunc(typeCode:Byte):(Any,Any)=>Any = {
        case CSONTypes.Int8 => (x:Any,y:Any) => {x.asInstanceOf[Byte].compareTo(y.asInstanceOf[Byte])}
        case CSONTypes.Int16=> (x:Any,y:Any) => {x.asInstanceOf[Byte].compareTo(y.asInstanceOf[Byte])}
        case CSONTypes.Int32=> (x:Any,y:Any) => {x.asInstanceOf[Byte].compareTo(y.asInstanceOf[Byte])}
        case CSONTypes.FloatingPoint => (x:Any,y:Any)=> {x.asInstanceOf[Double].compareTo(y.asInstanceOf[Double])}
        case CSONTypes.Single => (x:Any,y:Any)=> {x.asInstanceOf[Float].compareTo(y.asInstanceOf[Float])}
        case CSONTypes.UTF8String => (x:Any,y:Any)=> {x.asInstanceOf[String].compareTo(y.asInstanceOf[String])}
        case CSONTypes.Decimal => (x:Any,y:Any)=> {x.asInstanceOf[BigDecimal].compareTo(y.asInstanceOf[BigDecimal])}
        case CSONTypes.Int64 => (x:Any,y:Any)=> {x.asInstanceOf[Long].compareTo(y.asInstanceOf[Long])}
        case CSONTypes.Timestamp => (x:Any,y:Any)=> x.asInstanceOf[Long].compareTo(y.asInstanceOf[Long])
        case CSONTypes.UTCDatetime => (x:Any,y:Any)=> x.asInstanceOf[Date].compareTo(y.asInstanceOf[Date])
        case CSONTypes.ObjectId => (x:Any,y:Any)=> x.asInstanceOf[UUID].compareTo(y.asInstanceOf[UUID])
        //case CSONTypes.DBPointer => (x:Any,y:Any)=> x.asInstanceOf[UUID].compareTo(y.asInstanceOf[UUID])
        //case CSONTypes.JavaScriptCode => (x:Any,y:Any)=> x.asInstanceOf[String].compareTo(y.asInstanceOf[String])
        case CSONTypes.NullValue => (x:Any,y:Any)=> {0}
        case CSONTypes.NullElement => (x:Any,y:Any)=> {0}
        case _ => null
  }
}
