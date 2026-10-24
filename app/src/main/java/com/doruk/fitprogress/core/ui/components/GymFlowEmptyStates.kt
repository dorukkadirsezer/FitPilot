package com.doruk.fitprogress.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.doruk.fitprogress.R

object GymFlowEmptyStates {
    @Composable
    fun NoData(
        message: String,
        modifier: Modifier = Modifier,
        actionButton: (@Composable () -> Unit)? = null
    ) {
        EmptyStateCard(
            title = stringResource(R.string.empty_state_no_data),
            description = message,
            modifier = modifier,
            actionButton = actionButton
        )
    }

    @Composable
    fun Error(
        message: String,
        modifier: Modifier = Modifier,
        onRetry: (() -> Unit)? = null,
        actionButton: (@Composable () -> Unit)? = null
    ) {
        val retryText = stringResource(R.string.action_retry)
        EmptyStateCard(
            title = stringResource(R.string.empty_state_error),
            description = message,
            modifier = modifier,
            actionButton = actionButton ?: onRetry?.let {
                {
                    PrimaryActionButton(
                        onClick = it,
                        text = retryText
                    )
                }
            }
        )
    }

    @Composable
    fun EmptyStateCard(
        title: String,
        description: String,
        modifier: Modifier = Modifier,
        actionButton: (@Composable () -> Unit)? = null
    ) {
        MetricCard(title = "", modifier = modifier) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
                )
                if (actionButton != null) {
                    actionButton()
                }
            }
        }
    }
}
