package fr.xgouchet.packageexplorer.applist

import android.content.Context
import android.content.pm.PackageManager
import io.reactivex.rxjava3.core.ObservableEmitter
import io.reactivex.rxjava3.core.ObservableOnSubscribe

/**
 * @author Xavier F. Gouchet
 */
class AppListSource(val context: Context) :
    ObservableOnSubscribe<AppViewModel> {
    val pm = context.packageManager
    var applications = pm.getInstalledApplications(0)

    override fun subscribe(emitter: ObservableEmitter<AppViewModel>) {

        applications = pm.getInstalledApplications(0)
        val packages = pm.getInstalledPackages(PackageManager.GET_SIGNATURES or PackageManager.GET_SIGNING_CERTIFICATES)

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
        val packages = pm.getInstalledPackages(PackageManager.GET_SIGNATURES or PackageManager.GET_SIGNING_CERTIFICATES)

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
}
