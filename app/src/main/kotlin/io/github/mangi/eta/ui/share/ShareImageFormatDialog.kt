package io.github.mangi.eta.ui.share

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.mangi.eta.R
import io.github.mangi.eta.ui.components.EtaOverlayDialog
import io.github.mangi.eta.ui.components.EtaTextButton

internal enum class ShareImageFormat(val widthDp: Dp) {
    Phone(420.dp),
    Tablet(600.dp),
    Desktop(840.dp),
}

@Composable
internal fun ShareImageFormatDialog(
    onDismissRequest: () -> Unit,
    onSelect: (ShareImageFormat) -> Unit,
) {
    EtaOverlayDialog(
        show = true,
        title = stringResource(R.string.share_image_format_title),
        onDismissRequest = onDismissRequest,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ShareImageFormat.entries.forEach { format ->
                EtaTextButton(
                    text = stringResource(
                        when (format) {
                            ShareImageFormat.Phone -> R.string.share_image_format_phone
                            ShareImageFormat.Tablet -> R.string.share_image_format_tablet
                            ShareImageFormat.Desktop -> R.string.share_image_format_desktop
                        },
                    ),
                    onClick = { onSelect(format) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
