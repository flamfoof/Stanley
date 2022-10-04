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

fun exportManifestFromPackageXML(
    info: PackageInfo
): Observable<File> {
    return Observable.fromCallable {
        val name = exportedManifestNameXML(info.packageName)
        val apk = getPackageApkXML(info)
        Log.i("manifest_command_export", "manif2: " + name)
        return@fromCallable exportManifestFromApkFileXML(name, apk)
    }
}

fun exportManifestDomFromPackageXML(
    info: PackageInfo
): Document {
    Log.i("manifest_search", "manif1: " + info.packageName);

    return parseManifestFile(getPackageApkXML(info))
}

// TODO replace with
fun exportManifestFromApkXML(
    apk: File,
    context: Context
): Observable<File> {
    return Observable.fromCallable {
        val name = "${apk.nameWithoutExtension}_AndroidManifest.xml"

        return@fromCallable exportManifestFromApkFileXML(name, apk)
    }
}

 fun exportManifestFromApkFileXML(name: String, apk: File): File {
     Log.i("manifest_command_export", "running exports");
    val doc = parseManifestFile(apk)
    //val destFile = File(context.cacheDir, name)
    val destFile2 = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "manifests/" + name)
    File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "manifests/").mkdirs()
     Log.i("manifest_command_export", "manifList: " + destFile2.path)
    writeXmlXML(doc, destFile2)

    return destFile2
}

private fun writeXmlXML(doc: Document, output: File) {
    val transformerFactory = TransformerFactory.newInstance()
    val transformer = transformerFactory.newTransformer()
    //val activity2 = Activity
    transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8")
    transformer.setOutputProperty(OutputKeys.INDENT, "yes")
    transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")
    val source = DOMSource(doc)
    val result = StreamResult(output)
    Log.i("manifest_command_write", "manifwrite3: " + output)

    transformer.transform(source, result)
    Log.i("manifest_command_write", "manifwrite3: " + "Completed Writing")
}

fun exportedManifestNameXML(packageName: String): String {
    val cleaned = packageName.replace("\\.".toRegex(), "_")
    return "${cleaned}_AndroidManifest.xml"
}

fun getPackageApkXML(info: PackageInfo): File {
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
    Log.i("manifest_command_parse", "Parsed AndroidManifest from $apkFile in $duration ns")
    return doc ?: throw FileNotFoundException("Couldn't find manifest in apk")
}
