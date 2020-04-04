/*
 * Copyright 2020 Thibault Seisel
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

package fr.nihilus.music.settings

import android.app.ActivityManager
import android.content.Context
import android.os.Bundle
import fr.nihilus.music.R
import fr.nihilus.music.core.ui.base.BaseActivity
import kotlinx.android.synthetic.main.activity_manage_space.*

/**
 * Activity triggered by the system when clicking on the "Clear Data" button
 * on this app detail screen.
 */
internal class ManageSpaceActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_space)

        btn_clear_all.setOnClickListener {
            val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            manager.clearApplicationUserData()
        }
    }
}