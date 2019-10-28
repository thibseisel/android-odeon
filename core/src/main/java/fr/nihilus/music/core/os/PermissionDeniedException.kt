/*
 * Copyright 2019 Thibault Seisel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.nihilus.music.core.os

import android.Manifest

/**
 * Thrown when an operation has failed due to a Android permission not being granted.
 * This may be caught by UI components to request the missing permission.
 *
 * @property permission The name of the permission that is denied.
 * This is a constant from [Manifest.permission].
 */
class PermissionDeniedException(val permission: String) : RuntimeException(
    "An operation has failed because it requires the following permission: $permission"
)