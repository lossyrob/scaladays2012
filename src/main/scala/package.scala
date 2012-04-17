import scala.tools.nsc._

package object scaladays {
  def mkGlobal(s: Settings = new Settings): Global = {
    val g = new Global(s)
    new g.Run   // initialize it
    g
  }
}