package scaladays

import scala.tools.nsc.Global

trait GlobalUtil {
  val global: Global
  import global._
  import definitions._

  implicit def weLoveOurManifests[T](m: Manifest[T]): Symbol = manifestToSymbol(m)  
  
  // Symbol for class "Global"
  val globalClass = getRequiredClass(global.getClass.getName)
  
  // Eliminating noisy Tree.this, Types.this etc. qualifiers
  def clean(x: Any)      = ("" + x).replaceAll("""\w+\.this\.""", "")
  def pp(x: Any)         = println(clean(x))
  def pps(xs: List[Any]) = xs foreach pp
  
  // Hacky late night column wrap
  def ppWrap(col: Int)(xs: List[Any]) = {
    val buf = new StringBuilder
    var counter = col
    def app(s: String) = {
      buf append s
      counter -= s.length
    }
    
    for (List(l, r) <- xs map ("" + _) sliding 2 map (_.toList)) {
      if (buf.isEmpty)
        app(l)

      val comma = if (counter <= 0) { counter = col ; ",\n" } else ", "
      app(comma + r)
    }
    buf.result
  }

  def showMembersOf[T: Manifest] : Unit      = showMembersOf(manifestToSymbol(manifest[T]))
  def showMembersOf(className: String): Unit = showMembersOf(getClassIfDefined(className))
  def showMembersOf(clazz: Symbol): Unit     = showInnards("members", clazz, _.info.members)

  def showDeclsOf[T: Manifest] : Unit      = showDeclsOf(manifestToSymbol(manifest[T]))
  def showDeclsOf(className: String): Unit = showDeclsOf(getClassIfDefined(className))
  def showDeclsOf(clazz: Symbol): Unit     = showInnards("decls", clazz, _.info.decls.toList)
    
  private def showInnards(what: String, clazz: Symbol, fn: Symbol => List[Symbol]) {
    def forceSyms(syms: List[Symbol]) {
      syms foreach (sym => force(sym.info))
    }
    def force(info: Type) {
      info match {
        case PolyType(tparams, restpe)        => forceSyms(tparams) ; force(restpe)
        case MethodType(params, restpe)       => forceSyms(params) ; force(restpe)
        case ExistentialType(tparams, restpe) => forceSyms(tparams) ; force(restpe)
        case TypeRef(_, sym, _)               => forceSyms(sym :: sym.typeParams)
        case _                                => 
      }
    }
    forceSyms(fn(clazz))

    val members               = fn(clazz) filter (_.initialize.isPublic) sortBy (_.nameString)
    val (tpes, terms)         = members partition (_.isType)
    val (classes, nonClasses) = tpes partition (_.isClass)
    val (methods, nonMethods) = terms partition (m => m.isMethod && !m.isLazy)
    
    def msg(label: String, syms: List[Symbol]) = (
      if (syms.nonEmpty) {
        pps(("\n***** " + label + " " + what + " of " + clazz + " *****\n\n") :: syms.map(_.defString))
      }
    )
    
    msg("Class", classes)
    msg("Non-class type", nonClasses)
    msg("Method", methods)
    msg("Non-method term", nonMethods)
    
    pp("\nPrinting " + what + " after each phase:\n")
    
    members foreach { m => 
      printAfterEachPhase(m.defString)
      println("")
    }
  }

  // Eliminate any symbols in a list of classes which are ancestors of another in the list
  def elimSupers(syms: List[Symbol]): List[Symbol] = syms match {
    case Nil                                   => Nil
    case x :: xs if xs exists (_ isSubClass x) => elimSupers(xs)
    case x :: xs                               => x :: elimSupers(xs filterNot (x isSubClass _))
  }
  
  // A String representing a symbol's name and type
  def paramString(sym: Symbol) = "" + sym.initialize.name + ": " + sym.tpe.finalResultType.widen

  // A string representing the constructor arguments of one of the trees
  def constructorString(clazz: Symbol) = (
    if (clazz.isModule || clazz.isTrait) ""     // no parameter list for objects/traits
    else if (clazz.isCase) clazz.caseFieldAccessors map paramString mkString ("(", ", ", ")")
    else clazz.primaryConstructor.paramss.flatten map paramString mkString ("(", ", ", ")")
  )
}
