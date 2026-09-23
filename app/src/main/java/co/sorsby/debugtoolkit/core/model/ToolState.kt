package co.sorsby.debugtoolkit.core.model

sealed interface ToolState<out T> {
    data object Idle : ToolState<Nothing>
    data object Loading : ToolState<Nothing>
    data class Success<T>(val value: T) : ToolState<T>
    data class Error(val type: ToolError) : ToolState<Nothing>
}
