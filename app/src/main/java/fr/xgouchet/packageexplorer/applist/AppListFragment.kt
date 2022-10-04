package fr.xgouchet.packageexplorer.applist

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.appcompat.widget.SearchView
import fr.xgouchet.packageexplorer.R
import fr.xgouchet.packageexplorer.about.AboutActivity
import fr.xgouchet.packageexplorer.applist.sort.AppSort
import fr.xgouchet.packageexplorer.core.utils.*
import fr.xgouchet.packageexplorer.details.apk.ApkDetailsActivity
import fr.xgouchet.packageexplorer.launcher.LauncherDialog
import fr.xgouchet.packageexplorer.oss.OSSActivity
import fr.xgouchet.packageexplorer.ui.adapter.BaseAdapter
import fr.xgouchet.packageexplorer.ui.mvp.list.BaseListFragment
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.functions.Consumer
import android.content.pm.ApplicationInfo
import android.os.Environment
import java.io.File
import java.io.PrintWriter


class AppListFragment :
    BaseListFragment<AppViewModel, AppListPresenter>(),
    Consumer<AppViewModel> {

    override val adapter: BaseAdapter<AppViewModel> = AppAdapter(this, this)
    override val isFabVisible: Boolean = false
    override val fabIconOverride: Int? = null
    var exportDisposable: Disposable? = null

    // region Fragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.app_list, menu)

        menu.findItem(R.id.action_search)?.let {
            val searchView = it.actionView as SearchView
            searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean = false

                override fun onQueryTextChange(newText: String?): Boolean {
                    presenter.setFilter(newText ?: "")
                    return true
                }
            })
        }

        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        val systemAppsVisible = presenter.areSystemAppsVisible()
        menu.findItem(R.id.hide_system_apps).isVisible = systemAppsVisible
        menu.findItem(R.id.show_system_apps).isVisible = !systemAppsVisible
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {
            R.id.sort_by_title -> presenter.setSort(AppSort.TITLE)
            R.id.sort_by_package_name -> presenter.setSort(AppSort.PACKAGE_NAME)
            R.id.sort_by_install_time -> presenter.setSort(AppSort.INSTALL_TIME)
            R.id.sort_by_update_time -> presenter.setSort(AppSort.UPDATE_TIME)
            R.id.open_apk -> {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
                intent.type = APK_MIME_TYPE
                intent.addCategory(Intent.CATEGORY_OPENABLE)
                startActivityForResult(intent, OPEN_APK_REQUEST)
            }
            R.id.export_all_xml -> {
                Log.i("manifest_command", "Exporting all xmls")

                var pm = context?.packageManager
                var applications = pm?.getInstalledApplications(0)
                val packages = pm?.getInstalledPackages(PackageManager.GET_SIGNATURES or PackageManager.GET_SIGNING_CERTIFICATES)

                if(pm != null && packages != null)
                {
                    var packageNameList = ArrayList<String>()
                    var APKNameList = ArrayList<String>()
                    var apkNamesz = ""
                    var packageNamesz = ""
                    for (pi in packages) {
                        val ai = pm!!.getApplicationInfo(pi.packageName, 0)

                        println(">>>>>>packages is<<<<<<<<" + ai.publicSourceDir)

                        if (ai.flags and ApplicationInfo.FLAG_SYSTEM != 0) {
                            System.out.println(">>>>>>packages is system package" + pi.packageName)
                        } else {
                            Log.i("manifest_command", "Package Name1: " + pi.packageName)
                            var name = exportedManifestNameXML(pi.packageName)
                            var apk = getPackageApkXML(pi)
                            exportManifestFromApkFileXML(name, apk)
                        }
                        if (ai.flags and ApplicationInfo.FLAG_SYSTEM != 0) {
                            System.out.println(">>>>>>packages is system package" + pi.packageName)
                        } else {
                            Log.i("package_support_command", "Package Name1: " + pi.packageName)
                            packageNameList.add(pi.packageName)
                            packageNamesz += pi.packageName + "\n"
                            apkNamesz += (if(pi.applicationInfo != null)  pm.getApplicationLabel(pi.applicationInfo).toString() else "None") + "\n"
                            //Log.i("appNameTest", pi.applicationInfo.na)
                        }
                    }
                    var directoryOutput = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "PackageNameAndAPK/")
                    File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "PackageNameAndAPK/").mkdirs()
                    File(directoryOutput, "packageNames.txt").writeText(packageNamesz)
                    File(directoryOutput, "apkNames.txt").writeText(apkNamesz)
                    Log.e("package_support_command", "package: " + packageNamesz)
                    Log.e("package_support_command", "apk: " + apkNamesz)
                } else {
                    Log.e("manifest_search", "wtf? Null? Why?");
                }
            }
            R.id.play_exoplayer ->
            {
                Log.e("exoplayer_test", "starting exoplayer");
            }
            R.id.hide_system_apps -> presenter.setSystemAppsVisible(false)
            R.id.show_system_apps -> presenter.setSystemAppsVisible(true)
            R.id.about -> {
                startActivity(Intent(activity, AboutActivity::class.java))
            }
            R.id.licenses -> {
//                OssLicensesMenuActivity.setActivityTitle(getString(R.string.activity_title_licenses))
                startActivity(Intent(activity, OSSActivity::class.java))
            }
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            OPEN_APK_REQUEST -> onOpenApkResult(resultCode, data)
        }
    }

    // endregion

    // region Displayer

    fun promptActivity(resolvedInfos: List<ResolveInfo>) {
        val supportFragmentManager = activity?.supportFragmentManager ?: return
        val transaction = supportFragmentManager.beginTransaction()
        LauncherDialog.withData(resolvedInfos)
                .show(transaction, null)
    }

    @SuppressLint("NewApi")
    override fun getPermissionExplanation(permission: String): Int {
        return when (permission) {
            AppListPresenter.PERMISSION_QUERY_PACKAGES -> R.string.permission_explanation_query_packages
            else -> 0
        }
    }

    // endregion

    // region Consumer

    override fun accept(t: AppViewModel) {
        presenter.openApplication(t.packageName)
    }

    // endregion

    // region Private

    private fun onOpenApkResult(resultCode: Int, resultData: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            val uri: Uri? = resultData?.data
            if (uri != null) {
                val intent = Intent(context, ApkDetailsActivity::class.java)
                intent.data = uri
                startActivity(intent)
            }
        }
    }

    // endregion

    companion object {
        const val APK_MIME_TYPE = "application/vnd.android.package-archive"
        const val OPEN_APK_REQUEST = 1
    }
}
