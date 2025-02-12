// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.maddyhome.idea.copyright.actions

import com.intellij.copyright.CopyrightBundle
import com.intellij.ui.dsl.builder.panel
import javax.swing.JCheckBox

internal class UpdateCopyrightAdditionalUi {
  lateinit var updateExistingCopyrightsCb: JCheckBox
  val panel = panel {
    row {
      updateExistingCopyrightsCb = checkBox(CopyrightBundle.message("checkbox.text.update.existing.copyrights")).component
    }
  }
}