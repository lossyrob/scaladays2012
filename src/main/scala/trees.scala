// // Eliminating noisy Tree.this. qualifiers
// def clean(x: Any) = ("" + x).replaceAllLiterally("Trees.this.", "")
// def pp(x: Any) = println(clean(x))
// def pps(xs: List[Any]) = xs foreach pp
// 
// // Eliminate any symbols in a list of classes which are ancestors of another in the list
// def elimSupers(syms: List[Symbol]): List[Symbol] = syms match {
//   case Nil                                   => Nil
//   case x :: xs if xs exists (_ isSubClass x) => elimSupers(xs)
//   case x :: xs                               => x :: elimSupers(xs filterNot (x isSubClass _))
// }
// 
// // Symbol for class "Global"
// val g = intp.types[scala.tools.nsc.Global]
// 
// // Symbol for class "Tree", a type member of Global
// val treeSymbol = typeMember(g, "Tree")
// 
// // All members of Global which descend from Tree
// def treeDescendants = g.info.members filter (m => (m isSubClass treeSymbol) || (m.moduleClass isSubClass treeSymbol))
// 
// // All case classes/objects among treeDescendants
// def caseTrees = treeDescendants filter (_.isCase)
// 
// // All traits among treeDescendants
// def traitTrees = treeDescendants filter (_.isTrait)
// 
// // Eliminate any symbols in a list of classes which are ancestors of another in the list
// def elimSupers(syms: List[Symbol]): List[Symbol] = syms match {
//   case Nil                                   => Nil
//   case x :: xs if xs exists (_ isSubClass x) => elimSupers(xs)
//   case x :: xs                               => x :: elimSupers(xs filterNot (x isSubClass _))
// }
// 
// // A String representing a symbol's name and type
// def paramString(sym: Symbol) = "" + sym.initialize.name + ": " + sym.tpe.finalResultType.widen
// 
// // A string representing the constructor arguments of one of the trees
// def constructorString(clazz: Symbol) = (
//   if (clazz.isModule) ""     // no empty parameter list for objects
//   else clazz.caseFieldAccessors map paramString mkString ("(", ", ", ")")
// )
// 
// // The closest ancestor of clazz which descends from Tree
// def parentTree(clazz: Symbol) = elimSupers(clazz.parentSymbols filter (_ isSubClass treeSymbol)).head
// 
// // All the descendents of Tree, grouped by their first tree-derived parent, then sorted by fewest ancestors
// def groupedTrees = (treeClasses groupBy parentTree).toList sortBy (_._1.ancestors.size)
// 
// // The as-complete-as-we're-going-for signature of one of our case class/objects
// def signature(clazz: Symbol) = (
//   "case %6s %s%s extends %s".format(
//     clazz.kindString, 
//     clazz.name, 
//     constructorString(clazz),
//     elimSupers(clazz.parentSymbols filter (_ isSubClass treeSymbol)) map (_.name) mkString ", "
//   )
// )
// 
// // A brief look at tree traits with new declarations
// def showTreeTraitDeclarations() = {
//   for (parent <- treeTraits) {
//     val members = parent.info.decls.toList.filter(s => s.initialize.isPublic && !s.isConstructor && !s.isSetter)
//     if (members.nonEmpty)
//       pp(members.map(m => "  def " + paramString(m)).mkString("\n" + parent + " {\n", "\n", "}"))
//   }
// }
// 
// // The members of "Team AST."
// def showSausage() = {
//   for ((parent, children) <- groupedTrees) {
//     pp("\n" + parent.fullLocationString + " has " + children.size + " direct children:")
//     children.map(t => "  " + signature(t)).sorted foreach pp
//   }  
// }
// 
// // Members of a given package which satisfy the predicate
// def pkgMembersWhich(pkg: String)(p: Symbol => Boolean) = (
//   // Playing defense against the compiler's ill-preparedness for this sort of assault
//   // As a general rule please don't swallow all the exceptions which come your way
//   getRequiredModule(pkg).info.members filter (m => try p(m) catch { case t => false })
// )
// 
// def specializedClassesIn(pkg: String) = (
//   pkgMembersWhich(pkg)(_.typeParams exists (_.initialize hasAnnotation SpecializedClass)) map (_.defString)
// )
// 
// showTreeTraitDeclarations()
// showSausage()
// pps(specializedClassesIn("scala.runtime"))
// pps(specializedClassesIn("scala"))
