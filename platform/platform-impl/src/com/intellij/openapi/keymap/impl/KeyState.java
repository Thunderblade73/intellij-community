/*
 * Copyright 2000-2010 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.openapi.keymap.impl;

import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public enum KeyState {
  STATE_INIT,
  STATE_WAIT_FOR_SECOND_KEYSTROKE,
  STATE_SECOND_STROKE_IN_PROGRESS,
  STATE_PROCESSED,
  STATE_KEY_GESTURE_PROCESSOR,
  STATE_WAIT_FOR_POSSIBLE_ALT_GR

}
