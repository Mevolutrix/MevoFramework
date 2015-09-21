package mevolutrix.ServiceRT
import SVCInterface.ISvcHandler

/**
 * Processing SVC request and load/call the module for the appSpace
 */
object SVCHandler {
  def apply(alias:String) = SVCConfig(alias).newInstance().asInstanceOf[ISvcHandler]
}
