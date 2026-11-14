package com.workbuddy.wearosmail

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.ambient.AmbientLifecycleObserver
import com.workbuddy.wearosmail.mail.AccountStore
import com.workbuddy.wearosmail.ui.WearMailApp

/**
 * Wear OS 入口。
 * AmbientLifecycleObserver 是 androidx.wear 当前推荐的常亮（微光）模式实现，
 * 挂到 Activity 的 Lifecycle 上；进入微光时 Compose 切换到纯黑时钟界面。
 * 这是 Wear OS 应用特有的能力，普通 Android 应用不会有。
 */
class MainActivity : ComponentActivity() {

    private val ambientState = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val observer = AmbientLifecycleObserver(
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
        lifecycle.addObserver(observer)

        setContent {
            val vm: MailViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T =
                        MailViewModel(AccountStore(applicationContext)) as T
                }
            )
            WearMailApp(ambient = ambientState.value, vm = vm)
        }
    }
}
