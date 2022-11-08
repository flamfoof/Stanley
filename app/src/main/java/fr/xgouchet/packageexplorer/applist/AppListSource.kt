package fr.xgouchet.packageexplorer.applist

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import io.reactivex.rxjava3.core.ObservableEmitter
import io.reactivex.rxjava3.core.ObservableOnSubscribe

/**
 * @author Xavier F. Gouchet
 */
class AppListSource(val context: Context) :
    ObservableOnSubscribe<AppViewModel> {
    val pm = context.packageManager
    var applications = pm.getInstalledApplications(0)

    @Suppress("DEPRECATION")
    override fun subscribe(emitter: ObservableEmitter<AppViewModel>) {

        applications = pm.getInstalledApplications(0)
        val packages =
            pm.getInstalledPackages(PackageManager.GET_SIGNATURES or PackageManager.GET_SIGNING_CERTIFICATES)

        for (pi in packages) {
            val app = AppViewModel.fromAppInfo(pm, pi, pi.applicationInfo)

            emitter.onNext(app)
        }

//        applications.forEach {
//            val app = AppViewModel.fromAppInfo(pm, null, it)
//            emitter.onNext(app)
//        }

        emitter.onComplete()
    }

    fun subscribeExport(emitter: ObservableEmitter<AppViewModel>) {

        applications = pm.getInstalledApplications(0)
        val packages =
            pm.getInstalledPackages(PackageManager.GET_SIGNATURES or PackageManager.GET_SIGNING_CERTIFICATES)

        for (pi in packages) {
            val app = AppViewModel.fromAppInfo(pm, pi, pi.applicationInfo)

            val pm = context.packageManager
            val applications = pm.getInstalledApplications(0)
            val packages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pm.getInstalledPackages(PackageManager.GET_SIGNATURES or PackageManager.GET_SIGNING_CERTIFICATES)
            } else {
                pm.getInstalledPackages(PackageManager.GET_SIGNATURES)
            }

            applications.forEach { ai ->
                val pi = packages.firstOrNull() {
                    it.packageName == ai.packageName
                }
                val app = AppViewModel.fromAppInfo(pm, pi, ai)
                emitter.onNext(app)
            }

//        applications.forEach {
//            val app = AppViewModel.fromAppInfo(pm, null, it)
//            emitter.onNext(app)
//        }

            emitter.onComplete()
        }
    }
}