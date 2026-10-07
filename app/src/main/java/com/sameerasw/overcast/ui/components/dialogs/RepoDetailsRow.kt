package com.sameerasw.overcast.ui.components.dialogs

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.sameerasw.overcast.R
import com.sameerasw.overcast.utils.HapticUtil

@Composable
fun RepoDetailsRow(
    modifier: Modifier = Modifier,
    profileUrl: String = "https://github.com/sameerasw",
    repoUrl: String = "https://github.com/sameerasw/Overcast",
) {
    val context = LocalContext.current
    val view = LocalView.current
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Button(
            onClick = {
                HapticUtil.performUIHaptic(view)
                context.startActivity(Intent(Intent.ACTION_VIEW, profileUrl.toUri()))
            },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp, topEnd = 6.dp, bottomEnd = 6.dp),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
        ) {
            Icon(painterResource(R.drawable.brand_github), contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.app_github_username))
        }
        Button(
            onClick = {
                HapticUtil.performUIHaptic(view)
                context.startActivity(Intent(Intent.ACTION_VIEW, repoUrl.toUri()))
            },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp, topEnd = 20.dp, bottomEnd = 20.dp),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
        ) {
            Text(stringResource(R.string.app_repo_name))
        }
    }
}
