package com.jtransc.gen.js

import com.jtransc.ConfigOutputFile
import com.jtransc.ConfigTargetDirectory
import com.jtransc.annotation.JTranscAddMembersList
import com.jtransc.annotation.JTranscCustomMainList
import com.jtransc.annotation.JTranscMethodBodyList
import com.jtransc.ast.*
import com.jtransc.ast.feature.method.SwitchFeature
import com.jtransc.ds.Allocator
import com.jtransc.ds.getOrPut2
import com.jtransc.error.invalidOp
import com.jtransc.error.unexpected
import com.jtransc.gen.GenTargetDescriptor
import com.jtransc.gen.GenTargetProcessor
import com.jtransc.gen.common.*
import com.jtransc.injector.Injector
import com.jtransc.injector.Singleton
import com.jtransc.io.ProcessResult2
import com.jtransc.log.log
import com.jtransc.sourcemaps.Sourcemaps
import com.jtransc.target.Js
import com.jtransc.text.Indenter
import com.jtransc.text.isLetterDigitOrUnderscore
import com.jtransc.text.quote
import com.jtransc.vfs.ExecOptions
import com.jtransc.vfs.LocalVfs
import com.jtransc.vfs.LocalVfsEnsureDirs
import com.jtransc.vfs.SyncVfsFile
import java.io.File
import java.util.*

class JsTarget() : GenTargetDescriptor() {
	override val priority = 500
	override val name = "js"
	override val longName = "Javascript"
	override val sourceExtension = "js"
	override val outputExtension = "js"
	override val extraLibraries = listOf<String>()
	override val extraClasses = listOf<String>()
	override val runningAvailable: Boolean = true
	override fun getProcessor(injector: Injector): GenTargetProcessor {
		val settings = injector.get<AstBuildSettings>()
		val configTargetDirectory = injector.get<ConfigTargetDirectory>()
		val configOutputFile = injector.get<ConfigOutputFile>()
		val targetFolder = LocalVfsEnsureDirs(File("${configTargetDirectory.targetDirectory}/jtransc-js"))
		injector.mapInstance(ConfigFeatureSet(JsFeatures))
		injector.mapImpl<CommonNames, JsNames>()
		injector.mapInstance(CommonGenFolders(settings.assets.map { LocalVfs(it) }))
		injector.mapInstance(ConfigTargetFolder(targetFolder))
		injector.mapInstance(ConfigSrcFolder(targetFolder))
		injector.mapInstance(ConfigOutputFile2(targetFolder[configOutputFile.outputFileBaseName].realfile))
		injector.mapImpl<CommonProgramTemplate, CommonProgramTemplate>()
		return injector.get<JsGenTargetProcessor>()
	}

	override fun getTargetByExtension(ext: String): String? = when (ext) {
		"js" -> "js"
		else -> null
	}
}

data class ConfigJavascriptOutput(val javascriptOutput: SyncVfsFile)

val JsFeatures = setOf(SwitchFeature::class.java)

fun hasSpecialChars(name: String): Boolean {
	return !name.all { it.isLetterDigitOrUnderscore() }
}

fun accessStr(name: String): String {
	return if (hasSpecialChars(name)) "[${name.quote()}]" else ".$name"
}

@Singleton
class JsGenTargetProcessor(
	val injector: Injector,
	val configOutputFile2: ConfigOutputFile2,
	val configTargetFolder: ConfigTargetFolder,
	val program: AstProgram,
	val templateString: CommonProgramTemplate,
	val gen: GenJsGen
) : CommonGenTargetProcessor(gen) {
	//val outputFile2 = File(File(tinfo.outputFile).absolutePath)

	override fun buildSource() {
		//gen._write(configTargetFolder.targetFolder)
		gen._write(configTargetFolder.targetFolder)
		templateString.setInfoAfterBuildingSource()
	}

	override fun compileAndRun(redirect: Boolean): ProcessResult2 = _compileRun(run = true, redirect = redirect)
	override fun compile(): ProcessResult2 = _compileRun(run = false, redirect = false)

	fun _compileRun(run: Boolean, redirect: Boolean): ProcessResult2 {
		val outputFile = injector.get<ConfigJavascriptOutput>().javascriptOutput

		log.info("Generated javascript at..." + outputFile.realpathOS)

		if (run) {
			val result = CommonGenCliCommands.runProgramCmd(
				program,
				target = "js",
				default = listOf("node", "{{ outputFile }}"),
				template = templateString,
				options = ExecOptions(passthru = redirect)
			)
			return ProcessResult2(result)
		} else {
			return ProcessResult2(0)
		}
	}

	override fun run(redirect: Boolean): ProcessResult2 = ProcessResult2(0)
}

@Singleton
class JsNames(program: AstResolver, configMinimizeNames: ConfigMinimizeNames) : CommonNames(program, keywords = setOf("name", "constructor", "prototype", "__proto__")) {
	val minimize: Boolean = configMinimizeNames.minimizeNames

	override val stringPoolType: StringPoolType = StringPoolType.GLOBAL

	override fun buildTemplateClass(clazz: FqName): String = getClassFqNameForCalling(clazz)
	override fun buildTemplateClass(clazz: AstClass): String = getClassFqNameForCalling(clazz.name)
	override fun buildMethod(method: AstMethod, static: Boolean): String {
		val clazz = getClassFqNameForCalling(method.containingClass.name)
		val name = getJsMethodName(method)
		return if (static) "$clazz[${name.quote()}]" else name
	}

	override fun buildStaticInit(clazz: AstClass): String = getClassStaticInit(clazz.ref, "template sinit")

	override fun buildConstructor(method: AstMethod): String {
		val clazz = getClassFqNameForCalling(method.containingClass.name)
		val methodName = getJsMethodName(method)
		return "new $clazz()[${methodName.quote()}]"
	}

	private val fieldNames = hashMapOf<Any?, String>()
	private val cachedFieldNames = hashMapOf<AstFieldRef, String>()

	fun getNativeName(field: AstField): String {
		//"_" + field.uniqueName

		val fieldRef = field.ref
		val keyToUse = field.ref

		return fieldNames.getOrPut2(keyToUse) {
			if (fieldRef !in cachedFieldNames) {
				val fieldName = field.name.replace('$', '_')
				//var name = if (fieldName in JsKeywordsWithToStringAndHashCode) "${fieldName}_" else fieldName
				var name = "_$fieldName"

				val clazz = program[fieldRef]?.containingClass
				val clazzAncestors = clazz?.ancestors?.reversed() ?: listOf()
				val names = clazzAncestors.flatMap { it.fields }.filter { it.name == field.name }.map { getNativeName(it.ref) }.toHashSet()
				val fieldsColliding = clazz?.fields?.filter { it.name == field.name }?.map { it.ref } ?: listOf(field.ref)

				// JTranscBugInnerMethodsWithSameName.kt
				for (f2 in fieldsColliding) {
					while (name in names) name += "_"
					cachedFieldNames[f2] = name
					names += name
				}
				cachedFieldNames[field.ref] ?: unexpected("Unexpected. Not cached: $field")
			}
			cachedFieldNames[field.ref] ?: unexpected("Unexpected. Not cached: $field")
		}
	}

	override fun getNativeName(field: FieldRef): String = getNativeName(program[field.ref]!!)
	override fun getNativeName(methodRef: MethodRef): String = getJsMethodName(methodRef.ref)
	override fun getNativeName(local: LocalParamRef): String = super.getNativeName(local)
	override fun getNativeName(clazz: FqName): String = getClassFqNameForCalling(clazz)
	override fun buildAccessName(name: String, static: Boolean): String = accessStr(name)

	fun getJsMethodName(method: MethodRef): String = getJsMethodName(method.ref)

	fun getJsMethodName(methodRef: AstMethodRef): String {
		if (program is AstProgram) {
			val method = methodRef.resolve(program)
			if (method.nativeName != null) {
				return method.nativeName!!
			}
		}
		return if (methodRef.isInstanceInit) {
			"${methodRef.classRef.fqname}${methodRef.name}${methodRef.desc}"
		} else {
			"${methodRef.name}${methodRef.desc}"
		}
	}

	override fun getClassStaticInit(classRef: AstType.REF, reason: String): String = getClassFqNameForCalling(classRef.name) + ".SI();"
	override fun getClassFqName(name: FqName): String = name.fqname
	override fun getClassFqNameForCalling(fqName: FqName): String = fqName.fqname.replace('.', '_')
	fun getJsClassFqNameInt(fqName: FqName): String = fqName.simpleName
	override fun getFilePath(name: FqName): String = name.simpleName
	fun getJsGeneratedFqPackage(fqName: FqName): String = fqName.fqname
	override fun getGeneratedFqName(name: FqName): FqName = name
	override fun getGeneratedSimpleClassName(name: FqName): String = name.fqname
	override fun getTargetMethodAccess(refMethod: AstMethod, static: Boolean): String = accessStr(getNativeName(refMethod))
}

@Singleton
class GenJsGen(injector: Injector) : GenCommonGenSingleFile(injector) {
	// @TODO: Kotlin IDE: Refactoring imports doesn't take into account: JTranscAddFileList::value so this is a workaround for this
	val _JTranscCustomMainList = JTranscCustomMainList::class.java
	val _JTranscMethodBodyList = JTranscMethodBodyList::class.java
	val _JTranscAddMembersList = JTranscAddMembersList::class.java

	@Suppress("UNCHECKED_CAST")
	internal fun _write(output: SyncVfsFile) {
		val resourcesVfs = program.resourcesVfs
		val copyFiles = getFilesToCopy("js")

		data class ConcatFile(val prepend: String?, val append: String?)
		class CopyFile(val content: ByteArray, val dst: String, val isAsset: Boolean)

		val copyFilesTrans = copyFiles.filter { it.src.isNotEmpty() && it.dst.isNotEmpty() }.map {
			val file = resourcesVfs[it.src]
			if (it.process) {
				CopyFile(file.readString().template("copyfile").toByteArray(), it.dst, it.isAsset)
			} else {
				CopyFile(file.read(), it.dst, it.isAsset)
			}
		}

		// Copy assets
		folders.copyAssetsTo(output)
		for (file in copyFilesTrans) {
			output[file.dst].ensureParentDir().write(file.content)
		}

		templateString.params["assetFiles"] = (templateString.params["assetFiles"] as List<SyncVfsFile>) + copyFilesTrans.map { output[it.dst] }

		val concatFilesTrans = copyFiles.filter { it.append.isNotEmpty() || it.prepend.isNotEmpty() || it.prependAppend.isNotEmpty() }.map {
			val prependAppend = if (it.prependAppend.isNotEmpty()) (resourcesVfs[it.prependAppend].readString() + "\n") else null
			val prependAppendParts = prependAppend?.split("/* ## BODY ## */")

			val prepend = if (prependAppendParts != null && prependAppendParts.size >= 2) prependAppendParts[0] else if (it.prepend.isNotEmpty()) (resourcesVfs[it.prepend].readString() + "\n") else null
			val append = if (prependAppendParts != null && prependAppendParts.size >= 2) prependAppendParts[1] else if (it.append.isNotEmpty()) (resourcesVfs[it.append].readString() + "\n") else null

			fun process(str: String?): String? = if (it.process) str?.template("includeFile") else str

			ConcatFile(process(prepend), process(append))
		}

		val classesIndenter = arrayListOf<Indenter>()

		val indenterPerClass = hashMapOf<AstClass, Indenter>()

		val sortedClasses = program.classes.filter { !it.isNative }.sortedByExtending()

		for (clazz in sortedClasses) {
			val indenter = if (clazz.implCode != null) {
				Indenter.gen { line(clazz.implCode!!) }
			} else {
				//if (!clazz.isInterface) {
					writeClass(clazz)
				//} else {
				//	Indenter.EMPTY
				//}
			}
			indenterPerClass[clazz] = indenter
			classesIndenter.add(indenter)
		}

		val SHOW_SIZE_REPORT = true
		if (SHOW_SIZE_REPORT) {
			for ((clazz, text) in indenterPerClass.toList().map { it.first to it.second.toString() }.sortedBy { it.second.length }) {
				log.info("CLASS SIZE: ${clazz.fqname} : ${text.length}")
			}
		}

		val mainClassFq = program.entrypoint
		val mainClass = mainClassFq.targetClassFqName
		//val mainMethod = program[mainClassFq].getMethod("main", AstType.build { METHOD(VOID, ARRAY(STRING)) }.desc)!!.jsName
		val mainMethod = "main"
		val entryPointClass = FqName(mainClassFq.fqname + "_EntryPoint")
		val entryPointFilePath = entryPointClass.targetFilePath
		val entryPointFqName = entryPointClass.targetGeneratedFqName
		val entryPointSimpleName = entryPointClass.targetGeneratedSimpleClassName
		val entryPointPackage = entryPointFqName.packagePath

		val customMain = program.allAnnotationsList.getTypedList(JTranscCustomMainList::value).firstOrNull { it.target == "js" }?.value

		log("Using ... " + if (customMain != null) "customMain" else "plainMain")

		templateString.setExtraData(mapOf(
			"entryPointPackage" to entryPointPackage,
			"entryPointSimpleName" to entryPointSimpleName,
			"mainClass" to mainClass,
			"mainClass2" to mainClassFq.fqname,
			"mainMethod" to mainMethod
		))

		val strs = Indenter.gen {
			val strs = names.getGlobalStrings()
			val maxId = strs.maxBy { it.id }?.id ?: 0
			line("SS = new Array($maxId);")
			for (e in strs) line("SS[${e.id}] = ${e.str.quote()};")
		}

		val out = Indenter.gen {
			if (settings.debug) line("//# sourceMappingURL=program.js.map")
			for (f in concatFilesTrans) if (f.prepend != null) line(f.prepend)
			line(strs.toString())
			for (indent in classesIndenter) line(indent)
			val mainClassClass = program[mainClassFq]

			line("__createJavaArrays();")
			line("__buildStrings();")
			line("N.linit();")
			line(names.buildStaticInit(mainClassClass))
			val mainMethod = mainClassClass[AstMethodRef(mainClassFq, "main", AstType.METHOD(AstType.VOID, listOf(ARRAY(AstType.STRING))))]
			val mainCall = names.buildMethod(mainMethod, static = true)
			line("$mainCall(N.strArray(N.args()));")
			//N.args
			//names.
			//mainClassFq
			//line(templateString.gen(customMain ?: plainMain, context, "customMain"))
			for (f in concatFilesTrans.reversed()) if (f.append != null) line(f.append)
		}

		val sources = Allocator<String>()
		val mappings = hashMapOf<Int, Sourcemaps.MappingItem>()

		val source = out.toString { sb, line, data ->
			if (settings.debug && data is AstStm.LINE) {
				//println("MARKER: ${sb.length}, $line, $data, ${clazz.source}")
				mappings[line] = Sourcemaps.MappingItem(
					sourceIndex = sources.allocateOnce(data.file),
					sourceLine = data.line,
					sourceColumn = 0,
					targetColumn = 0
				)
				//clazzName.internalFqname + ".java"
			}
		}

		val sourceMap = if (settings.debug) Sourcemaps.encodeFile(sources.array, mappings) else null
		// Generate source
		//println("outputFileBaseName:$outputFileBaseName")
		output[outputFileBaseName] = source
		if (sourceMap != null) output[outputFileBaseName + ".map"] = sourceMap

		injector.mapInstance(ConfigEntryPointClass(entryPointClass))
		injector.mapInstance(ConfigEntryPointFile(entryPointFilePath))
		injector.mapInstance(ConfigJavascriptOutput(output[outputFile]))
	}

	override fun genStmLine(stm: AstStm.LINE) = indent {
		mark(stm)
	}

	override fun genStmTryCatch(stm: AstStm.TRY_CATCH) = indent {
		line("try") {
			line(stm.trystm.genStm())
		}
		line("catch (J__i__exception__)") {
			line("J__exception__ = J__i__exception__;")
			line(stm.catch.genStm())
		}
	}

	override fun genStmRethrow(stm: AstStm.RETHROW) = indent { line("throw J__i__exception__;") }

	override fun genBodyLocals(locals: List<AstLocal>) = indent {
		if (locals.isNotEmpty()) {
			val vars = locals.map { local -> "${local.nativeName} = ${local.type.nativeDefaultString}" }.joinToString(", ")
			line("var $vars;")
		}
	}

	override fun genBodyLocal(local: AstLocal) = indent { line("var ${local.nativeName} = ${local.type.nativeDefaultString};") }
	override fun genBodyTrapsPrefix() = indent { line("var J__exception__ = null;") }
	override fun genBodyStaticInitPrefix(clazzRef: AstType.REF, reasons: ArrayList<String>) = indent {
		line(names.getClassStaticInit(clazzRef, reasons.joinToString(", ")))
	}

	override fun N_AGET_T(arrayType: AstType.ARRAY, elementType: AstType, array: String, index: String) = "($array.data[$index])"
	override fun N_ASET_T(arrayType: AstType.ARRAY, elementType: AstType, array: String, index: String, value: String) = "$array.data[$index] = $value;"

	override fun N_is(a: String, b: AstType.Reference): String {
		return when (b) {
			is AstType.REF -> {
				val clazz = program[b]!!
				if (clazz.isInterface) {
					"N.isClassId($a, ${clazz.classId})"
				} else {
					"($a instanceof ${b.targetTypeCast})"
				}
			}
			else -> {
				super.N_is(a, b)
			}
		}
	}

	override fun N_is(a: String, b: String) = "N.is($a, $b)"
	override fun N_z2i(str: String) = "N.z2i($str)"
	override fun N_i(str: String) = "(($str)|0)"
	override fun N_i2z(str: String) = "(($str)!=0)"
	override fun N_i2b(str: String) = "(($str)<<24>>24)" // shifts uses 32-bit integers
	override fun N_i2c(str: String) = "(($str)&0xFFFF)"
	override fun N_i2s(str: String) = "(($str)<<16>>16)" // shifts uses 32-bit integers
	override fun N_f2i(str: String) = "(($str)|0)"
	override fun N_i2i(str: String) = N_i(str)
	override fun N_i2j(str: String) = "N.i2j($str)"
	override fun N_i2f(str: String) = "Math.fround(+($str))"
	override fun N_i2d(str: String) = "+($str)"
	override fun N_f2f(str: String) = "Math.fround($str)"
	override fun N_f2d(str: String) = "($str)"
	override fun N_d2f(str: String) = "Math.fround(+($str))"
	override fun N_d2i(str: String) = "(($str)|0)"
	override fun N_d2d(str: String) = "+($str)"
	override fun N_l2i(str: String) = "N.l2i($str)"
	override fun N_l2l(str: String) = "($str)"
	override fun N_l2f(str: String) = "Math.fround(N.l2d($str))"
	override fun N_l2d(str: String) = "N.l2d($str)"
	override fun N_getFunction(str: String) = "N.getFunction($str)"
	override fun N_c(str: String, from: AstType, to: AstType) = "($str)"
	override fun N_lneg(str: String) = "N.lneg($str)"
	override fun N_linv(str: String) = "N.linv($str)"
	override fun N_ineg(str: String) = "-($str)"
	override fun N_iinv(str: String) = "~($str)"
	override fun N_fneg(str: String) = "-($str)"
	override fun N_dneg(str: String) = "-($str)"
	override fun N_znot(str: String) = "!($str)"
	override fun N_imul(l: String, r: String): String = "Math.imul($l, $r)"
	override fun genLiteralString(v: String): String = "S[" + names.allocString(context.clazz.name, v) + "]"

	override fun genExprCallBaseSuper(e2: AstExpr.CALL_SUPER, clazz: AstType.REF, refMethodClass: AstClass, method: AstMethodRef, methodAccess: String, args: List<String>): String {
		val superMethod = refMethodClass[method.withoutClass] ?: invalidOp("Can't find super for method : $method")
		val base = names.getClassFqNameForCalling(superMethod.containingClass.name) + ".prototype"
		val argsString = (listOf(e2.obj.genExpr()) + args).joinToString(", ")
		return "$base$methodAccess.call($argsString)"
	}

	private fun AstMethod.getJsNativeBodies(): Map<String, Indenter> = this.getNativeBodies(target = "js")

	fun writeClass(clazz: AstClass): Indenter {
		setCurrentClass(clazz)

		//val isRootObject = clazz.name.fqname == "java.lang.Object"
		val isAbstract = (clazz.classType == AstClassType.ABSTRACT)
		val simpleClassName = clazz.name.targetGeneratedSimpleClassName
		refs._usedDependencies.clear()

		if (!clazz.extending?.fqname.isNullOrEmpty()) refs.add(AstType.REF(clazz.extending!!))
		for (impl in clazz.implementing) refs.add(AstType.REF(impl))

		val classCodeIndenter = Indenter.gen {
			if (isAbstract) line("// ABSTRACT")

			val classBase = names.getClassFqNameForCalling(clazz.name)
			val memberBaseStatic = classBase
			val memberBaseInstance = "$classBase.prototype"
			fun getMemberBase(isStatic: Boolean) = if (isStatic) memberBaseStatic else memberBaseInstance

			//fun renderFields(fields: List<AstField>) {
			//	for (field in fields) {
			//		val nativeMemberName = if (field.targetName2 == field.name) field.name else field.targetName2
			//		line("${getMemberBase(field.isStatic)}${accessStr(nativeMemberName)} = ${field.escapedConstantValue};")
			//	}
			//}

			//val declarationHead = "func " + names.getClassFqNameForCalling(clazz.name) + " = program.registerType(${clazz.classId}, null, ${simpleClassName.quote()}, ${clazz.modifiers.acc}, ${clazz.extending?.targetClassFqName?.quote()}, $interfaces, null, function() {"
			val parentClassBase = if (clazz.extending != null) names.getClassFqNameForCalling(clazz.extending!!) else "java_lang_Object_base";

			val staticFields = clazz.fields.filter { it.isStatic }
			val instanceFields = clazz.fields.filter { !it.isStatic }
			val allInstanceFields = (listOf(clazz) + clazz.parentClassList).flatMap { it.fields }.filter { !it.isStatic }

			fun lateInitField(a: Any?) = (a is String)

			val allInstanceFieldsThis = allInstanceFields.filter { lateInitField(it) }
			val allInstanceFieldsProto = allInstanceFields.filter { !lateInitField(it) }

			//val allInstanceFieldsThis = allInstanceFields
			//val allInstanceFieldsProto = allInstanceFields
			//val allInstanceFieldsProto = listOf<AstField>()


			line("function $classBase()") {
				for (field in allInstanceFieldsThis) {
					val nativeMemberName = if (field.targetName2 == field.name) field.name else field.targetName2
					line("this${accessStr(nativeMemberName)} = ${field.escapedConstantValue};")
				}
			}

			line("$classBase.prototype = Object.create($parentClassBase.prototype);")
			line("$classBase.prototype.constructor = $classBase;")

			for (field in allInstanceFieldsProto) {
				val nativeMemberName = if (field.targetName2 == field.name) field.name else field.targetName2
				line("$classBase.prototype${accessStr(nativeMemberName)} = ${field.escapedConstantValue};")
			}

			//line("$classBase.SI_INIT = false;")


			if (staticFields.isNotEmpty() || clazz.staticConstructor != null) {
				line("$classBase.SI = function()", after2 = ";") {
					line("$classBase.SI = N.EMPTY_FUNCTION;")
					if (clazz.staticConstructor != null) {
						line("$classBase${names.getTargetMethodAccess(clazz.staticConstructor!!, true)}();")
					}
					for (field in staticFields) {
						val nativeMemberName = if (field.targetName2 == field.name) field.name else field.targetName2
						line("${getMemberBase(field.isStatic)}${accessStr(nativeMemberName)} = ${field.escapedConstantValue};")
					}
					if (clazz.staticConstructor != null) {
						line("$classBase${names.getTargetMethodAccess(clazz.staticConstructor!!, true)}();")
					}
				}
			} else {
				line("$classBase.SI = N.EMPTY_FUNCTION;")
			}

			val relatedTypesIds = clazz.getAllRelatedTypes().map { it.classId }
			line("$classBase.prototype.\$\$CLASS_ID = ${clazz.classId};")
			line("$classBase.prototype.\$\$CLASS_IDS = [${relatedTypesIds.joinToString(",")}];")

			//renderFields(clazz.fields);

			fun writeMethod(method: AstMethod): Indenter {
				setCurrentMethod(method)
				return Indenter.gen {
					refs.add(method.methodType)
					val margs = method.methodType.args.map { it.name }

					//val defaultMethodName = if (method.isInstanceInit) "${method.ref.classRef.fqname}${method.name}${method.desc}" else "${method.name}${method.desc}"
					//val methodName = if (method.targetName == defaultMethodName) null else method.targetName
					val nativeMemberName = names.buildMethod(method, false)
					val prefix = "${getMemberBase(method.isStatic)}${accessStr(nativeMemberName)}"

					val rbody = if (method.body != null) method.body else if (method.bodyRef != null) program[method.bodyRef!!]?.body else null

					fun renderBranch(actualBody: Indenter?) = Indenter.gen {

						if (actualBody != null) {
							line("$prefix = function(${margs.joinToString(", ")})", after2 = ";") {
								line(actualBody)
								if (method.methodVoidReturnThis) line("return this;")
							}
						} else {
							line("$prefix = function() { N.methodWithoutBody('$prefix') };")
						}
					}

					fun renderBranches() = Indenter.gen {
						try {
							val nativeBodies = method.getJsNativeBodies()
							var javaBodyCacheDone: Boolean = false
							var javaBodyCache: Indenter? = null
							fun javaBody(): Indenter? {
								if (!javaBodyCacheDone) {
									javaBodyCacheDone = true
									javaBodyCache = rbody?.genBodyWithFeatures(method)
								}
								return javaBodyCache
							}
							//val javaBody by lazy {  }

							// @TODO: Do not hardcode this!
							if (nativeBodies.isEmpty() && javaBody() == null) {
								line(renderBranch(null))
							} else {
								if (nativeBodies.isNotEmpty()) {
									val default = if ("" in nativeBodies) nativeBodies[""]!! else javaBody() ?: Indenter.EMPTY
									val options = nativeBodies.filter { it.key != "" }.map { it.key to it.value } + listOf("" to default)

									if (options.size == 1) {
										line(renderBranch(default))
									} else {
										for (opt in options.withIndex()) {
											if (opt.index != options.size - 1) {
												val iftype = if (opt.index == 0) "if" else "else if"
												line("$iftype (${opt.value.first})") { line(renderBranch(opt.value.second)) }
											} else {
												line("else") { line(renderBranch(opt.value.second)) }
											}
										}
									}
									//line(nativeBodies ?: javaBody ?: Indenter.EMPTY)
								} else {
									line(renderBranch(javaBody()))
								}
							}
						} catch (e: Throwable) {
							log.printStackTrace(e)
							log.warn("WARNING GenJsGen.writeMethod:" + e.message)

							line("// Errored method: ${clazz.name}.${method.name} :: ${method.desc} :: ${e.message};")
							line(renderBranch(null))
						}
					}

					line(renderBranches())
				}
			}

			for (method in clazz.methods.filter { it.isClassOrInstanceInit }) line(writeMethod(method))
			for (method in clazz.methods.filter { !it.isClassOrInstanceInit }) line(writeMethod(method))
		}

		return classCodeIndenter
	}

	override fun genStmSetArrayLiterals(stm: AstStm.SET_ARRAY_LITERALS) = Indenter.gen {
		line("${stm.array.genExpr()}.setArraySlice(${stm.startIndex}, [${stm.values.map { it.genExpr() }.joinToString(", ")}]);")
	}

	override fun genExprCallBaseStatic(e2: AstExpr.CALL_STATIC, clazz: AstType.REF, refMethodClass: AstClass, method: AstMethodRef, methodAccess: String, args: List<String>): String {
		val className = method.containingClassType.fqname
		val methodName = method.name
		return if (className == Js::class.java.name && methodName.endsWith("_raw")) {
			val arg = e2.args[0].value
			if (arg !is AstExpr.LITERAL || arg.value !is String) invalidOp("Raw call $e2 has not a string literal! but ${args[0]} at $context")
			val base = templateString.gen((arg.value as String))
			when (methodName) {
				"v_raw" -> base
				"o_raw" -> base
				"z_raw" -> "(!!($base))"
				"i_raw" -> "(($base)|0)"
				"d_raw" -> "(+($base))"
				"s_raw" -> "N.str($base)"
				else -> base
			}
		} else {
			super.genExprCallBaseStatic(e2, clazz, refMethodClass, method, methodAccess, args)
		}
	}
}