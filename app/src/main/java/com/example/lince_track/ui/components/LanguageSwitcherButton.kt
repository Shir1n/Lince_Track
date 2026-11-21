package com.example.lince_track.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.lince_track.R
import com.example.lince_track.ui.theme.AppLanguage
import com.example.lince_track.ui.theme.appLanguageFlow
import com.example.lince_track.ui.theme.setAppLanguage
import com.example.lince_track.util.LinceLogger
import kotlinx.coroutines.launch

@Composable
fun LanguageSwitcherButton(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentLanguage by context.appLanguageFlow.collectAsState(initial = AppLanguage.SYSTEM)
    val scope = rememberCoroutineScope()
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            shadowElevation = 2.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            IconButton(
                onClick = { expanded = true },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Language,
                    contentDescription = stringResource(R.string.cd_cambiar_idioma),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            AppLanguage.entries.forEach { language ->
                val selected = currentLanguage == language
                DropdownMenuItem(
                    text = { Text(text = language.label()) },
                    trailingIcon = if (selected) {
                        {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else null,
                    onClick = {
                        expanded = false
                        LinceLogger.i("IDIOMA", "Cambio de idioma a ${language.name}")
                        scope.launch { context.setAppLanguage(language) }
                    }
                )
            }
        }
    }
}

@Composable
fun AppLanguage.label(): String {
    return when (this) {
        AppLanguage.SYSTEM -> stringResource(R.string.idioma_sistema)
        AppLanguage.ES -> stringResource(R.string.idioma_espanol)
        AppLanguage.EN -> stringResource(R.string.idioma_ingles)
    }
}
