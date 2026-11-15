package com.tengwear.tengemail

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.ambient.AmbientLifecycleObserver
import com.tengwear.tengemail.mail.AccountStore
import com.tengwear.tengemail.ui.CrashScreen
import com.tengwear.tengemail.ui.WearMailApp

/**
 * Wear OS 入口，同时兼容普通安卓设备。
 *
 * 注意：AmbientLifecycleObserver 只能在确认存在
 * com.google.android.wearable.compat.WearableActivityController 的设备上创建。
 * 该共享库由 Wear OS 系统提供，普通手机 / 平板 / 部分国产手表没有；
 * 若无条件 addObserver，onCreate 回调会抛 IllegalStateException 导致启动闪退。
 * 这里按能力条件挂载，任何异常都降级为「无微光模式」而非崩溃。
 */
class MainActivity : ComponentActivity() {

    private val ambientState = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        CrashLog.install(applicationContext)

        if (DeviceSupport.ambientAvailable(this)) {
            // 双保险：即使共享库存在但版本不匹配（回调方法签名变化），
            // addObserver 立即分发 ON_CREATE 时抛出的异常也不能让应用崩溃。
            var observer: AmbientLifecycleObserver? = null
            runCatching {
                observer = AmbientLifecycleObserver(
                    this,
                    object : AmbientLifecycleObserver.AmbientLifecycleCallback {
                        override fun onEnterAmbient(
                            ambientDetails: AmbientLifecycleObserver.AmbientDetails,
                        ) {
                            ambientState.value = true
                        }

                        override fun onExitAmbient() {
                            ambientState.value = false
                        }
                    },
                )
                lifecycle.addObserver(observer!!)
            }.onFailure {
                observer?.let { runCatching { lifecycle.removeObserver(it) } }
                ambientState.value = false
            }
        }

        val crash = CrashLog.read(applicationContext)

        setContent {
            if (crash != null) {
                CrashScreen(crash) {
                    CrashLog.clear(applicationContext)
                    recreate()
                }
                return@setContent
            }
            val vm: MailViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T =
                        MailViewModel(
                            AccountStore(applicationContext),
                            ThemeStore(applicationContext),
                        ) as T
                }
            )
            WearMailApp(
                ambient = ambientState.value,
                vm = vm,
                onExit = { finish() },
            )
        }
    }
}
