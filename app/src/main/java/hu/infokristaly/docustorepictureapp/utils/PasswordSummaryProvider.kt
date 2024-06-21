package hu.infokristaly.docustorepictureapp.utils
import androidx.preference.EditTextPreference
import androidx.preference.Preference
class PasswordSummaryProvider : Preference.SummaryProvider<EditTextPreference> {
    private companion object {
        private const val MASK_CHAR = '•'
    }

    override fun provideSummary(preference: EditTextPreference): CharSequence {
        val password = preference.text
        return if (password.isNullOrEmpty()) {
            ""
        } else {
            maskPassword(password)
        }
    }

    private fun maskPassword(password: String): CharSequence {
        return buildString {
            repeat(password.length) {
                append(MASK_CHAR)
            }
        }
    }
}