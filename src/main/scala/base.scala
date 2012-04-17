package scaladays

import scala.tools.nsc.{ Global, Settings }

class SymbolOrganizer[G <: Global with Singleton](val global: G, typeName: String)(condition: Global#Symbol => Boolean) 
        extends GlobalUtil {
  import global._
  import definitions._

  // Symbol for type member of Global with name `typeName`
  val baseSymbol = typeMember(globalClass, typeName)
  println("\n***** Now calculating organization of " + baseSymbol.fullLocationString + "\n")

  // All members of Global which descend from baseSymbol
  def descendants = (
    globalClass.info.members
      filter (m => (m isSubClass baseSymbol) || (m.moduleClass isSubClass baseSymbol))
      filter (m => condition(m.initialize))
  )
  
  pp("%s has %s qualifying descendants:\n\n%s".format(
    baseSymbol, descendants.size, ppWrap(70)(descendants.map(_.name.toString).sorted)))

  // All case classes/objects among descendants
  def caseDescendants = descendants filter (_.initialize.isCase)

  // All traits among descendants
  def traitDescendants = descendants filter (_.initialize.isTrait)
  
  // The direct parents of clazz which descend from baseSymbol, with redundant superclasses eliminated
  def coveringParents(clazz: Symbol) = elimSupers(clazz.parentSymbols filter (_ isSubClass baseSymbol))
  
  // The closest ancestor of clazz which descends from baseSymbol
  def closestParent(clazz: Symbol) = coveringParents(clazz).headOption.fold(NoSymbol: Symbol)(x => x)

  // All the descendents of baseSymbol, grouped by their first covering parent
  def closestChildrenMap = (
    descendants 
            map (clazz => (closestParent(clazz), clazz))
      filterNot (_._1 eq NoSymbol)
        groupBy (_._1)
      mapValues (xs => xs map (_._2) sortBy (_.nameString))
  )
  
  // The pairs in closestChildrenMap, sorted by fewest ancestors
  def closestChildren = closestChildrenMap.toList sortBy (_._1.ancestors.size)

  // The as-complete-as-we're-going-for signature of one of our case class/objects
  def signature(clazz: Symbol) = (
    "%s%6s %s%s extends %s".format(
      if (clazz.isCase) "case " else "",
      clazz.kindString, 
      clazz.name, 
      constructorString(clazz),
      coveringParents(clazz) map (_.name) mkString ", "
    )
  )
  
  // The members of "Team `typeName`."
  def showSausage() = {
    for ((parent, children) <- closestChildren) {
      pp("\n" + parent.fullLocationString + " is the most beloved parent of " + children.size + " children:")
      children.map(t => "  " + signature(t)).sorted foreach pp
    }  
  }
}
object SymbolOrganizer {
  def apply[G <: Global with Singleton](global: G, typeName: String) = {
    (condition: Global#Symbol => Boolean) => new SymbolOrganizer(global, typeName)(condition)
  }
}

class SausageStretcher(global: Global) {
  def this() = this(mkGlobal())

  import global._
  import definitions.{ getRequiredModule, SpecializedClass }
  
  private val treeOrg = SymbolOrganizer[global.type](global, "Tree")(_.isCase)
  private val typeOrg = SymbolOrganizer[global.type](global, "Type")(_.isCase)
  private val symOrg  = SymbolOrganizer[global.type](global, "Symbol")(_ => true)

  def showTreeOrg() {
    treeOrg.showSausage()
  }
  
  def showMiscOrg() {
    // A brief look at tree traits with new declarations
    def showTreeTraitDeclarations() = {
      import treeOrg._
      for (parent <- traitDescendants) {
        val members = parent.info.decls.toList.filter(s => s.initialize.isPublic && !s.isConstructor && !s.isSetter)
        if (members.nonEmpty)
          treeOrg.pp(members.map(m => "  def " + paramString(m)).mkString("\n" + parent + " {\n", "\n", "\n}"))
      }
    }
    showTreeTraitDeclarations()

    import symOrg._
    // Members of a given package which satisfy the predicate
    def pkgMembersWhich(pkg: String)(p: Symbol => Boolean) = (
      // Playing defense against the compiler's ill-preparedness for this sort of assault
      // As a general rule please don't swallow all the exceptions which come your way
      getRequiredModule(pkg).info.members filter (m => try p(m) catch { case t => false })
    )
    def pkgMembersWithTparamWhich(pkg: String)(p: Symbol => Boolean) = 
      pkgMembersWhich(pkg)(_.typeParams exists p)

    def specializedClassesIn(pkg: String) = (
         ("\nSpecialized classes in package " + pkg + "")
      +: (pkgMembersWithTparamWhich(pkg)(_ hasAnnotation SpecializedClass).map("  " + _.defString))
    )
    
    pps(specializedClassesIn("scala.runtime"))
    pps(specializedClassesIn("scala"))

    showMembersOf[Global]
    showMembersOf[Traversable[Int]]
  }

  def showAll() {
    treeOrg.showSausage()
    symOrg.showSausage()
    typeOrg.showSausage()

    showMiscOrg()
  }
}
