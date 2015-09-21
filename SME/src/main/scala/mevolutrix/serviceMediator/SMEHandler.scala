package mevolutrix.serviceMediator
import mevolutrix.Interface.ISmeHandler
import mevolutrix.engineConfig.SMEConfig

/**
 * Used to implement SME dynamic service load
 */
object SMEHandler {
  def apply(appSpace:String) = SMEConfig(appSpace).newInstance().asInstanceOf[ISmeHandler]
}
