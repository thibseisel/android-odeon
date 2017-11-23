/*
 * Copyright 2017 Thibault Seisel
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

package fr.nihilus.music.service

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.res.XmlResourceParser
import android.os.Process
import android.util.Base64
import android.util.Log
import fr.nihilus.music.R
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException


private const val TAG = "PackageValidator"

class PackageValidator(context: Context) {

    private val validCertificates = readValidCertificates(
            context.resources.getXml(R.xml.allowed_media_browser_callers))

    private fun readValidCertificates(parser: XmlResourceParser): Map<String, ArrayList<CallerInfo>> {
        val validCertificates = HashMap<String, ArrayList<CallerInfo>>()
        try {
            var eventType = parser.next()
            while (eventType != XmlResourceParser.END_DOCUMENT) {
                if (eventType == XmlResourceParser.START_TAG && parser.name == "signing_certificate") {

                    val name = parser.getAttributeValue(null, "name")
                    val packageName = parser.getAttributeValue(null, "package")
                    val isRelease = parser.getAttributeBooleanValue(null, "release", false)
                    val certificate = parser.nextText().replace("""\s|\n""".toRegex(), "")

                    val info = CallerInfo(name, packageName, isRelease)

                    var infos = validCertificates.get(certificate)
                    if (infos == null) {
                        infos = ArrayList()
                        validCertificates.put(certificate, infos)
                    }
                    Log.v(TAG, """
                        Adding allowed caller: ${info.name},
                        package=${info.packageName}, release=${info.release},
                        certificate=$certificate
                        """.trimIndent())
                    infos.add(info)
                }
                eventType = parser.next()
            }
        } catch (e: XmlPullParserException) {
            Log.e(TAG, "Could not read allowed callers from XML.", e)
        } catch (e: IOException) {
            Log.e(TAG, "Could not read allowed callers from XML.", e)
        }

        return validCertificates
    }

    fun isCallerAllowed(context: Context, callingPackage: String, callingUid: Int): Boolean {
        // Always allow calls from the framework, self app or development environment.
        if (Process.SYSTEM_UID == callingUid || Process.myUid() == callingUid) {
            return true
        }

        if (isPlatformSigned(context, callingPackage)) {
            return true
        }

        val packageInfo = getPackageInfo(context, callingPackage) ?: return false
        if (packageInfo.signatures.size != 1) {
            Log.w(TAG, "Caller does not have exactly one signature certificate!")
            return false
        }
        val signature = Base64.encodeToString(
                packageInfo.signatures[0].toByteArray(), Base64.NO_WRAP)

        // Test for known signatures:
        val validCallers = validCertificates.get(signature)
        if (validCallers == null) {
            Log.v(TAG, "Signature for caller $callingPackage is not valid: \n$signature")
            if (validCertificates.isEmpty()) {
                Log.w(TAG, """
                    The list of valid certificates is empty.
                    Either your file "res/xml/allowed_media_browser_callers.xml is empty
                    or there was an error while reading it. Check previous log messages.
                    """.trimIndent())
            }
            return false
        }

        // Check if the package name is valid for the certificate:
        val expectedPackages = StringBuffer()
        for (info in validCallers) {
            if (callingPackage == info.packageName) {
                Log.v(TAG, "Valid caller: ${info.name}, package=${info.packageName}, release=${info.release}")
                return true
            }
            expectedPackages.append(info.packageName).append(' ')
        }

        Log.i(TAG, """
            Caller has a valid certificate, but its package doesn't match any expected package for the given certificate.
            Caller's package is $callingPackage.
            Expected packages as defined in res/xml/allowed_media_browser_callers.xml are ($expectedPackages).
            This caller's certificate is: \n"$signature
            """.trimIndent())

        return false
    }

    /**
     * @return [PackageInfo] for the package name or null if it's not found.
     */
    private fun getPackageInfo(context: Context, pkgName: String): PackageInfo? {
        val pm = context.packageManager
        return try {
            pm.getPackageInfo(pkgName, PackageManager.GET_SIGNATURES)
        } catch (e: PackageManager.NameNotFoundException) {
            Log.w(TAG, "Package manager can't find package: $pkgName", e)
            return null
        }
    }


    /**
     * @return true if the installed package signature matches the platform signature.
     */
    private fun isPlatformSigned(context: Context, pkgName: String): Boolean {
        val platformPackageInfo = getPackageInfo(context, "android")

        // Should never happen.
        if (platformPackageInfo?.signatures == null || platformPackageInfo.signatures.isEmpty()) {
            return false
        }

        val clientPackageInfo = getPackageInfo(context, pkgName)

        return (clientPackageInfo?.signatures != null &&
                clientPackageInfo.signatures.isNotEmpty() &&
                platformPackageInfo.signatures[0] == clientPackageInfo.signatures[0])
    }

    private class CallerInfo(val name: String, val packageName: String, val release: Boolean)
}