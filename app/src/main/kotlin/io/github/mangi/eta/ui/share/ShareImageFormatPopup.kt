package io.github.mangi.eta.ui.share

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.mangi.eta.R
import io.github.mangi.eta.ui.components.EtaListPopup
import top.yukonga.miuix.kmp.basic.DropdownImpl
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.theme.LocalDismissState

internal enum class ShareImageFormat(val widthDp: Dp) {
    Phone(420.dp),
    Tablet(600.dp),
    Desktop(840.dp),
}

@Composable
internal fun ShareImageFormatPopup(
    show: Boolean,
    onDismissRequest: () -> Unit,
    onSelect: (ShareImageFormat) -> Unit,
) {
    EtaListPopup(show = show, onDismissRequest = onDismissRequest) {
        val dismiss = LocalDismissState.current
        ListPopupColumn {
            ShareImageFormat.entries.forEachIndexed { index, format ->
                DropdownImpl(
                    text = stringResource(
                        when (format) {
                            ShareImageFormat.Phone -> R.string.share_image_format_phone
                            ShareImageFormat.Tablet -> R.string.share_image_format_tablet
                            ShareImageFormat.Desktop -> R.string.share_image_format_desktop
                        },
                    ),
                    optionSize = ShareImageFormat.entries.size,
                    isSelected = false,
                    index = index,
                    onSelectedIndexChange = {
                        dismiss?.invoke()
                        onSelect(format)
                    },
                )
            }
        }
    }
}
