// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.kotlin.idea.decompiler.navigation

import com.intellij.psi.*
import com.intellij.psi.impl.source.resolve.reference.impl.PsiMultiReference
import com.intellij.util.ThrowableRunnable
import org.jetbrains.kotlin.idea.base.projectStructure.RootKindFilter
import org.jetbrains.kotlin.idea.base.projectStructure.matches
import org.jetbrains.kotlin.idea.navigation.NavigationTestUtils
import org.jetbrains.kotlin.idea.references.KtReference
import org.jetbrains.kotlin.idea.test.*
import java.io.File

abstract class AbstractNavigateToLibraryTest : KotlinLightCodeInsightFixtureTestCase() {
    abstract val expectedFileExt: String

    protected fun doTest(path: String) {
        myFixture.configureByFile(fileName())
        val pathOfExpect = path.replace(Regex("\\.kt|\\.java"), expectedFileExt)
        NavigationChecker.checkAnnotatedCode(file, File(pathOfExpect))
    }

    override fun tearDown() = runAll(
        ThrowableRunnable { SourceNavigationHelper.resetForceResolve() },
        ThrowableRunnable { super.tearDown() }
    )
}

abstract class AbstractNavigateToDecompiledLibraryTest : AbstractNavigateToLibraryTest() {
    override val expectedFileExt: String get() = ".decompiled.expected"

    private val mockLibraryFacility = MockLibraryFacility(
        source = IDEA_TEST_DATA_DIR.resolve("decompiler/navigation/library"),
        attachSources = false,
    )

    override fun setUp() {
        super.setUp()
        mockLibraryFacility.setUp(module)
    }

    override fun tearDown() = runAll(
        ThrowableRunnable { mockLibraryFacility.tearDown(module) },
        ThrowableRunnable { super.tearDown() }
    )
}

abstract class AbstractNavigateToLibrarySourceTest : AbstractNavigateToLibraryTest() {
    override val expectedFileExt: String = ".source.expected"

    private val mockLibraryFacility = MockLibraryFacility(
        source = IDEA_TEST_DATA_DIR.resolve("decompiler/navigation/library")
    )

    override fun setUp() {
        super.setUp()
        mockLibraryFacility.setUp(module)
    }

    override fun tearDown() = runAll(
        ThrowableRunnable { mockLibraryFacility.tearDown(module) },
        ThrowableRunnable { super.tearDown() }
    )
}

abstract class AbstractNavigateJavaToLibraryTest(testDataPath: String, attachSources: Boolean) : AbstractNavigateToLibraryTest() {
    protected val mockLibraryFacility = MockLibraryFacility(
        source = IDEA_TEST_DATA_DIR.resolve(testDataPath),
        attachSources = attachSources,
        options = listOf("-Xcontext-receivers")
    )

    override val expectedFileExt: String = ".source.expected"

    override fun setUp() {
        super.setUp()
        mockLibraryFacility.setUp(module)
    }

    override fun tearDown() = runAll(
        ThrowableRunnable { mockLibraryFacility.tearDown(module) },
        ThrowableRunnable { super.tearDown() }
    )
}

abstract class AbstractNavigateJavaSourceToLibraryTest : AbstractNavigateJavaToLibraryTest("decompiler/navigation/fromJavaSource", false)

abstract class AbstractNavigateJavaSourceToLibrarySourceTest : AbstractNavigateJavaToLibraryTest("navigation/javaSource/librarySource", true)

abstract class AbstractNavigateToLibrarySourceTestWithJS : AbstractNavigateToLibrarySourceTest() {
    private val mockLibraryFacility = MockLibraryFacility(IDEA_TEST_DATA_DIR.resolve("decompiler/navigation/fromJavaSource"),
                                                          options = listOf("-Xcontext-receivers"))

    override fun tearDown() = runAll(
        ThrowableRunnable { mockLibraryFacility.tearDown(module) },
        ThrowableRunnable { super.tearDown() }
    )

    override fun getProjectDescriptor(): KotlinLightProjectDescriptor = KotlinMultiModuleProjectDescriptor(
        "AbstractNavigateToLibrarySourceTestWithJS",
        mockLibraryFacility.asKotlinLightProjectDescriptor,
        KotlinStdJSProjectDescriptor
    )
}

class NavigationChecker(val file: PsiFile, val referenceTargetChecker: (PsiElement) -> Unit) {
    fun annotatedLibraryCode(): String {
        val navigableElements = collectInterestingNavigationElements()
        return NavigationTestUtils.getNavigateElementsText(file.project, navigableElements)
    }

    private fun collectInterestingNavigationElements(): List<PsiElement?> {
        val refs = collectInterestingReferences()
        return refs.map {
            val target = requireNotNull(it.resolve())
            target.navigationElement
        }
    }

    private fun collectInterestingReferences(): Collection<PsiReference> {
        val referenceContainersToReferences = LinkedHashMap<PsiElement, PsiReference>()
        val allRefs = (0 until file.textLength).flatMap { offset ->
          when (val ref = file.findReferenceAt(offset)) {
              is KtReference, is PsiReferenceExpression, is PsiJavaCodeReferenceElement -> listOf(ref)
              is PsiMultiReference -> ref.references.filterIsInstance<KtReference>()
              else -> emptyList()
          }
        }.distinct()

        for (reference in allRefs) {
            referenceContainersToReferences.addReference(reference)
        }
        return referenceContainersToReferences.values
    }

    private fun MutableMap<PsiElement, PsiReference>.addReference(ref: PsiReference) {
        if (containsKey(ref.element)) return
        val target = ref.resolve() ?: return

        referenceTargetChecker(target)

        val targetNavPsiFile = target.navigationElement.containingFile ?: return

        val targetNavFile = targetNavPsiFile.virtualFile ?: return

        if (!RootKindFilter.projectSources.matches(target.project, targetNavFile)) {
            put(ref.element, ref)
        }
    }

    companion object {
        fun checkAnnotatedCode(file: PsiFile, expectedFile: File, referenceTargetChecker: (PsiElement) -> Unit = {}) {
            val navigationChecker = NavigationChecker(file, referenceTargetChecker)
            try {
                for (forceResolve in listOf(false, true)) {
                    SourceNavigationHelper.setForceResolve(forceResolve)
                    KotlinTestUtils.assertEqualsToFile(expectedFile, navigationChecker.annotatedLibraryCode())
                }
            } finally {
                SourceNavigationHelper.resetForceResolve()
            }
        }
    }
}
