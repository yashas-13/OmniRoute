package online.omniroute.mobile.runtime

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class RuntimeViewModelFactory(
    private val manager: TermuxRuntimeManager,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(RuntimeViewModel::class.java))
        return RuntimeViewModel(manager) as T
    }
}
