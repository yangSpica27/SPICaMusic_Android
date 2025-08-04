package me.spica27.spicamusic.widget


import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import me.spica27.spicamusic.R


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgreePrivacyDialog(onDismissRequest: () -> Unit) {


  BasicAlertDialog(
    properties = DialogProperties(
      dismissOnBackPress = false,
      dismissOnClickOutside = false
    ),
    onDismissRequest = {
      onDismissRequest()
    },
    content = {
      Surface(
        modifier = Modifier
          .wrapContentWidth()
          .height(350.dp),
        shape = MaterialTheme.shapes.large,
        tonalElevation = AlertDialogDefaults.TonalElevation
      ) {
        Column(
          modifier = Modifier.fillMaxSize()
        ) {
          Text(
            text = AnnotatedString.fromHtml(
              stringResource(R.string.permission_declaration)
            ),
          )
        }
      }


    },
  )
}