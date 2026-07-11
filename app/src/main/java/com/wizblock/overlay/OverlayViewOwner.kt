package com.wizblock.overlay

import android.os.Bundle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner

class OverlayViewOwner : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val ownerViewModelStore = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    init {
        savedStateRegistryController.performAttach()
        savedStateRegistryController.performRestore(Bundle())
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    override val viewModelStore: ViewModelStore
        get() = ownerViewModelStore

    fun destroy() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        ownerViewModelStore.clear()
    }
}
