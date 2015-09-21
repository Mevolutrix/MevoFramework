package EntityInterface

trait IRequestContext {
  def alias : String
  def appSpaceID : String
  def userID:String
  def tenantID : Int
  def operationID : String
  def requestID : Int
}
object OrderByType extends Enumeration {
  type OderByType = Value
  val Ascending = Value(1)
  val Descending = Value(2)
}

import OrderByType._

trait IQueryRequest {
  def filters : String
  def params : Array[Object]
  def orderByColumns : Array[String]
  def orderByTypes : Array[OderByType]
  def selectColumns : String
  def groupBy  : String
  def groupValue : String
  def topCount : Int
}