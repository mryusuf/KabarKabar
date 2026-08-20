package io.mryusuf.kabarkabar.di

import org.koin.core.module.Module

/** Android-only composition bridge; the shared data graph remains in commonMain. */
val androidSharedModule: Module
    get() = sharedModule
