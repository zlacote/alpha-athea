package iatheiya.ayana.ui.main

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import iatheiya.ayana.R
import iatheiya.ayana.ui.common.AyanaConfirmDialog
import iatheiya.ayana.ui.common.AyanaTextFieldDialog

@Composable
fun RenameDialog(
    initialName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    AyanaTextFieldDialog(
        title = stringResource(R.string.rename_title),
        initialValue = initialName,
        onDismiss = onDismiss,
        onSave = onSave,
    )
}

@Composable
fun DeleteConfirmDialog(
    sessionName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AyanaConfirmDialog(
        title = stringResource(R.string.delete_confirm_title),
        text = stringResource(R.string.delete_confirm_text, sessionName),
        confirmLabel = stringResource(R.string.menu_delete),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        confirmTinted = true,
    )
}
