package fr.xgouchet.packageexplorer.core.utils

import android.Manifest
import android.Manifest.*
import android.app.Activity
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Environment
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import fr.xgouchet.axml.CompressedXmlParser
import fr.xgouchet.packageexplorer.core.utils.getMainActivities
import io.reactivex.rxjava3.core.Observable
import org.w3c.dom.Document
import timber.log.Timber
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream
import java.security.AccessController
import java.util.Enumeration
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerException
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import kotlin.reflect.jvm.internal.impl.load.java.lazy.ContextKt
import kotlin.system.measureNanoTime

const val MANIFEST_FILE_NAME = "AndroidManifest.xml"

fun exportManifestFromPackage(
    info: PackageInfo,
    context: Context
): Observable<File> {
    return Observable.fromCallable {
        val name = exportedManifestName(info.packageName)
        val apk = getPackageApk(info)

        return@fromCallable exportManifestFromApkFile(name, apk, context)
    }
}

fun exportManifestDomFromPackage(
    info: PackageInfo
): Document {
    Log.i("manifest_search", "manif1: " + info.packageName);

    return parseManifestFile(getPackageApk(info))
}

// TODO replace with
fun exportManifestFromApk(
    apk: File,
    context: Context
): Observable<File> {
    return Observable.fromCallable {
        val name = "${apk.nameWithoutExtension}_AndroidManifest.xml"
        Log.i("manifest_search", "manif2: " + name)
        return@fromCallable exportManifestFromApkFile(name, apk, context)
    }
}

private fun exportManifestFromApkFile(name: String, apk: File, context: Context): File {
    val doc = parseManifestFile(apk)
    val destFile = File(context.cacheDir, name)
    val destFile2 = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "manifests/" + name)
    File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "manifests/").mkdirs()
    Log.i("manifest_search", "manifList: " + destFile.path)
    writeXml(doc, destFile, context)
    Log.i("manifest_search", "manif3: " + destFile)
    Log.e("manifest_search", "manif33: " + destFile2)
    writeXml(doc, destFile2, context)
    return destFile
}

private fun writeXml(doc: Document, output: File, activity: Context) {
    val transformerFactory = TransformerFactory.newInstance()
    val transformer = transformerFactory.newTransformer()
    //val activity2 = Activity
    transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8")
    transformer.setOutputProperty(OutputKeys.INDENT, "yes")
    transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")
    val source = DOMSource(doc)
    val result = StreamResult(output)
    Log.i("manifest_search", "manifwrite3: " + output)
    val permission =
        ActivityCompat.checkSelfPermission(activity, permission.WRITE_EXTERNAL_STORAGE)

    if (permission != PackageManager.PERMISSION_GRANTED) {
        // We don't have permission so prompt the user
//        ActivityCompat.requestPermissions(
//            activity2,
//            arrayOf(
//                Manifest.permission.READ_EXTERNAL_STORAGE,
//                Manifest.permission.WRITE_EXTERNAL_STORAGE
//            ),
//            1
//        )
        Log.e("manifest_search", "WHAAAAT");
    } else
    {
        Log.e("manifest_search", "Yes perms");
    }
        Log.e("manifest_search", "checkpoint for writing");
    transformer.transform(source, result)
//    try{
//
//    } catch (e: TransformerException) {
//        e.printStackTrace()
//        Log.e("manifest_search", "error: " + e.printStackTrace());
////        if (permission != PackageManager.PERMISSION_GRANTED) {
////            // We don't have permission so prompt the user
////            ActivityCompat.requestPermissions(
////                activity,
////                Manifest.permission.READ_EXTERNAL_STORAGE,
////                Manifest.permission.WRITE_EXTERNAL_STORAGE
////            );
////        }
//    } finally{
//        Log.e("manifest_search", "finally");
//    }

}

private fun exportedManifestName(packageName: String): String {
    val cleaned = packageName.replace("\\.".toRegex(), "_")
    return "${cleaned}_AndroidManifest.xml"
}

private fun getPackageApk(info: PackageInfo): File {
    val srcPackage = info.applicationInfo.publicSourceDir
    return File(srcPackage)
}

private fun parseManifestFile(apkFile: File): Document {
    var doc: Document? = null

    val duration = measureNanoTime {
        var inputStream: InputStream? = null
        var zipFile: ZipFile? = null
        try {
            zipFile = ZipFile(apkFile)
            val entries: Enumeration<out ZipEntry> = zipFile.entries()

            val parser = CompressedXmlParser()

            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                if (entry.name == MANIFEST_FILE_NAME) {

                    inputStream = zipFile.getInputStream(entry)
                    doc = parser.parseDOM(inputStream)
                    break
                }
            }
        } finally {
            inputStream?.close()
            zipFile?.close()
        }
    }
    Log.i("manifest_search", "Parsed AndroidManifest from $apkFile in $duration ns")
    return doc ?: throw FileNotFoundException("Couldn't find manifest in apk")
}
