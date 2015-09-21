package HandlerSocket.Protocol

import java.util.ArrayList
import java.util

sealed abstract class Op(){def symbol():String}
case class Eq(symbol:String="=") extends Op
case class Gt(symbol:String=">") extends Op
case class Lt(symbol:String="<") extends Op
case class GE(symbol:String=">=") extends Op
case class LE(symbol:String="<=") extends Op
case class Ins(symbol:String="+")  extends Op

case class Row(columns:Array[String],columnNumber:Int, offset:Int=0) {
  if (offset<0) throw new IllegalArgumentException(offset+" is an illegal number for columns array.")
  def column(i:Int):String = {
    if(i<0 || i >= columnNumber){ throw new IllegalArgumentException(i+" is an illegal index for columns array.")}else{ columns(i+offset)}
  }
}
object FilterType extends Enumeration {
  type FilterType = Value
  val FILTER = Value("F")
  val WHILE = Value("W")
}

import FilterType._
abstract class HsCommand {
  def encodeValue(buffer:StringBuilder,v:String):StringBuilder = {
    if (v != null) buffer.append(v) else buffer.append('\0')
    buffer
  }
  def toCommand(indexId:Int, cmdEncoder:CommandEncoder):Unit
}
case class Filter(fType:FilterType,findOperator:Op,col:Int,value:String)

case class HsResult(errorCode:Int, columnNumber:Int, columns:Array[String]) {
  def iterator():util.Collection[Row] = {
    if(columns == null || columns.length == 0) return new ArrayList[Row](0)

    val rowCount = columns.length / columnNumber
    val rows = new ArrayList[Row](rowCount)
    for (cursor <-0 until rowCount) rows.add(cursor, Row(columns, columnNumber, cursor*columnNumber))
    rows
  }
}
case class OpenIndexSpec(db:String, table:String, columns:Array[String],
                         index:String="PRIMARY",filterCols:Array[String]) extends HsCommand
{
  def toCommand(indexId:Int, cmdEncoder:CommandEncoder) = {
    cmdEncoder.getCommandBytes()((buf)=>{
      buf.append("P\t").append(indexId).append("\t").append(db).append("\t").append(table).append("\t").append(index).append("\t")
      columns.foreach(buf.append(_).append(","))
      if (filterCols!=null && filterCols.length>0) {
        buf.deleteCharAt(buf.length-1).append("\t")
        filterCols.foreach(buf.append(_).append(","))
      }
      buf.deleteCharAt(buf.length-1)
      buf.append("\n")
    })
  }
}
case class Get(op:Op, indexValues:Array[String], lm:(Int,Int)=(20000,0),filters:Seq[Filter]=null) extends HsCommand
{
  def toCommand(indexId:Int, cmdEncoder:CommandEncoder) = {
    cmdEncoder.getCommandBytes()((buf) => {
      buf.append(indexId).append("\t").append(op.symbol).append("\t").append(indexValues.length).append("\t")
      indexValues.foreach(encodeValue(buf,_).append("\t") )
      buf.append(lm._1).append("\t").append(lm._2)
      if (filters!=null && filters.length>0) filters.foreach(f=> {
        buf.append("\t").append(f.fType.toString).append("\t").append(f.findOperator.symbol()).
          append("\t").append(f.col).append("\t").append(f.value)
      })
      buf.append("\n")
    })
  }
}
case class Update(op:Op, indexValues:Array[String], colValues:Array[String],lm:(Int,Int)=(1,0), mop:Char='U') extends  HsCommand
{
  def toCommand(indexId:Int, cmdEncoder:CommandEncoder) = {
    cmdEncoder.getCommandBytes()((buf) => {
      buf.append(indexId).append("\t").append(op.symbol).append("\t").append(indexValues.length).append("\t")
      indexValues.foreach(encodeValue(buf,_).append("\t"))
      buf.append(lm._1).append("\t").append(lm._2).append("\t").append(mop).append("\t")
      colValues.foreach(encodeValue(buf,_).append("\t") )
      buf.deleteCharAt(buf.length-1).append("\n")
    })
  }
}
case class Delete(op:Op, indexValues:Array[String], lm:(Int,Int)=(1,0), mop:Char='D') extends  HsCommand
{
  def toCommand(indexId:Int, cmdEncoder:CommandEncoder) = {
    cmdEncoder.getCommandBytes()((buf) => {
      buf.append(indexId).append("\t").append(op.symbol).append("\t").append(indexValues.length).append("\t")
      indexValues.foreach(encodeValue(buf,_).append("\t"))
      buf.append(lm._1).append("\t").append(lm._2).append("\t").append(mop).append("\n")
    })
  }
}
case class Insert(colValues:Array[String], op:Op=Ins()) extends  HsCommand
{
  def toCommand(indexId:Int, cmdEncoder:CommandEncoder) = {
    cmdEncoder.getCommandBytes()((buf) => {
      buf.append(indexId).append("\t").append(op.symbol).append("\t").append(colValues.length).append("\t")
      colValues.foreach(encodeValue(buf,_).append("\t"))
      buf.deleteCharAt(buf.length-1).append("\n")
    })
  }
}