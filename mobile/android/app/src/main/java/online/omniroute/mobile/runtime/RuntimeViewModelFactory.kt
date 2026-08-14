package online.omniroute.mobile.runtime

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import online.omniroute.mobile.gateway.OmniRouteClient

class RuntimeViewModelFactory(
    private val manager: TermuxRuntimeManager,
    private val client: OmniRouteClient,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(RuntimeViewModel::class.java))
        return RuntimeViewModel(manager, client) as T
    }
}
