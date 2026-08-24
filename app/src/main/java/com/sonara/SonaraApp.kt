package com.sonara

import android.app.Application
import com.sonara.di.AppContainer

/** Application entry point; owns the hand-rolled dependency container. */
class SonaraApp : Application() {

    val container: AppContainer by lazy { AppContainer(this) }
}
