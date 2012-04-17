package scaladays

private object util extends { val global = mkGlobal() } with GlobalUtil { }
import util._

object Demo {
  def main(args: Array[String]) { new SausageStretcher() showAll }
}

object Members {
  def main(args: Array[String]) { args foreach showMembersOf }
}

object Decls {
  def main(args: Array[String]) { args foreach showDeclsOf }
}
