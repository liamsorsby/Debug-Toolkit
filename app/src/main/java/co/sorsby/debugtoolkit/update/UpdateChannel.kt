package co.sorsby.debugtoolkit.update

enum class UpdateChannel(val queryValue: String) {
    PLAY("play"),
    OTHER("other"),
    ;

    companion object {
        fun fromInstallerPackageName(installerPackageName: String?): UpdateChannel =
            if (installerPackageName == PLAY_STORE_INSTALLER) PLAY else OTHER

        private const val PLAY_STORE_INSTALLER = "com.android.vending"
    }
}
